param(
    [Parameter(ValueFromPipeline = $true)]
    [string]$InputText,
    [string]$TeeDir = "$PSScriptRoot\logs",
    [int]$MaxFiles = 50
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path $TeeDir)) {
    New-Item -Path $TeeDir -ItemType Directory -Force | Out-Null
}

# 1. Generate filename: YYYYMMDD-HHMMSS-RANDOM.log
$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$Random = -join ((48..57) + (97..122) | Get-Random -Count 4 | ForEach-Object { [char]$_ })
$Filename = "raw-$Timestamp-$Random.log"
$FilePath = Join-Path $TeeDir $Filename

# 2. Save content
$InputText | Out-File -FilePath $FilePath -Encoding utf8

# 3. Simple rotation: keep latest N files
$Files = Get-ChildItem -Path $TeeDir -Filter "raw-*.log" | Sort-Object LastWriteTime -Descending
if ($Files.Count -gt $MaxFiles) {
    $Files | Select-Object -Skip $MaxFiles | Remove-Item -Force
}

# 4. Return the path
Write-Output $FilePath
