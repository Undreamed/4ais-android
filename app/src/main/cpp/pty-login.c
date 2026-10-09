/*
 * pty-login.c: tiny Bionic/AArch64 PTY adapter for Android's OpenClaw CLI.
 *
 * The 4AIs app talks to this executable over ordinary ProcessBuilder pipes.
 * The child sees an actual controlling TTY (forkpty) so OpenClaw can run
 * its built-in OAuth login UI without pretending a Java pipe is a terminal.
 *
 * No network access, auth or credential storage here. The adapter merely
 * relays bytes and propagates the child exit status.
 */
#define _GNU_SOURCE
#include <pty.h>
#include <signal.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/select.h>
#include <sys/wait.h>
#include <unistd.h>

static pid_t child_pid = -1;
static volatile sig_atomic_t stopped = 0;
static void on_term(int sig) {
    (void)sig;
    stopped = 1;
    if (child_pid > 0) kill(-child_pid, SIGTERM);
}
static int transfer(int input, int output) {
    char buf[8192];
    ssize_t got;
    do { got = read(input, buf, sizeof(buf)); } while (got < 0 && errno == EINTR);
    if (got == 0) return 0;
    if (got < 0) return errno == EIO ? 0 : -1;
    ssize_t off = 0;
    while (off < got) {
        ssize_t n = write(output, buf + off, (size_t)(got - off));
        if (n < 0 && errno == EINTR) continue;
        if (n <= 0) return -1;
        off += n;
    }
    return 1;
}

int main(int argc, char **argv) {
    if (argc < 2) {
        fprintf(stderr, "Usage: pty-login <command> [args...]\n");
        return 64;
    }
    signal(SIGTERM, on_term);
    signal(SIGINT, on_term);
    // A modest terminal: not inheriting the Android app's Java-side pipe types.
    struct winsize size = { .ws_row = 30, .ws_col = 100, .ws_xpixel = 0, .ws_ypixel = 0 };
    int master = -1;
    child_pid = forkpty(&master, NULL, NULL, &size);
    if (child_pid < 0) {
        perror("forkpty");
        return 71;
    }
    if (child_pid == 0) {
        // TTY child session established; propagate process environment unchanged.
        setenv("TERM", "xterm", 1);
        execvp(argv[1], &argv[1]);
        perror("execvp");
        _exit(127);
    }
    int input_open = 1, master_open = 1;
    while (!stopped && master_open) {
        fd_set rd;
        FD_ZERO(&rd);
        FD_SET(master, &rd);
        int maxfd = master;
        if (input_open) {
            FD_SET(STDIN_FILENO, &rd);
            if (STDIN_FILENO > maxfd) maxfd = STDIN_FILENO;
        }
        struct timeval interval = { .tv_sec = 1, .tv_usec = 0 };
        int n = select(maxfd + 1, &rd, NULL, NULL, &interval);
        if (n < 0 && errno == EINTR) continue;
        if (n < 0) break;
        if (n == 0) {
            int stat = 0;
            if (waitpid(child_pid, &stat, WNOHANG) == child_pid) {
                child_pid = -1;
                close(master);
                if (WIFEXITED(stat)) return WEXITSTATUS(stat);
                return 128 + WTERMSIG(stat);
            }
            continue;
        }
        if (FD_ISSET(master, &rd) && transfer(master, STDOUT_FILENO) <= 0) {
            master_open = 0;
        }
        if (input_open && FD_ISSET(STDIN_FILENO, &rd) &&
            transfer(STDIN_FILENO, master) <= 0) {
            input_open = 0;
        }
    }
    if (stopped && child_pid > 0) kill(-child_pid, SIGTERM);
    int stat = 0;
    if (child_pid > 0) waitpid(child_pid, &stat, 0);
    close(master);
    if (WIFEXITED(stat)) return WEXITSTATUS(stat);
    if (WIFSIGNALED(stat)) return 128 + WTERMSIG(stat);
    return 1;
}
