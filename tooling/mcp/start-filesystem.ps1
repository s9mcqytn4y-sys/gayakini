param(
    [string]$Root,
    [switch]$ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\common.ps1"

Load-McpEnvironment

$ResolvedRoot = Assert-Directory -Path (Get-RepoRoot -Root $Root) -Label 'Filesystem root'

if ($ValidateOnly) {
    Write-Output "Filesystem root: $ResolvedRoot"
}

Start-McpNpx -Package "@modelcontextprotocol/server-filesystem" -Args @($ResolvedRoot) -ValidateOnly:$ValidateOnly
