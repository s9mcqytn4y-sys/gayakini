param(
    [string]$Token,
    [switch]$ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\common.ps1"

Load-McpEnvironment

$ResolvedToken = Get-FirstValue -Values @(
    $Token,
    (Get-EnvironmentValue -Names @('GITHUB_PERSONAL_ACCESS_TOKEN', 'GITHUB_TOKEN', 'GH_TOKEN'))
)

if ([string]::IsNullOrWhiteSpace($ResolvedToken)) {
    throw "GitHub token is missing. Set GITHUB_PERSONAL_ACCESS_TOKEN, GITHUB_TOKEN, or GH_TOKEN before starting the launcher."
}

[System.Environment]::SetEnvironmentVariable('GITHUB_PERSONAL_ACCESS_TOKEN', $ResolvedToken, 'Process')

if ($ValidateOnly) {
    Write-Output "GitHub token source resolved."
}

Start-McpNpx -Package "@modelcontextprotocol/server-github" -ValidateOnly:$ValidateOnly
