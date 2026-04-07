param(
    [switch]$ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Load helper
. "$PSScriptRoot\common.ps1"

# 1. Environment resolution
Load-McpEnvironment

# 2. Validation / Runtime
if ($ValidateOnly) {
    $RepoRoot = Get-RepoRoot
    Write-Host "Terminal launcher preflight:" -ForegroundColor Cyan
    Write-Host "  Repo Root: $RepoRoot"
    Write-Host "  Terminal package: @modelcontextprotocol/server-terminal"
}

Start-McpNpx -Package "@modelcontextprotocol/server-terminal" -ValidateOnly:$ValidateOnly
