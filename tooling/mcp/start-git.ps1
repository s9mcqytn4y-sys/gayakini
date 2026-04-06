param(
    [string]$Repository,
    [switch]$ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\common.ps1"

Load-McpEnvironment

$RepositoryRoot = Assert-Directory -Path (Get-RepoRoot -Root $Repository) -Label 'Git repository root'
$null = Assert-Command "git.exe"

if (-not (Test-Path -LiteralPath (Join-Path -Path $RepositoryRoot -ChildPath '.git'))) {
    throw "Git repository metadata not found under: $RepositoryRoot"
}

if ($ValidateOnly) {
    Write-Output "Git repository: $RepositoryRoot"
}

Start-McpNpx -Package "@modelcontextprotocol/server-git" -Args @("--repository", $RepositoryRoot) -ValidateOnly:$ValidateOnly
