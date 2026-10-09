#!/usr/bin/env python3
"""Build the tiny Android terminal adapter with the installed NDK."""
import argparse
import os
from pathlib import Path
import platform
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--ndk', type=Path, help='NDK directory (or set ANDROID_NDK_HOME)')
args = parser.parse_args()
ndk = args.ndk or (Path(os.environ['ANDROID_NDK_HOME']) if os.environ.get('ANDROID_NDK_HOME') else None)
if ndk is None:
    parser.error('Pass --ndk or set ANDROID_NDK_HOME. NDK 28.1.13356709 is used in CI.')
host = {'Windows': 'windows-x86_64', 'Linux': 'linux-x86_64', 'Darwin': 'darwin-x86_64'}[platform.system()]
toolchain = ndk / 'toolchains/llvm/prebuilt' / host
compiler = toolchain / 'bin' / ('clang.exe' if platform.system() == 'Windows' else 'clang')
root = Path(__file__).resolve().parent.parent
output = root / 'app/src/main/jniLibs/arm64-v8a/libptylogin.so'
output.parent.mkdir(parents=True, exist_ok=True)
subprocess.run([str(compiler), '--target=aarch64-linux-android26',
                '--sysroot=' + str(toolchain / 'sysroot'), '-std=c11', '-O2', '-fPIE', '-pie',
                '-Wall', '-Wextra', '-Werror', '-Wl,-z,max-page-size=16384',
                '-o', str(output), str(root / 'app/src/main/cpp/pty-login.c')], check=True)
print('Built', output.name)
