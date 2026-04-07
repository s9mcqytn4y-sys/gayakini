param(
    [string]$Root,
    [switch]$ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\common.ps1"

Load-McpEnvironment

$ResolvedRoot = Assert-Directory -Path (Get-RepoRoot -Root $Root) -Label 'Git repository root'

if ($ValidateOnly) {
    Write-Host "Git launcher preflight:" -ForegroundColor Cyan
    Write-Host "  Repo Root: $ResolvedRoot"
}

Start-McpNpx -Package "@modelcontextprotocol/server-git" -Args @($ResolvedRoot) -ValidateOnly:$ValidateOnly
