Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\common.ps1"

# Load credentials from .env
Load-McpEnvironment

# Construct Connection String
$DbUrl = $env:DATABASE_URL
if (-not $DbUrl) {
    $HostName = $env:DB_HOST -or "localhost"
    $Port = $env:DB_PORT -or "5432"
    $DbName = $env:DB_NAME -or "gayakini"
    $User = $env:DB_USERNAME -or "postgres"
    $Pass = $env:DB_PASSWORD

    if (-not $Pass) {
        Write-Warning "DB_PASSWORD is empty. Connection may fail."
    }

    $DbUrl = "postgresql://$($User):$($Pass)@$($HostName):$($Port)/$($DbName)"
}

Write-Host "Starting gayakini-postgres MCP server..."
Start-McpNpx -Package "@modelcontextprotocol/server-postgres" -Args $DbUrl
