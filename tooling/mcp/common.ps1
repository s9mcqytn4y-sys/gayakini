Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# Resolve repository root
function Get-RepoRoot {
    $Root = (Resolve-Path "$PSScriptRoot\..\..").Path
    return $Root
}

# Assert required commands exist
function Assert-Command {
    param([string]$Command)
    if (-not (Get-Command $Command -ErrorAction SilentlyContinue)) {
        Write-Error "Required command '$Command' not found in PATH."
    }
}

# Load environment variables from .env files
function Import-EnvFile {
    param([string]$Path)
    if (Test-Path $Path) {
        Write-Host "Loading environment from $Path"
        Get-Content $Path | ForEach-Object {
            $line = $_.Trim()
            if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
                $pos = $line.IndexOf('=')
                $name = $line.Substring(0, $pos).Trim()
                $value = $line.Substring($pos + 1).Trim()
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
        [string[]]$Args = @()
    )
    Assert-Command "npx.cmd"
    Write-Host "Starting MCP server: $Package"
    # Use -y to bypass interactive prompt
    npx.cmd -y $Package @Args
}
