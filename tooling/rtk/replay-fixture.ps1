param(
    [string]$FixtureName = "noisy-gradle-failure.txt",
    [string]$Profile = "balanced",
    [switch]$Benchmark
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$FixturePath = Join-Path "$PSScriptRoot\fixtures" $FixtureName
if (-not (Test-Path $FixturePath)) {
    throw "Fixture not found: $FixturePath"
}

$Content = Get-Content $FixturePath -Raw
Write-Host "--- REPLAYING FIXTURE: $FixtureName (Profile: $Profile) ---" -ForegroundColor Cyan

& "$PSScriptRoot\filter-terminal-output.ps1" `
    -InputText $Content `
    -Profile $Profile `
    -Mode "filtered" `
    -CommandText "REPLAY $FixtureName" `
    -ExitCode 1 `
    -ShowBenchmark:$Benchmark
