param(
    [ValidateSet('default', 'chrome', 'edge')]
    [string]$BrowserPreference,
    [string]$BaseUrl,
    [string]$BrowserExecutablePath,
    [string]$UserDataDir,
    [Nullable[bool]]$Headless,
    [switch]$AllowDangerous,
    [switch]$ValidateOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. "$PSScriptRoot\common.ps1"

Load-McpEnvironment

$ResolvedBaseUrl = Assert-HttpUrl -Url (Get-FirstValue -Values @(
    $BaseUrl,
    (Get-EnvironmentValue -Names @('BROWSER_BASE_URL', 'APP_BASE_URL', 'API_BASE_URL'))
) -Default 'http://localhost:8080') -Label 'Browser base URL'

$ResolvedBrowserPreference = Get-FirstValue -Values @(
    $BrowserPreference,
    (Get-EnvironmentValue -Names @('BROWSER_PREFERENCE', 'BROWSER_TYPE'))
) -Default 'default'

switch ($ResolvedBrowserPreference.ToLowerInvariant()) {
    'edge' {
        $DefaultExecutableCandidates = @(
            'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe',
            'C:\Program Files\Microsoft\Edge\Application\msedge.exe'
        )
    }
    'chrome' {
        $DefaultExecutableCandidates = @(
            'C:\Program Files\Google\Chrome\Application\chrome.exe',
            'C:\Program Files (x86)\Google\Chrome\Application\chrome.exe'
        )
    }
    default {
        $DefaultExecutableCandidates = @()
    }
}

$ResolvedExecutablePath = Get-FirstValue -Values @(
    $BrowserExecutablePath,
    (Get-EnvironmentValue -Names @('BROWSER_EXECUTABLE_PATH', 'CHROME_PATH', 'CHROME_BIN', 'EDGE_PATH', 'MS_EDGE_PATH')),
    $(foreach ($Candidate in $DefaultExecutableCandidates) {
        if (Test-Path -LiteralPath $Candidate -PathType Leaf) {
            $Candidate
            break
        }
    })
)

if (-not [string]::IsNullOrWhiteSpace($ResolvedExecutablePath)) {
    $ResolvedExecutablePath = Assert-File -Path $ResolvedExecutablePath -Label 'Browser executable'
} elseif ($ResolvedBrowserPreference -ne 'default') {
    throw "Requested browser preference '$ResolvedBrowserPreference' but no matching executable was found. Set BROWSER_EXECUTABLE_PATH explicitly."
}

$ResolvedUserDataDir = Get-FirstValue -Values @(
    $UserDataDir,
    (Get-EnvironmentValue -Names @('BROWSER_USER_DATA_DIR', 'PUPPETEER_USER_DATA_DIR'))
)

$ExplicitHeadless = $PSBoundParameters.ContainsKey('Headless')
$HasLaunchOverrides = $ExplicitHeadless -or
    (-not [string]::IsNullOrWhiteSpace($ResolvedExecutablePath)) -or
    (-not [string]::IsNullOrWhiteSpace($ResolvedUserDataDir))

if ($HasLaunchOverrides) {
    $LaunchOptions = @{}

    if ($ExplicitHeadless) {
        $LaunchOptions.headless = [bool]$Headless
    }

    if (-not [string]::IsNullOrWhiteSpace($ResolvedExecutablePath)) {
        $LaunchOptions.executablePath = $ResolvedExecutablePath
    }

    if (-not [string]::IsNullOrWhiteSpace($ResolvedUserDataDir)) {
        $LaunchOptions.args = @("--user-data-dir=$ResolvedUserDataDir")
    }

    [System.Environment]::SetEnvironmentVariable(
        'PUPPETEER_LAUNCH_OPTIONS',
        ($LaunchOptions | ConvertTo-Json -Compress -Depth 6),
        'Process'
    )
}

if ($AllowDangerous) {
    [System.Environment]::SetEnvironmentVariable('ALLOW_DANGEROUS', 'true', 'Process')
}

if ($ValidateOnly) {
    Write-Output "Browser target base URL: $ResolvedBaseUrl"
    Write-Output "Browser preference: $ResolvedBrowserPreference"
    if ($HasLaunchOverrides) {
        Write-Output 'Browser launch options: generated from parameters/env overrides'
    } elseif (-not [string]::IsNullOrWhiteSpace((Get-EnvironmentValue -Names @('PUPPETEER_LAUNCH_OPTIONS')))) {
        Write-Output 'Browser launch options: using existing PUPPETEER_LAUNCH_OPTIONS'
    } else {
        Write-Output 'Browser launch options: package defaults'
    }

    if (-not [string]::IsNullOrWhiteSpace($ResolvedExecutablePath)) {
        Write-Output "Browser executable: $ResolvedExecutablePath"
    } else {
        Write-Output 'Browser executable: package-managed Chromium/installed default'
    }

    if ($AllowDangerous -or ((Get-EnvironmentValue -Names @('ALLOW_DANGEROUS')) -eq 'true')) {
        Write-Output 'Browser dangerous launch options: enabled'
    }
}

Start-McpNpx -Package '@modelcontextprotocol/server-puppeteer' -ValidateOnly:$ValidateOnly
