Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\common.ps1"

# Load environment variables (for Gradle etc)
Load-McpEnvironment

Write-Host "Starting gayakini-terminal MCP server..."

# Launch the terminal server
Start-McpNpx -Package "@modelcontextprotocol/server-terminal"
