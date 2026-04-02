Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\common.ps1"

$RepoRoot = Get-RepoRoot
# Resolve the path to ensure it's absolute and standardized
$AbsRepoRoot = (Get-Item $RepoRoot).FullName

Write-Host "Starting gayakini-filesystem MCP server..."
Write-Host "Restricting to: $AbsRepoRoot"

Start-McpNpx -Package "@modelcontextprotocol/server-filesystem" -Args $AbsRepoRoot
