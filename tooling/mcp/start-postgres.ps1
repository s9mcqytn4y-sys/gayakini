param(
    [string]$ConnectionString,
    [string]$DbHost,
    [string]$Port,
    [string]$Database,
    [string]$Username,
    [string]$Password,
    [switch]$ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\common.ps1"

Load-McpEnvironment

$DbUrl = Get-FirstValue -Values @(
    $ConnectionString,
    (Get-EnvironmentValue -Names @('DATABASE_URL'))
)

if ([string]::IsNullOrWhiteSpace($DbUrl)) {
    $ResolvedHost = Get-FirstValue -Values @(
        $DbHost,
        (Get-EnvironmentValue -Names @('DB_HOST', 'PGHOST'))
    ) -Default 'localhost'
    $ResolvedPort = Get-FirstValue -Values @(
        $Port,
        (Get-EnvironmentValue -Names @('DB_PORT', 'PGPORT'))
    ) -Default '5432'
    $ResolvedDatabase = Get-FirstValue -Values @(
        $Database,
        (Get-EnvironmentValue -Names @('DB_NAME', 'PGDATABASE'))
    ) -Default 'gayakini'
    $ResolvedUsername = Get-FirstValue -Values @(
        $Username,
        (Get-EnvironmentValue -Names @('DB_USERNAME', 'PGUSER'))
    ) -Default 'postgres'
    $ResolvedPassword = Get-FirstValue -Values @(
        $Password,
        (Get-EnvironmentValue -Names @('DB_PASSWORD', 'PGPASSWORD'))
    ) -Default 'password'

    $DbUrl = "postgresql://$ResolvedUsername`:$ResolvedPassword@$ResolvedHost`:$ResolvedPort/$ResolvedDatabase"
}

if ($ValidateOnly) {
    if ([string]::IsNullOrWhiteSpace($ConnectionString) -and [string]::IsNullOrWhiteSpace((Get-EnvironmentValue -Names @('DATABASE_URL')))) {
        Write-Output "Postgres target: host=$ResolvedHost port=$ResolvedPort database=$ResolvedDatabase username=$ResolvedUsername"
    } else {
        Write-Output "Postgres target: using explicit connection string or DATABASE_URL"
    }
}

Start-McpNpx -Package "@modelcontextprotocol/server-postgres" -Args @($DbUrl) -ValidateOnly:$ValidateOnly
