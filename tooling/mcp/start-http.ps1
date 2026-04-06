param(
    [string]$ApiBaseUrl,
    [string]$OpenApiSpecPath,
    [string]$ApiHeaders,
    [ValidateSet('all', 'dynamic', 'explicit')]
    [string]$ToolsMode,
    [switch]$ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\common.ps1"

Load-McpEnvironment

$RepoRoot = Get-RepoRoot
$ResolvedApiBaseUrl = Assert-HttpUrl -Url (Get-FirstValue -Values @(
    $ApiBaseUrl,
    (Get-EnvironmentValue -Names @('APP_BASE_URL', 'API_BASE_URL'))
) -Default 'http://localhost:8080') -Label 'HTTP API base URL'

$ConfiguredSpec = Get-FirstValue -Values @(
    $OpenApiSpecPath,
    (Get-EnvironmentValue -Names @('OPENAPI_SPEC_PATH'))
) -Default (Join-Path -Path $RepoRoot -ChildPath 'docs\brand-fashion-ecommerce-api-final.yaml')

if ($ConfiguredSpec -match '^https?://') {
    $ResolvedSpecPath = $ConfiguredSpec
} else {
    $ResolvedSpecPath = Assert-File -Path $ConfiguredSpec -Label 'OpenAPI spec'
}

$ResolvedHeaders = Get-FirstValue -Values @(
    $ApiHeaders,
    (Get-EnvironmentValue -Names @('API_HEADERS'))
)

$ResolvedToolsMode = Get-FirstValue -Values @(
    $ToolsMode,
    (Get-EnvironmentValue -Names @('TOOLS_MODE'))
) -Default 'dynamic'

$Args = @(
    '--api-base-url', $ResolvedApiBaseUrl,
    '--openapi-spec', $ResolvedSpecPath,
    '--transport', 'stdio',
    '--tools', $ResolvedToolsMode,
    '--verbose', 'false'
)

if (-not [string]::IsNullOrWhiteSpace($ResolvedHeaders)) {
    $Args += @('--headers', $ResolvedHeaders)
}

if ($ValidateOnly) {
    Write-Output "HTTP target: $ResolvedApiBaseUrl"
    Write-Output "HTTP spec: $ResolvedSpecPath"
    Write-Output "HTTP tools mode: $ResolvedToolsMode"
    if ($ResolvedApiBaseUrl -match '^https?://(localhost|127\.0\.0\.1)(:\d+)?$') {
        Write-Output 'HTTP scope: local app target'
    } else {
        Write-Output 'HTTP scope: non-local target configured explicitly'
    }
    if (-not [string]::IsNullOrWhiteSpace($ResolvedHeaders)) {
        Write-Output 'HTTP headers: configured via API_HEADERS/parameter'
    }
}

Start-McpNpx -Package '@ivotoby/openapi-mcp-server' -Args $Args -ValidateOnly:$ValidateOnly
