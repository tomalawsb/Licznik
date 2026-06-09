# upload_to_github.ps1
# Licznik Android
# Skrypt pobiera aktualne repo do katalogu tymczasowego, kopiuje projekt i wysyla zmiany.

$ErrorActionPreference = "Stop"

$RepoUrl = "https://github.com/tomalawsb/Licznik.git"
$GitUserName = "Tomasz Wolak"
$GitUserEmail = "wolak82@gmail.com"
$DefaultCommitMessage = "Licznik Android v2.4 - 0906261645 nowy interfejs"

function Stop-WithMessage($Message) {
    Write-Host ""
    Write-Host "BLAD: $Message" -ForegroundColor Red
    Write-Host ""
    Set-Location $ProjectPath -ErrorAction SilentlyContinue
    exit 1
}

function Info($Message) { Write-Host $Message -ForegroundColor Cyan }
function Ok($Message) { Write-Host $Message -ForegroundColor Green }
function Warn($Message) { Write-Host $Message -ForegroundColor Yellow }

Write-Host "========================================"
Write-Host " Wysylanie Licznika Android na GitHub"
Write-Host "========================================"

$ProjectPath = (Get-Location).Path
$TempRoot = Join-Path $env:TEMP "licznik_android_git_upload"
$RepoWorkPath = Join-Path $TempRoot "repo"

Info "Folder projektu: $ProjectPath"
Info "Repozytorium: $RepoUrl"

try { git --version | Out-Null } catch { Stop-WithMessage "Git nie jest zainstalowany albo nie jest dostepny w PATH." }

if (!(Test-Path (Join-Path $ProjectPath "settings.gradle"))) { Stop-WithMessage "Brak settings.gradle. Uruchom skrypt z glownego folderu projektu." }
if (!(Test-Path (Join-Path $ProjectPath "app\src\main\AndroidManifest.xml"))) { Stop-WithMessage "Brak AndroidManifest.xml. Nie wysylam niepelnego projektu." }
if (!(Test-Path (Join-Path $ProjectPath "app\src\main\java\pl\tomalawsb\licznik\RideTrackingService.java"))) { Stop-WithMessage "Brak RideTrackingService.java. To jest wymagane do pracy GPS w tle." }

Info "Ustawiam autora Git..."
git config --global user.name "$GitUserName"
git config --global user.email "$GitUserEmail"
Ok "Autor Git: $GitUserName <$GitUserEmail>"

Info "Czyszcze katalog tymczasowy..."
if (Test-Path $TempRoot) { Remove-Item $TempRoot -Recurse -Force }
New-Item -ItemType Directory -Path $TempRoot | Out-Null

Info "Pobieram aktualne repozytorium z GitHuba..."
git clone $RepoUrl $RepoWorkPath
if ($LASTEXITCODE -ne 0) { Stop-WithMessage "Nie udalo sie pobrac repozytorium." }

Info "Kopiuje aktualny projekt do repozytorium..."
$RoboArgs = @(
    $ProjectPath,
    $RepoWorkPath,
    "/MIR",
    "/XD", ".git", ".gradle", ".idea", "build", "app\build",
    "/XF", "*.iml", "local.properties", ".DS_Store"
)
robocopy @RoboArgs | Out-Null
$RoboCode = $LASTEXITCODE
if ($RoboCode -gt 7) { Stop-WithMessage "Robocopy nie skopiowal poprawnie plikow. Kod: $RoboCode" }

Set-Location $RepoWorkPath
git config core.autocrlf false
git branch -M main

Info "Sprawdzam wymagane pliki po skopiowaniu..."
if (!(Test-Path "settings.gradle")) { Stop-WithMessage "Po kopiowaniu brakuje settings.gradle." }
if (!(Test-Path "app\src\main\AndroidManifest.xml")) { Stop-WithMessage "Po kopiowaniu brakuje AndroidManifest.xml." }
if (!(Test-Path "app\src\main\java\pl\tomalawsb\licznik\RideTrackingService.java")) { Stop-WithMessage "Po kopiowaniu brakuje RideTrackingService.java." }

Info "Dodaje pliki..."
git add -A

$Status = git status --porcelain
if ([string]::IsNullOrWhiteSpace($Status)) {
    Warn "Brak zmian do wyslania. Repozytorium jest juz aktualne."
    Set-Location $ProjectPath
    exit 0
}

Info "Zmiany wykryte przez Git:"
git status --short

$CommitMessage = $DefaultCommitMessage
Info "Tworze commit: $CommitMessage"
git commit -m "$CommitMessage"
if ($LASTEXITCODE -ne 0) { Stop-WithMessage "Nie udalo sie utworzyc commita." }

Info "Wysylam na GitHub..."
git push origin main
if ($LASTEXITCODE -ne 0) { Stop-WithMessage "Nie udalo sie wyslac projektu na GitHub. Sprawdz logowanie GitHub/Git Credential Manager." }

Set-Location $ProjectPath

Write-Host "========================================"
Ok "Gotowe. Projekt zostal wyslany na GitHub."
Write-Host "Po wyslaniu GitHub Actions zbuduje APK release i utworzy GitHub Release. Pobierz artifact Licznik-release-apk albo plik APK z Releases."
Write-Host "========================================"
