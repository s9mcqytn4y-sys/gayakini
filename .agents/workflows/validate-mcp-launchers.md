---
description: Validate all MCP launcher scripts using -ValidateOnly preflight
---

# Validate MCP Launchers

Runs `-ValidateOnly` on every `start-*.ps1` launcher in `tooling\mcp\`.
Equivalent of the VSCode task "MCP: Validate launchers".

## Steps

// turbo-all

1. Set the GitHub token env var if not already set (needed for github launcher):
```shell
$env:GITHUB_PERSONAL_ACCESS_TOKEN = if ($env:GITHUB_PERSONAL_ACCESS_TOKEN) { $env:GITHUB_PERSONAL_ACCESS_TOKEN } else { 'dummy_local_token' }
```

2. Validate the filesystem launcher:
```shell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-filesystem.ps1 -ValidateOnly
```

3. Validate the postgres launcher:
```shell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-postgres.ps1 -ValidateOnly
```

4. Validate the github launcher:
```shell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-github.ps1 -ValidateOnly
```

5. Validate the git launcher:
```shell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-git.ps1 -ValidateOnly
```

6. Validate the terminal launcher:
```shell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-terminal.ps1 -ValidateOnly
```

7. Validate the http launcher:
```shell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-http.ps1 -ValidateOnly
```

8. Validate the browser launcher:
```shell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tooling\mcp\start-browser.ps1 -ValidateOnly
```

All 7 launchers must pass. If any fail, check `npx.cmd` resolution and env vars per `docs/tooling/mcp-servers.md`.
