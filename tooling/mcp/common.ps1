Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Get-FirstValue {
    param(
        [string[]]$Values = @(),
        [string]$Default = $null
    )

    foreach ($Value in $Values) {
        if (-not [string]::IsNullOrWhiteSpace($Value)) {
            return $Value
        }
    }

    return $Default
}

function Get-EnvironmentValue {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Names,
        [string]$Default = $null
    )

    $Values = foreach ($Name in $Names) {
        Get-FirstValue -Values @(
            [System.Environment]::GetEnvironmentVariable($Name, 'Process'),
            [System.Environment]::GetEnvironmentVariable($Name, 'User'),
            [System.Environment]::GetEnvironmentVariable($Name, 'Machine')
        )
    }

    return Get-FirstValue -Values $Values -Default $Default
}

# Resolve repository root
function Get-RepoRoot {
    param([string]$Root)

    $PreferredRoot = Get-FirstValue -Values @(
        $Root,
        (Get-EnvironmentValue -Names @('PROJECT_ROOT', 'REPO_ROOT'))
    )

    if (-not [string]::IsNullOrWhiteSpace($PreferredRoot)) {
        if (-not (Test-Path -LiteralPath $PreferredRoot)) {
            throw "Repository root not found: $PreferredRoot"
        }

        return (Resolve-Path -LiteralPath $PreferredRoot).Path
    }

    return (Resolve-Path "$PSScriptRoot\..\..").Path
}

# Assert required commands exist
function Assert-Command {
    param([string]$Command)

    $ResolvedCommand = Get-Command $Command -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $ResolvedCommand) {
        throw "Required command '$Command' not found in PATH."
    }

    return $ResolvedCommand.Source
}

function Assert-Directory {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [string]$Label = 'Directory'
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
        throw "$Label not found: $Path"
    }

    return (Resolve-Path -LiteralPath $Path).Path
}

function Format-CommandPreview {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Command,
        [string[]]$Arguments = @()
    )

    $Segments = @($Command) + $Arguments
    return ($Segments | ForEach-Object {
        if ($_ -match '\s') {
            '"' + ($_ -replace '"', '\"') + '"'
        } else {
            $_
        }
    }) -join ' '
}

function Get-NpxCommandPath {
    $ProgramFilesX86 = [System.Environment]::GetEnvironmentVariable('ProgramFiles(x86)', 'Process')
    if ([string]::IsNullOrWhiteSpace($ProgramFilesX86)) {
        $ProgramFilesX86 = [System.Environment]::GetEnvironmentVariable('ProgramFiles(x86)', 'Machine')
    }

    $Candidates = @(
        (Get-EnvironmentValue -Names @('MCP_NPX', 'NPX_PATH', 'NODEJS_NPX')),
        $(if ($env:ProgramFiles) { Join-Path -Path $env:ProgramFiles -ChildPath 'nodejs\npx.cmd' }),
        $(if (-not [string]::IsNullOrWhiteSpace($ProgramFilesX86)) { Join-Path -Path $ProgramFilesX86 -ChildPath 'nodejs\npx.cmd' }),
        $(if ($env:APPDATA) { Join-Path -Path $env:APPDATA -ChildPath 'npm\npx.cmd' }),
        $(if ($env:LOCALAPPDATA) { Join-Path -Path $env:LOCALAPPDATA -ChildPath 'Volta\bin\npx.cmd' }),
        $(if ($env:USERPROFILE) { Join-Path -Path $env:USERPROFILE -ChildPath 'scoop\shims\npx.cmd' })
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }

    foreach ($CommandName in @('npx.cmd', 'npx')) {
        $ResolvedCommand = Get-Command $CommandName -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($ResolvedCommand) {
            return $ResolvedCommand.Source
        }
    }

    foreach ($Candidate in $Candidates) {
        if (Test-Path -LiteralPath $Candidate -PathType Leaf) {
            return (Resolve-Path -LiteralPath $Candidate).Path
        }
    }

    throw "Unable to find npx.cmd. Install Node.js 18+ and ensure npx is on PATH, or set MCP_NPX to the full path of npx.cmd."
}

# Load environment variables from .env files
function Import-EnvFile {
    param([string]$Path)
    if (Test-Path $Path) {
        Get-Content $Path | ForEach-Object {
            $line = $_.Trim()
            if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
                $pos = $line.IndexOf('=')
                $name = $line.Substring(0, $pos).Trim()
                $value = $line.Substring($pos + 1).Trim()
                if ($name.StartsWith('export ')) {
                    $name = $name.Substring(7).Trim()
                }
                if ($name) {
                    # Remove surrounding quotes if present
                    if ($value.StartsWith('"') -and $value.EndsWith('"')) { $value = $value.Substring(1, $value.Length - 2) }
                    elseif ($value.StartsWith("'") -and $value.EndsWith("'")) { $value = $value.Substring(1, $value.Length - 2) }

                    [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
                }
            }
        }
    }
}

# Load environment files in order of priority
function Load-McpEnvironment {
    $RepoRoot = Get-RepoRoot
    # Priority: .env.mcp > local.env > .env
    Import-EnvFile "$RepoRoot\.env"
    Import-EnvFile "$RepoRoot\local.env"
    Import-EnvFile "$RepoRoot\.env.mcp"
}

# Start an npx-based MCP server safely
function Start-McpNpx {
    param(
        [Parameter(Mandatory=$true)] [string]$Package,
        [string[]]$Args = @(),
        [switch]$ValidateOnly
    )

    $NpxPath = Get-NpxCommandPath
    $InvocationArgs = @('-y', $Package) + $Args

    if ($ValidateOnly) {
        Write-Output ("Launcher check OK: " + (Format-CommandPreview -Command $NpxPath -Arguments $InvocationArgs))
        return
    }

    & $NpxPath @InvocationArgs
}
