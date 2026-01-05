# Deploy script for AI Agent Platform
# Usage: .\deploy.ps1 [service]
# Examples:
#   .\deploy.ps1          # Rebuild all
#   .\deploy.ps1 backend  # Rebuild backend only
#   .\deploy.ps1 frontend # Rebuild frontend only

param(
    [string]$Service = ""
)

$ComposeFile = "docker-compose.tunnel.yml"

Write-Host "=== AI Agent Platform Deploy ===" -ForegroundColor Cyan

if ($Service -eq "") {
    Write-Host "Rebuilding all services..." -ForegroundColor Yellow
    docker-compose -f $ComposeFile up -d --build
} else {
    Write-Host "Rebuilding $Service..." -ForegroundColor Yellow
    docker-compose -f $ComposeFile up -d --build $Service
}

Write-Host ""
Write-Host "Deploy complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Services status:" -ForegroundColor Cyan
docker-compose -f $ComposeFile ps
