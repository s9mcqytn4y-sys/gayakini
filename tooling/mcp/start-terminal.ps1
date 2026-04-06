param(
    [switch]$ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\common.ps1"

Load-McpEnvironment

if ($ValidateOnly) {
    Write-Output "Terminal launcher ready."
}

Start-McpNpx -Package "@modelcontextprotocol/server-terminal" -ValidateOnly:$ValidateOnly
