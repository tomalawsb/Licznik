$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

Write-Host "Ten skrypt ze starszych paczek nie jest już używany." -ForegroundColor Yellow
Write-Host "Dla tej wersji uruchom: .\URUCHOM_WSZYSTKO.ps1" -ForegroundColor Cyan
Write-Host ""

if (Test-Path ".\URUCHOM_WSZYSTKO.ps1") {
    & ".\URUCHOM_WSZYSTKO.ps1"
} else {
    Write-Host "Brak pliku URUCHOM_WSZYSTKO.ps1" -ForegroundColor Red
    pause
    exit 1
}
