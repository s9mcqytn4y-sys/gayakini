param(
    [switch]$ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Load helper
. "$PSScriptRoot\common.ps1"

# 1. Environment resolution
Load-McpEnvironment

# RTK Terminal Filter Configuration
$RtkEnabled = (Get-EnvironmentValue -Names @('RTK_ENABLED') -Default 'false') -eq 'true'
$RtkProfile = Get-EnvironmentValue -Names @('RTK_PROFILE') -Default 'balanced'
$RtkMode = Get-EnvironmentValue -Names @('RTK_MODE') -Default 'filtered'
$RtkRulesPath = Get-EnvironmentValue -Names @('RTK_RULES_PATH') -Default "$PSScriptRoot\..\rtk\rtk-rules.json"
$RtkTeeDir = Get-EnvironmentValue -Names @('RTK_TEE_DIR') -Default "$PSScriptRoot\..\rtk\logs"

# 2. Validation / Runtime
if ($ValidateOnly) {
    $RepoRoot = Get-RepoRoot
    Write-Host "Terminal launcher preflight:" -ForegroundColor Cyan
    Write-Host "  Repo Root: $RepoRoot"
    Write-Host "  Terminal package: @modelcontextprotocol/server-terminal"

    # RTK Validation
    if ($RtkEnabled) {
        Write-Host "  RTK Filter: ENABLED" -ForegroundColor Yellow
        Assert-File -Path "$PSScriptRoot\..\rtk\filter-terminal-output.ps1" -Label "RTK Filter Script" | Out-Null
        Assert-File -Path "$RtkRulesPath" -Label "RTK Rules JSON" | Out-Null
        Assert-File -Path "$PSScriptRoot\..\rtk\tee-output.ps1" -Label "RTK Tee Script" | Out-Null

        # Validate JSON
        try {
            Get-Content $RtkRulesPath -Raw | ConvertFrom-Json | Out-Null
            Write-Host "  RTK Rules JSON: VALID" -ForegroundColor Green
        } catch {
            throw "Invalid RTK Rules JSON: $_"
        }
    } else {
        Write-Host "  RTK Filter: DISABLED"
    }
}

Start-McpNpx -Package "@modelcontextprotocol/server-terminal" -ValidateOnly:$ValidateOnly
