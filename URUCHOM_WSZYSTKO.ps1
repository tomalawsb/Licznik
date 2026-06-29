# URUCHOM_WSZYSTKO.ps1
# Licznik jazdy 3.10.1 - FIX TABS/HISTORIA
# Poprawia plik GitHub Actions i wysyla projekt na GitHub.
# Klon roboczy jest tworzony poza Dropboxem/OneDrive: %LOCALAPPDATA%\Temp.

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

$RepoUrl = "https://github.com/tomalawsb/Licznik.git"
$BranchName = "main"
$VersionApk = "Licznik-v3.10.1-290626-tabs-fix.apk"

function Fail([string]$Message) {
    Write-Host ""
    Write-Host "BLAD: $Message" -ForegroundColor Red
    Write-Host ""
    pause
    exit 1
}

function Info([string]$Message) { Write-Host $Message -ForegroundColor Cyan }
function Ok([string]$Message) { Write-Host $Message -ForegroundColor Green }

$Required = @(
    ".\settings.gradle",
    ".\build.gradle",
    ".\app\build.gradle",
    ".\app\src\main\AndroidManifest.xml",
    ".\app\src\main\java\pl\tomalawsb\licznik\MainActivity.java",
    ".\app\src\main\java\pl\tomalawsb\licznik\RouteMapView.java",
    ".\.github\workflows\android-build.yml"
)

foreach ($Path in $Required) {
    if (!(Test-Path $Path)) { Fail "Brak pliku: $Path" }
}

if (!(Get-Command git -ErrorAction SilentlyContinue)) { Fail "Nie znaleziono git w PATH." }

$Main = Get-Content ".\app\src\main\java\pl\tomalawsb\licznik\MainActivity.java" -Raw
if ($Main -notmatch 'VERSION_NAME = "3\.10\.1 - 290626-tabs-fix"') { Fail "MainActivity.java nie ma wersji 3.10.1." }

$Gradle = Get-Content ".\app\build.gradle" -Raw
if ($Gradle -notmatch "versionCode 31001") { Fail "app/build.gradle nie ma versionCode 31001." }
if ($Gradle -notmatch "versionName '3\.10\.1 - 290626-tabs-fix'") { Fail "app/build.gradle nie ma versionName 3.10.1." }

$Workflow = Get-Content ".\.github\workflows\android-build.yml" -Raw
if ($Workflow -match "printf '\r?\n") { Fail "Workflow ma stary bledny printf rozbijajacy YAML. Uzyj tej poprawionej paczki." }
if ($Workflow -notmatch 'softprops/action-gh-release@v2') { Fail "Workflow nie ma poprawionego publikowania Release." }
if ($Workflow -notmatch 'gradle assembleRelease') { Fail "Workflow nie buduje APK z kodu." }
if ($Workflow -notmatch [regex]::Escape($VersionApk)) { Fail "Workflow nie publikuje APK 3.10.1." }

$env:GIT_TERMINAL_PROMPT = "0"
$env:GCM_INTERACTIVE = "Never"

$UseCurrentRepo = Test-Path ".\.git"
$PublishDir = $Root

if ($UseCurrentRepo) {
    Info "Folder jest repozytorium Git - wysylam bezposrednio z tego folderu."
    $PublishDir = $Root
} else {
    Info "Ten folder nie ma .git. Tworze klon roboczy poza Dropboxem/OneDrive."

    if ($env:LOCALAPPDATA) { $TempBase = Join-Path $env:LOCALAPPDATA "Temp" }
    elseif ($env:TEMP) { $TempBase = $env:TEMP }
    else { $TempBase = "C:\Temp" }

    if (!(Test-Path $TempBase)) { New-Item -ItemType Directory -Path $TempBase -Force | Out-Null }

    $PublishDir = Join-Path $TempBase ("Licznik_publish_v3101_fix_" + (Get-Date -Format "yyyyMMdd_HHmmss"))

    Info "Klonuje repo do: $PublishDir"
    git clone $RepoUrl $PublishDir
    if ($LASTEXITCODE -ne 0) { Fail "Nie udalo sie sklonowac repozytorium." }

    Set-Location $PublishDir
    git checkout $BranchName
    if ($LASTEXITCODE -ne 0) { Fail "Nie udalo sie przelaczyc na branch main." }

    Set-Location $Root
    Info "Kopiuje poprawiony projekt do klonu roboczego..."
    robocopy $Root $PublishDir /E /XD ".git" ".gradle" "build" "app\build" "Licznik_publish_*" /XF "*.apk" | Out-Host
    if ($LASTEXITCODE -gt 7) { Fail "Robocopy nie skopiowal poprawnie projektu do klonu." }
}

Set-Location $PublishDir

$Origin = ""
try { $Origin = (git remote get-url origin) } catch { $Origin = "" }
if ([string]::IsNullOrWhiteSpace($Origin)) { git remote add origin $RepoUrl }

git status | Out-Host
git add .

git commit -m "Licznik 3.10.1: napraw zakladki i historie"
$CommitExit = $LASTEXITCODE
if ($CommitExit -ne 0) {
    Write-Host "Brak nowych zmian do commita albo commit juz istnieje. Probuje wyslac aktualny stan." -ForegroundColor Yellow
}

git push origin $BranchName
if ($LASTEXITCODE -ne 0) { Fail "Nie udalo sie wyslac zmian na GitHub." }

Write-Host ""
Ok "GOTOWE."
Ok "Zmiany wyslane na GitHub."
Ok "GitHub Actions zbuduje APK: $VersionApk"
Write-Host ""
Write-Host "Teraz wejdz w Actions i sprawdz najnowszy run. Ma byc zielony." -ForegroundColor Cyan
Write-Host ""
pause
