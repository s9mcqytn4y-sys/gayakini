Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\common.ps1"

# Load token from environment
Load-McpEnvironment

if (-not $env:GITHUB_PERSONAL_ACCESS_TOKEN) {
    Write-Warning "GITHUB_PERSONAL_ACCESS_TOKEN is not set. GitHub server may fail."
}

Write-Host "Starting gayakini-github MCP server..."
Start-McpNpx -Package "@modelcontextprotocol/server-github"
