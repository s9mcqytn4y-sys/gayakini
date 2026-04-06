---
description: Check that all agent/provider docs reference required MCP launcher terms
---

# MCP Docs Parity Check

Verifies that operational docs (`AGENTS.md`, `docs/tooling/mcp-servers.md`) reference all launcher terms, and all provider overlays reference `AGENTS.md` and `ValidateOnly`.
Equivalent of the VSCode task "MCP: Docs parity check".

## Steps

// turbo-all

1. Verify launcher terms in operational docs:
```shell
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$docsWithLaunchers=@('AGENTS.md','docs\tooling\mcp-servers.md'); $launcherTerms=@('ValidateOnly','start-filesystem.ps1','start-postgres.ps1','start-github.ps1','start-git.ps1','start-terminal.ps1','start-http.ps1','start-browser.ps1'); foreach($doc in $docsWithLaunchers){ $content=Get-Content $doc -Raw; foreach($term in $launcherTerms){ if($content -notmatch [regex]::Escape($term)){ throw \"Missing $term in $doc\" } } }; Write-Host 'Operational docs: all launcher terms present.'"
```

2. Verify provider overlay docs reference AGENTS.md and ValidateOnly:
```shell
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$providerDocs=@('gemini.md','CLAUDE.md','CODEX.md','docs\agents\codex-mcp-prompts.md','docs\agents\codex.md'); foreach($doc in $providerDocs){ $content=Get-Content $doc -Raw; foreach($term in @('AGENTS.md','ValidateOnly')){ if($content -notmatch [regex]::Escape($term)){ throw \"Missing $term in $doc\" } } }; Write-Host 'Provider overlays: all parity terms present.'"
```

Both checks must pass. If any fail, the named doc is missing the named term.
