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
    [string]$RawOutputRef,
    [switch]$ShowBenchmark
)

$ErrorActionPreference = 'Continue'

function Strip-Ansi {
    param([string]$Text)
    return $Text -replace "\x1B\[[0-9;]*[a-zA-Z]", ""
}

# 1. Stats Gathering
$RawLineCount = ($InputText -split "`r?`n").Count
$RawCharCount = $InputText.Length
$EstRawTokens = [math]::Ceiling($RawCharCount / 4) # Rough LLM token estimation

# 2. Load Rules
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
$InCollapseBlock = $false
$CurrentCollapseCount = 0

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

    # Stack/Block Collapse
    if ($ActiveProfile.collapseStack) {
        $MatchCollapse = $false
        foreach ($p in $Rules.collapsePatterns) {
            if ($ProcessedLine -match $p) { $MatchCollapse = $true; break }
        }

        if ($MatchCollapse) {
            $CurrentCollapseCount++
            if ($CurrentCollapseCount -gt $Rules.maxStackFrames) {
                if (-not $InCollapseBlock) {
                    $FilteredLines.Add("    ... [COLLAPSED REPETITIVE STACK FRAMES]")
                    $InCollapseBlock = $true
                }
                continue
            }
        } else {
            $InCollapseBlock = $false
            $CurrentCollapseCount = 0
        }
    }

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

    # Truncate Long Lines
    if ($ProcessedLine.Length -gt $Rules.truncateLongLineAt) {
        $ProcessedLine = $ProcessedLine.Substring(0, $Rules.truncateLongLineAt) + "... [TRUNCATED]"
    }

    $FilteredLines.Add($ProcessedLine)
}

# 4. Truncate Output
$MAX_HEAD_LINES = 50
$MAX_TAIL_LINES = 20
$TRUNCATION_THRESHOLD = $MAX_HEAD_LINES + $MAX_TAIL_LINES

if ($FilteredLines.Count -gt $MaxLines) {
    $Head = $FilteredLines.GetRange(0, [math]::Min($MAX_HEAD_LINES, $FilteredLines.Count))
    $Tail = $FilteredLines.GetRange($FilteredLines.Count - $MAX_TAIL_LINES, $MAX_TAIL_LINES)
    $FilteredLines = $Head + "--- [TRUNCATED $( $FilteredLines.Count - $TRUNCATION_THRESHOLD ) LINES] ---" + $Tail
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

if ($ShowBenchmark) {
    $FilteredCharCount = $FinalOutput.Length
    $FilteredLineCount = ($FinalOutput -split "`n").Count
    $EstFilteredTokens = [math]::Ceiling($FilteredCharCount / 4)
    $ReductionPct = [math]::Round((1 - ($FilteredCharCount / $RawCharCount)) * 100, 2)

    $Bench = @"

--- RTK BENCHMARK ---
RAW_LINES: $RawLineCount
FILTERED_LINES: $FilteredLineCount
RAW_CHARS: $RawCharCount
FILTERED_CHARS: $FilteredCharCount
EST_RAW_TOKENS: $EstRawTokens
EST_FILTERED_TOKENS: $EstFilteredTokens
REDUCTION: $ReductionPct%
TEE_SAVED: $([bool]$RawOutputRef)
"@
    $FinalOutput += $Bench
}

Write-Output $FinalOutput
