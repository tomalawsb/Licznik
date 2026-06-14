$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

$Required = @(
    ".\app\src\main\java\pl\tomalawsb\licznik\MainActivity.java",
    ".\app\src\main\java\pl\tomalawsb\licznik\RouteMapView.java",
    ".\app\build.gradle",
    ".\tools\apply_ui_v27.py",
    ".\tools\compass_v27.b64"
)

foreach ($Path in $Required) {
    if (!(Test-Path $Path)) {
        Write-Host "BRAK PLIKU: $Path" -ForegroundColor Red
        Write-Host "Rozpakuj zawartosc paczki bezposrednio do glownego folderu projektu Licznik." -ForegroundColor Yellow
        pause
        exit 1
    }
}

$Backup = ".\backup_przed_2.7_" + (Get-Date -Format "yyyyMMdd_HHmmss")
New-Item -ItemType Directory -Path $Backup -Force | Out-Null

Copy-Item ".\app\src\main\java\pl\tomalawsb\licznik\MainActivity.java" $Backup
Copy-Item ".\app\src\main\java\pl\tomalawsb\licznik\RouteMapView.java" $Backup
Copy-Item ".\app\build.gradle" $Backup
if (Test-Path ".\README.md") { Copy-Item ".\README.md" $Backup }
if (Test-Path ".\.github\workflows\android-build.yml") {
    Copy-Item ".\.github\workflows\android-build.yml" $Backup
}

$Python = $null
if (Get-Command py -ErrorAction SilentlyContinue) {
    $Python = "py"
} elseif (Get-Command python -ErrorAction SilentlyContinue) {
    $Python = "python"
} else {
    Write-Host "Nie znaleziono Pythona. Zainstaluj Python 3 i zaznacz Add Python to PATH." -ForegroundColor Red
    pause
    exit 1
}

& $Python ".\tools\apply_ui_v27.py"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Nie udalo sie zastosowac poprawek." -ForegroundColor Red
    Write-Host "Kopia poprzednich plikow: $Backup" -ForegroundColor Yellow
    pause
    exit 1
}

$Workflow = Get-Content ".\.github\workflows\android-build.yml" -Raw
if ($Workflow -notmatch 'TAG="v2\.7-1406262027"') {
    Write-Host "BLAD: workflow nadal nie publikuje wersji 2.7." -ForegroundColor Red
    pause
    exit 1
}

Write-Host ""
Write-Host "GOTOWE — poprawiono kod i publikowanie aktualizacji." -ForegroundColor Green
Write-Host "Wersja aplikacji: 2.7 - 1406262027" -ForegroundColor Green
Write-Host "Release: v2.7-1406262027" -ForegroundColor Green
Write-Host "Plik APK w Release: Licznik-v2.7-1406262027.apk" -ForegroundColor Green
Write-Host ""
Write-Host "Teraz uruchom swoj upload_to_github.ps1, aby wyslac zmiany." -ForegroundColor Cyan
Write-Host ""
pause
