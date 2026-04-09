param(
    [Parameter(Mandatory = $true)]
    [string]$Cmd,
    [string]$Profile = "balanced",
    [string]$Mode = "filtered"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$RtkEnabled = ($env:RTK_ENABLED -eq 'true')
$RulesPath = if ($env:RTK_RULES_PATH) { $env:RTK_RULES_PATH } else { "$PSScriptRoot\rtk-rules.json" }
$TeeDir = if ($env:RTK_TEE_DIR) { $env:RTK_TEE_DIR } else { "$PSScriptRoot\logs" }

if (-not $RtkEnabled) {
    # Direct execution if RTK is disabled
    Invoke-Expression $Cmd
    exit $LASTEXITCODE
}

# 1. Execute and capture
$FullOutput = ""
$CapturedExitCode = 0

try {
    # Using script block to capture both stdout and stderr
    $FullOutput = & powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$Cmd 2>&1" | Out-String
    $CapturedExitCode = $LASTEXITCODE
} catch {
    $FullOutput = $_.ToString()
    $CapturedExitCode = 1
}

# 2. Tee/Save raw output
$RawOutputRef = ""
try {
    $RawOutputRef = $FullOutput | & "$PSScriptRoot\tee-output.ps1" -TeeDir $TeeDir
} catch {
    Write-Warning "RTK: Failed to save raw output to tee."
}

# 3. Filter output
try {
    & "$PSScriptRoot\filter-terminal-output.ps1" `
        -InputText $FullOutput `
        -Profile $Profile `
        -Mode $Mode `
        -RulesPath $RulesPath `
        -CommandText $Cmd `
        -ExitCode $CapturedExitCode `
        -RawOutputRef $RawOutputRef `
        -PassThruOnParseError
} catch {
    # Fallback to raw output if filtering crashes
    Write-Warning "RTK: Filter failed, falling back to raw output."
    Write-Output $FullOutput
}

# 4. Preserve original exit code
exit $CapturedExitCode
