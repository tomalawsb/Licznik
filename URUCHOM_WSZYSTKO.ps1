# URUCHOM_WSZYSTKO.ps1
# Licznik jazdy 3.2
# FIX-DROPBOX:
# Gdy projekt jest w Dropbox/OneDrive, Git czasem nie moze zapisac plikow .git/objects/pack.
# Dlatego klon roboczy tworzony jest poza Dropboxem: w %LOCALAPPDATA%\Temp.

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

$RepoUrl = "https://github.com/tomalawsb/Licznik.git"
$BranchName = "main"
$VersionApk = "Licznik-v3.2-2906260712.apk"

function Fail([string]$Message) {
    Write-Host ""
    Write-Host "BLAD: $Message" -ForegroundColor Red
    Write-Host ""
    pause
    exit 1
}

function Info([string]$Message) {
    Write-Host $Message -ForegroundColor Cyan
}

function Ok([string]$Message) {
    Write-Host $Message -ForegroundColor Green
}

$Required = @(
    ".\settings.gradle",
    ".\build.gradle",
    ".\app\build.gradle",
    ".\app\src\main\AndroidManifest.xml",
    ".\app\src\main\java\pl\tomalawsb\licznik\MainActivity.java",
    ".\app\src\main\java\pl\tomalawsb\licznik\RouteMapView.java",
    ".\app\src\main\res\drawable-nodpi\kompas_tarcza.png",
    ".\app\src\main\res\drawable-nodpi\kompas_igla_glowna.png",
    ".\app\src\main\res\drawable-nodpi\kompas_wskaznik_celu.png",
    ".\.github\workflows\android-build.yml"
)

foreach ($Path in $Required) {
    if (!(Test-Path $Path)) {
        Fail "Brak pliku: $Path"
    }
}

if (!(Get-Command git -ErrorAction SilentlyContinue)) {
    Fail "Nie znaleziono git w PATH."
}

$Main = Get-Content ".\app\src\main\java\pl\tomalawsb\licznik\MainActivity.java" -Raw
if ($Main -notmatch 'VERSION_NAME = "3\.2 - 2906260712"') {
    Fail "MainActivity.java nie ma wersji 3.2."
}
if ($Main -notmatch 'R\.drawable\.kompas_tarcza') {
    Fail "Nie znaleziono nowego kompasu warstwowego w MainActivity.java."
}

$Workflow = Get-Content ".\.github\workflows\android-build.yml" -Raw
if ($Workflow -notmatch 'gradle assembleRelease') {
    Fail "Workflow nie buduje APK z kodu."
}
if ($Workflow -notmatch [regex]::Escape($VersionApk)) {
    Fail "Workflow nie publikuje APK 3.2."
}

$env:GIT_TERMINAL_PROMPT = "0"
$env:GCM_INTERACTIVE = "Never"

$UseCurrentRepo = Test-Path ".\.git"
$PublishDir = $Root

if ($UseCurrentRepo) {
    Info "Folder jest repozytorium Git - wysylam bezposrednio z tego folderu."
    $PublishDir = $Root
} else {
    Info "Ten folder nie ma .git."
    Info "Tworze klon roboczy poza Dropboxem/OneDrive: w folderze TEMP."

    if ($env:LOCALAPPDATA) {
        $TempBase = Join-Path $env:LOCALAPPDATA "Temp"
    } elseif ($env:TEMP) {
        $TempBase = $env:TEMP
    } else {
        $TempBase = "C:\Temp"
    }

    if (!(Test-Path $TempBase)) {
        New-Item -ItemType Directory -Path $TempBase -Force | Out-Null
    }

    $PublishDir = Join-Path $TempBase ("Licznik_publish_v32_" + (Get-Date -Format "yyyyMMdd_HHmmss"))

    Info "Klonuje repo do: $PublishDir"
    git clone $RepoUrl $PublishDir
    if ($LASTEXITCODE -ne 0) {
        Fail "Nie udalo sie sklonowac repozytorium. Jesli ten blad sie powtorzy, uruchom PowerShell jako administrator albo sprawdz antywirusa."
    }

    Set-Location $PublishDir
    git checkout $BranchName
    if ($LASTEXITCODE -ne 0) {
        Fail "Nie udalo sie przelaczyc na branch main."
    }

    Set-Location $Root

    Info "Kopiuje poprawiony projekt do klonu roboczego..."
    robocopy $Root $PublishDir /E /XD ".git" ".gradle" "build" "app\build" "Licznik_publish_v32_*" /XF "*.apk" | Out-Host
    if ($LASTEXITCODE -gt 7) {
        Fail "Robocopy nie skopiowal poprawnie projektu do klonu."
    }
}

Set-Location $PublishDir

$Origin = ""
try { $Origin = (git remote get-url origin) } catch { $Origin = "" }
if ([string]::IsNullOrWhiteSpace($Origin)) {
    git remote add origin $RepoUrl
}

git status | Out-Host
git add .

git commit -m "Licznik 3.2: kompas na mapie, cel z mapy, poprawiona duza mapa"
$CommitExit = $LASTEXITCODE
if ($CommitExit -ne 0) {
    Write-Host "Brak nowych zmian do commita albo commit juz istnieje. Probuje wyslac aktualny stan." -ForegroundColor Yellow
}

git push origin $BranchName
if ($LASTEXITCODE -ne 0) {
    Fail "Nie udalo sie wyslac zmian na GitHub. Sprawdz uprawnienia i zapisane logowanie Git."
}

Write-Host ""
Ok "GOTOWE."
Ok "Zmiany wyslane na GitHub."
Ok "GitHub Actions zbuduje APK: $VersionApk"
Write-Host ""
Write-Host "Klon roboczy zostal tutaj:" -ForegroundColor Yellow
Write-Host $PublishDir -ForegroundColor Yellow
Write-Host ""
Write-Host "Sprawdz zakladke Actions albo Release w repozytorium." -ForegroundColor Cyan
Write-Host ""

pause
