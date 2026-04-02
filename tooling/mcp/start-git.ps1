Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\common.ps1"

$RepoRoot = Get-RepoRoot
$AbsRepoRoot = (Get-Item $RepoRoot).FullName

# Validate git is present
Assert-Command "git.exe"

Write-Host "Starting gayakini-git MCP server..."
Write-Host "Repository: $AbsRepoRoot"

Start-McpNpx -Package "@modelcontextprotocol/server-git" -Args "--repository", $AbsRepoRoot
