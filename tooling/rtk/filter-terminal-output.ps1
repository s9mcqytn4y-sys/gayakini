param(
    [Parameter(ValueFromPipeline = $true)]
    [string]$InputText,
    [string]$Profile = "balanced",
    [int]$MaxLines = 100,
    [int]$MaxChars = 10000,
    [string]$Mode = "filtered",
    [string]$RulesPath = "$PSScriptRoot\rtk-rules.json",
    [switch]$PassThruOnParseError,
    [string]$CommandText,
    [int]$ExitCode = 0,
    [string]$RawOutputRef
)

$ErrorActionPreference = 'Continue'

function Strip-Ansi {
    param([string]$Text)
    return $Text -replace "\x1B\[[0-9;]*[a-zA-Z]", ""
}

# 1. Load Rules
try {
    if (-not (Test-Path $RulesPath)) { throw "Rules file not found at $RulesPath" }
    $Rules = Get-Content $RulesPath -Raw | ConvertFrom-Json
} catch {
    if ($PassThruOnParseError) {
        Write-Output $InputText
        return
    }
    throw $_
}

# 2. Extract Profile
$ActiveProfile = $Rules.profiles.$Profile
if ($null -eq $ActiveProfile) { $ActiveProfile = $Rules.profiles.balanced }

if ($Mode -eq "passthrough") {
    Write-Output $InputText
    return
}

# 3. Processing
$Lines = $InputText -split "`r?`n"
$FilteredLines = New-Object System.Collections.Generic.List[string]

$DedupeMap = @{}
$RepeatedCount = 0

foreach ($Line in $Lines) {
    $ProcessedLine = $Line
    if ($ActiveProfile.stripAnsi) { $ProcessedLine = Strip-Ansi $ProcessedLine }

    # Drop Progress/Spam
    $IsProgress = $false
    foreach ($p in $Rules.progressPatterns) {
        if ($ProcessedLine -match $p) { $IsProgress = $true; break }
    }
    if ($IsProgress) { continue }

    # Drop Noisy
    $IsNoisy = $false
    if ($ActiveProfile.dropNoisy) {
        foreach ($p in $Rules.noisyPatterns) {
            if ($ProcessedLine -match $p) { $IsNoisy = $true; break }
        }
    }
    if ($IsNoisy) { continue }

    # Deduplication
    if ($ActiveProfile.dedupe) {
        $Key = $ProcessedLine.Trim()
        if ($DedupeMap.ContainsKey($Key)) {
            $DedupeMap[$Key]++
            if ($DedupeMap[$Key] -gt $Rules.maxRepeatedLines) { continue }
        } else {
            $DedupeMap[$Key] = 1
        }
    }

    # Stack Collapse
    if ($ActiveProfile.collapseStack) {
        $IsStack = $false
        foreach ($p in $Rules.collapsePatterns) {
            if ($ProcessedLine -match $p) { $IsStack = $true; break }
        }
        # Simplified: just keep first few if it looks like a stack trace block
        # Real implementation would be stateful, but for now we just filter based on patterns
    }

    # Truncate Long Lines
    if ($ProcessedLine.Length -gt $Rules.truncateLongLineAt) {
        $ProcessedLine = $ProcessedLine.Substring(0, $Rules.truncateLongLineAt) + "... [TRUNCATED]"
    }

    $FilteredLines.Add($ProcessedLine)
}

# 4. Truncate Output
if ($FilteredLines.Count -gt $MaxLines) {
    $Head = $FilteredLines.GetRange(0, [math]::Min(50, $FilteredLines.Count))
    $Tail = $FilteredLines.GetRange($FilteredLines.Count - 20, 20)
    $FilteredLines = $Head + "--- [TRUNCATED $( $FilteredLines.Count - 70 ) LINES] ---" + $Tail
}

# 5. Format Output
$Summary = if ($ExitCode -eq 0) { "SUCCESS" } else { "FAILURE" }

$Output = New-Object System.Collections.Generic.List[string]
$Output.Add("COMMAND: $CommandText")
$Output.Add("SUMMARY: $Summary (Exit Code: $ExitCode)")

if ($RawOutputRef) {
    $Output.Add("RAW_OUTPUT_REF: $RawOutputRef")
}

$Output.Add("--- FILTERED OUTPUT ---")
foreach ($l in $FilteredLines) { $Output.Add($l) }

$FinalOutput = $Output -join "`n"
if ($FinalOutput.Length -gt $MaxChars) {
    $FinalOutput = $FinalOutput.Substring(0, $MaxChars) + "... [TOTAL CHAR LIMIT EXCEEDED]"
}

Write-Output $FinalOutput
