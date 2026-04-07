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
    Write-Host "HTTP launcher preflight:" -ForegroundColor Cyan
    Write-Host "  Target: $ResolvedApiBaseUrl"
    Write-Host "  Spec: $ResolvedSpecPath"
    Write-Host "  Tools mode: $ResolvedToolsMode"

    if ($ResolvedApiBaseUrl -match '^https?://(localhost|127\.0\.0\.1)(:\d+)?$') {
        Write-Host '  Scope: local app target'
    } else {
        Write-Host '  Scope: non-local target configured' -ForegroundColor Yellow
    }

    if (-not [string]::IsNullOrWhiteSpace($ResolvedHeaders)) {
        Write-Host '  Headers: configured'
    }
}

Start-McpNpx -Package '@ivotoby/openapi-mcp-server' -Args $Args -ValidateOnly:$ValidateOnly
