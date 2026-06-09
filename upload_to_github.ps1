# upload_to_github.ps1
# Licznik jazdy PWA
# Skrypt bezpieczny: pobiera aktualne repo do katalogu tymczasowego, kopiuje paczke programu i wysyla zmiany.

$ErrorActionPreference = "Stop"

$RepoUrl = "https://github.com/tomalawsb/Licznik.git"
$GitUserName = "Tomasz Wolak"
$GitUserEmail = "wolak82@gmail.com"
$DefaultCommitMessage = "Licznik jazdy PWA 1.1 - 0906260752"

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
Write-Host " Wysylanie projektu Licznik na GitHub"
Write-Host "========================================"

$ProjectPath = (Get-Location).Path
$TempRoot = Join-Path $env:TEMP "licznik_pwa_git_upload"
$RepoWorkPath = Join-Path $TempRoot "repo"

Info "Folder projektu: $ProjectPath"
Info "Repozytorium: $RepoUrl"

try { git --version | Out-Null } catch { Stop-WithMessage "Git nie jest zainstalowany albo nie jest dostepny w PATH." }

if (!(Test-Path (Join-Path $ProjectPath "index.html"))) { Stop-WithMessage "Brak index.html w folderze programu. Uruchom skrypt z glownego folderu paczki." }
if (!(Test-Path (Join-Path $ProjectPath "app.js"))) { Stop-WithMessage "Brak app.js w folderze programu. Nie wysylam niepelnej paczki." }
if (!(Test-Path (Join-Path $ProjectPath "style.css"))) { Stop-WithMessage "Brak style.css w folderze programu. Nie wysylam niepelnej paczki." }
if (!(Test-Path (Join-Path $ProjectPath "manifest.webmanifest"))) { Stop-WithMessage "Brak manifest.webmanifest w folderze programu. Nie wysylam niepelnej paczki." }
if (!(Test-Path (Join-Path $ProjectPath "service-worker.js"))) { Stop-WithMessage "Brak service-worker.js w folderze programu. Nie wysylam niepelnej paczki." }

Info "Ustawiam autora Git..."
git config --global user.name "$GitUserName"
git config --global user.email "$GitUserEmail"
Ok "Autor Git: $GitUserName <$GitUserEmail>"

Info "Czyszcze katalog tymczasowy..."
if (Test-Path $TempRoot) { Remove-Item $TempRoot -Recurse -Force }
New-Item -ItemType Directory -Path $TempRoot | Out-Null

Info "Pobieram aktualne repozytorium z GitHuba..."
git clone $RepoUrl $RepoWorkPath
if ($LASTEXITCODE -ne 0) { Stop-WithMessage "Nie udalo sie pobrac repozytorium. Sprawdz adres repo albo logowanie GitHub." }

Info "Kopiuje aktualna paczke programu do repozytorium..."
$RoboArgs = @(
    $ProjectPath,
    $RepoWorkPath,
    "/MIR",
    "/XD", ".git", ".github", "node_modules", "__pycache__",
    "/XF", "*.pyc", ".DS_Store", "*.zip"
)
robocopy @RoboArgs | Out-Null
$RoboCode = $LASTEXITCODE
if ($RoboCode -gt 7) { Stop-WithMessage "Robocopy nie skopiowal poprawnie plikow. Kod: $RoboCode" }

Set-Location $RepoWorkPath

git config core.autocrlf false
git branch -M main

Info "Sprawdzam wymagane pliki po skopiowaniu..."
if (!(Test-Path "index.html")) { Stop-WithMessage "Po kopiowaniu brakuje index.html." }
if (!(Test-Path "app.js")) { Stop-WithMessage "Po kopiowaniu brakuje app.js." }
if (!(Test-Path "style.css")) { Stop-WithMessage "Po kopiowaniu brakuje style.css." }
if (!(Test-Path "manifest.webmanifest")) { Stop-WithMessage "Po kopiowaniu brakuje manifest.webmanifest." }
if (!(Test-Path "service-worker.js")) { Stop-WithMessage "Po kopiowaniu brakuje service-worker.js." }

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
Write-Host "Adres strony po wlaczeniu GitHub Pages: https://tomalawsb.github.io/Licznik/?v=1_1_0906260752"
Write-Host "========================================"
