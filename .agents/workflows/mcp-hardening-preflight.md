---
description: Full MCP hardening preflight — validates launchers then checks doc parity
---

# MCP Hardening Preflight

Runs the full MCP hardening preflight by combining launcher validation and doc parity checks.
Equivalent of the VSCode task "MCP: Hardening preflight" which depends on "MCP: Validate launchers" + "MCP: Docs parity check".

## Steps

1. Run the `/validate-mcp-launchers` workflow first. All 7 launchers must pass `-ValidateOnly`.

2. Run the `/docs-parity-check` workflow. All docs must reference required launcher terms.

3. Optionally verify workspace JSON sanity:
```shell
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "@('.vscode\settings.json','.vscode\launch.json','.vscode\tasks.json','.vscode\extensions.json') | ForEach-Object { if (Test-Path $_) { Get-Content $_ -Raw | ConvertFrom-Json | Out-Null } }; Write-Host 'Workspace JSON: valid.'"
```

All steps must pass before committing changes to launcher/doc/workflow files.
