# Gayakini Development Utility for PowerShell
# Usage: .\dev.ps1 <command>

param (
    [Parameter(Mandatory=$true)]
    [ValidateSet("infra-up", "infra-down", "app-run", "dev-stack", "dev-down", "clean", "help")]
    $Command
)

# Load .env into current session environment
if (Test-Path ".env") {
    Get-Content .env | Where-Object { $_ -match '=' -and $_ -notmatch '^#' } | ForEach-Object {
        $name, $value = $_.Split('=', 2)
        [System.Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim().Trim('"').Trim("'"), "Process")
    }
}

switch ($Command) {
    "infra-up" {
        Write-Host "🚀 Starting local infrastructure (DB/Mailpit)..." -ForegroundColor Cyan
        docker compose up -d
    }
    "infra-down" {
        Write-Host "🛑 Stopping local infrastructure..." -ForegroundColor Yellow
        docker compose down
    }
    "app-run" {
        $Profile = if ($env:SPRING_PROFILES_ACTIVE) { $env:SPRING_PROFILES_ACTIVE } else { "local" }
        Write-Host "☕ Running app locally with profile: [$Profile]" -ForegroundColor Green
        ./gradlew bootRun
    }
    "dev-stack" {
        Write-Host "🐳 Starting full containerized dev stack..." -ForegroundColor Cyan
        docker compose --profile full up -d --build
    }
    "dev-down" {
        Write-Host "🛑 Stopping dev stack..." -ForegroundColor Yellow
        docker compose --profile full down
    }
    "clean" {
        Write-Host "🧹 Cleaning up build artifacts and Docker system..." -ForegroundColor Yellow
        ./gradlew clean
        docker system prune -f
    }
    "help" {
        Write-Host "Gayakini CLI Commands:" -ForegroundColor White
        Write-Host "  infra-up   : Run DB and Mailpit only"
        Write-Host "  infra-down : Stop infrastructure"
        Write-Host "  app-run    : Run application on host via Gradle"
        Write-Host "  dev-stack  : Run everything in Docker (App + Infra)"
        Write-Host "  dev-down   : Stop Docker stack"
        Write-Host "  clean      : Clean build and Docker"
    }
}
