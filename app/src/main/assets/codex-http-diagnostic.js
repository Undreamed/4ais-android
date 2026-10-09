// Only classify known Codex backend errors. Never persist prompts, answers or credentials.
// CODEX_HTTP_DIAGNOSTIC_V1
if (parsedUrl.hostname === "chatgpt.com" &&
    parsedUrl.pathname === "/backend-api/codex/responses" &&
    response.status >= 400 && process.env.CODEX_DIAG_PATH) {
    try {
        const payload = await response.clone().json();
        const detail = typeof payload?.detail === "string" ? payload.detail :
            typeof payload?.error?.message === "string" ? payload.error.message : "";
        let hint = "unclassified";
        const field = /^(?:Unsupported|Unknown|Invalid) parameter:\s*([a-z_]{1,40})\s*\.?$/i.exec(detail);
        if (field) hint = "rejected_field:" + field[1].toLowerCase();
        else if (/One of ["']?input/i.test(detail) && /must be provided/i.test(detail)) hint = "missing_input";
        else if (/model/i.test(detail) && /(not found|not supported|invalid)/i.test(detail)) hint = "model_rejected";
        else if (/access|organization|permission|not authorized/i.test(detail)) hint = "access_rejected";
        const fs = await import("node:fs");
        fs.writeFileSync(process.env.CODEX_DIAG_PATH, JSON.stringify({status: response.status, hint}));
    } catch { /* Error hints must never affect agent execution. */ }
}
