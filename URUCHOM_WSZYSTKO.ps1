# URUCHOM_WSZYSTKO.ps1
# Licznik jazdy 3.0
# Jeden skrypt: buduje APK, tworzy paczke, wysyla projekt przez Git
# i czeka, az GitHub Actions automatycznie opublikuje APK.
# Nie uzywa GitHub CLI i nie zadaje pytan.

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $Root

$VersionName = "3.0 - 1506260712"
$Tag = "v3.0-1506260712"
$VersionFileName = "Licznik-v3.0-1506260712.apk"
$RepoUrl = "https://github.com/tomalawsb/Licznik.git"
$RepoApi = "https://api.github.com/repos/tomalawsb/Licznik"
$BranchName = "main"
$GitUserName = "Tomasz Wolak"
$GitUserEmail = "wolwolak82@gmail.com"
$CommitMessage = "Licznik Android v3.0 - 1506260712: przyciski nizsze o 3 dp, kompas 100 dp przesuniety o 2 dp w gore i w prawo"

$BaseApk = Join-Path $Root "baseline\Licznik-v2.9-1406262155.apk"
$UnsignedApk = Join-Path $Root "output\Licznik-v3.0-1506260712-unsigned.apk"
$FinalApk = Join-Path $Root ("output\" + $VersionFileName)
$RootApk = Join-Path $Root $VersionFileName
$Package = Join-Path $Root ("output\Licznik-v3.0-1506260712-KOMPLETNY-PROJEKT.zip")
$KeyStore = Join-Path $Root "signing\licznik-release.jks"
$Password = "Licznik2026!"
$Alias = "licznik"

# Git ma korzystac z zapisanych danych Git Credential Manager.
# Brak pytan tekstowych i brak logowania przez GitHub CLI.
$env:GIT_TERMINAL_PROMPT = "0"
$env:GCM_INTERACTIVE = "Never"

function Info([string]$Message) {
    Write-Host $Message -ForegroundColor Cyan
}

function Ok([string]$Message) {
    Write-Host $Message -ForegroundColor Green
}

function Warn([string]$Message) {
    Write-Host $Message -ForegroundColor Yellow
}

function Stop-WithMessage([string]$Message) {
    Write-Host ""
    Write-Host "BLAD: $Message" -ForegroundColor Red
    Write-Host ""
    Set-Location $Root -ErrorAction SilentlyContinue
    exit 1
}

function Find-Executable([string]$Name, [string[]]$ExtraPaths) {
    $Command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($Command) {
        return $Command.Source
    }

    foreach ($Candidate in $ExtraPaths) {
        if ($Candidate -and (Test-Path -LiteralPath $Candidate)) {
            return $Candidate
        }
    }

    return $null
}

function Invoke-NativeChecked(
    [string]$Exe,
    [string[]]$CommandArguments,
    [string]$ErrorMessage
) {
    $PreviousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        & $Exe @CommandArguments
        $ExitCode = $LASTEXITCODE
    }
    catch {
        $ExitCode = 1
    }
    finally {
        $ErrorActionPreference = $PreviousPreference
    }

    if ($ExitCode -ne 0) {
        Stop-WithMessage $ErrorMessage
    }
}

function Invoke-NativeCapture(
    [string]$Exe,
    [string[]]$CommandArguments,
    [string]$ErrorMessage
) {
    $PreviousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $Output = & $Exe @CommandArguments 2>$null
        $ExitCode = $LASTEXITCODE
    }
    catch {
        $Output = $null
        $ExitCode = 1
    }
    finally {
        $ErrorActionPreference = $PreviousPreference
    }

    if ($ExitCode -ne 0) {
        Stop-WithMessage $ErrorMessage
    }

    return (($Output | Out-String).Trim())
}

function Get-PythonCommand {
    $Py = Get-Command py -ErrorAction SilentlyContinue
    if ($Py) {
        return @($Py.Source, "-3")
    }

    $Python = Get-Command python -ErrorAction SilentlyContinue
    if ($Python) {
        return @($Python.Source)
    }

    Stop-WithMessage "Nie znaleziono Pythona. Uruchom skrypt w PowerShellu z aktywnym srodowiskiem (base)."
}

function Invoke-PythonChecked(
    [string[]]$PythonCommand,
    [string[]]$CommandArguments,
    [string]$ErrorMessage
) {
    $Exe = $PythonCommand[0]
    $AllArguments = @()

    if ($PythonCommand.Count -gt 1) {
        $AllArguments += $PythonCommand[1..($PythonCommand.Count - 1)]
    }

    $AllArguments += $CommandArguments
    Invoke-NativeChecked $Exe $AllArguments $ErrorMessage
}

function Copy-DirectoryContent(
    [string]$Source,
    [string]$Destination,
    [string[]]$ExcludedNames
) {
    if (!(Test-Path -LiteralPath $Destination)) {
        New-Item -ItemType Directory -Path $Destination -Force | Out-Null
    }

    Get-ChildItem -LiteralPath $Source -Force | ForEach-Object {
        if ($ExcludedNames -notcontains $_.Name) {
            $Target = Join-Path $Destination $_.Name
            if ($_.PSIsContainer) {
                Copy-DirectoryContent $_.FullName $Target $ExcludedNames
            }
            else {
                Copy-Item -LiteralPath $_.FullName -Destination $Target -Force
            }
        }
    }
}

function Copy-ProjectToRepository([string]$Destination) {
    $Items = @(
        ".github",
        ".gitignore",
        "baseline",
        "reference",
        "tools",
        "README.md",
        "RELEASE_NOTES.md",
        "RAPORT_ZMIAN.md",
        "VALIDATION_RESULTS.txt",
        "SHA256SUMS.txt",
        "requirements.txt",
        "URUCHOM_WSZYSTKO.ps1",
        $VersionFileName
    )

    foreach ($Name in $Items) {
        $Source = Join-Path $Root $Name
        if (!(Test-Path -LiteralPath $Source)) {
            continue
        }

        $Target = Join-Path $Destination $Name
        $Item = Get-Item -LiteralPath $Source

        if ($Item.PSIsContainer) {
            if (Test-Path -LiteralPath $Target) {
                Remove-Item -LiteralPath $Target -Recurse -Force
            }
            Copy-DirectoryContent $Source $Target @(".git", ".tools", "output", "signing")
        }
        else {
            Copy-Item -LiteralPath $Source -Destination $Target -Force
        }
    }

    # Usuwamy poprzednie, rozdzielone skrypty. Zostaje tylko jeden plik PS1.
    @(
        "upload_to_github.ps1",
        "build_export_update.ps1"
    ) | ForEach-Object {
        $Obsolete = Join-Path $Destination $_
        if (Test-Path -LiteralPath $Obsolete) {
            Remove-Item -LiteralPath $Obsolete -Force
        }
    }
}

function Update-Sha256File {
    $Hash = (Get-FileHash -LiteralPath $RootApk -Algorithm SHA256).Hash.ToLowerInvariant()
    $Line = "$Hash  $VersionFileName"
    [System.IO.File]::WriteAllText(
        (Join-Path $Root "SHA256SUMS.txt"),
        $Line + [Environment]::NewLine,
        (New-Object System.Text.UTF8Encoding($false))
    )
}

function Wait-ForRelease([string]$CommitSha) {
    Info "[8/8] Czekam na automatyczna publikacje GitHub Release..."
    $Headers = @{ "User-Agent" = "Licznik-Automatyczna-Publikacja" }
    $ReleaseUrl = "$RepoApi/releases/tags/$Tag"
    $ExpectedCommitLine = "Commit: $CommitSha"

    for ($Attempt = 1; $Attempt -le 24; $Attempt++) {
        try {
            $Release = Invoke-RestMethod -Uri $ReleaseUrl -Headers $Headers -TimeoutSec 20
            $HasApk = $false

            foreach ($Asset in $Release.assets) {
                if ($Asset.name -eq $VersionFileName) {
                    $HasApk = $true
                    break
                }
            }

            if ($HasApk -and ([string]$Release.body).Contains($ExpectedCommitLine)) {
                Ok "GitHub Release zostal opublikowany automatycznie."
                return
            }
        }
        catch {
            # Workflow moze jeszcze pracowac. Ponawiamy bez pytan.
        }

        Write-Host ("  Oczekiwanie na GitHub Actions: " + $Attempt + "/24") -ForegroundColor DarkGray
        Start-Sleep -Seconds 15
    }

    Warn "Projekt zostal wyslany, ale GitHub Actions nie zakonczyl publikacji w ciagu 6 minut."
    Warn "Workflow nadal moze pracowac w tle."
}

Write-Host "==============================================" -ForegroundColor Cyan
Write-Host " LICZNIK - AUTOMATYCZNE BUDOWANIE I PUBLIKACJA" -ForegroundColor Cyan
Write-Host " Wersja: $VersionName" -ForegroundColor Cyan
Write-Host " Bez GitHub CLI, bez pytan i bez recznego logowania" -ForegroundColor Cyan
Write-Host "==============================================" -ForegroundColor Cyan

$RequiredFiles = @(
    $BaseApk,
    $KeyStore,
    (Join-Path $Root "requirements.txt"),
    (Join-Path $Root "tools\patch_apk.py"),
    (Join-Path $Root "tools\sign_apk_v2.py"),
    (Join-Path $Root "tools\validate_apk.py"),
    (Join-Path $Root ".github\workflows\android-build.yml")
)

foreach ($File in $RequiredFiles) {
    if (!(Test-Path -LiteralPath $File)) {
        Stop-WithMessage "Brakuje pliku: $File"
    }
}

Info "[1/8] Sprawdzam Git i Python..."
$GitExe = Find-Executable "git" @(
    "$env:ProgramFiles\Git\cmd\git.exe",
    "$env:ProgramFiles\Git\bin\git.exe",
    "$env:LOCALAPPDATA\Programs\Git\cmd\git.exe",
    "E:\Programy\Git\cmd\git.exe"
)

if (!$GitExe) {
    Stop-WithMessage "Nie znaleziono Git. Skrypt korzysta z Git zainstalowanego w systemie."
}

$PythonCommand = Get-PythonCommand
Ok "Git: $GitExe"
Ok "Python: $($PythonCommand -join ' ')"

Info "[2/8] Instaluje wymagane biblioteki Python..."
Invoke-PythonChecked $PythonCommand @(
    "-m", "pip", "install", "-r", (Join-Path $Root "requirements.txt"),
    "--disable-pip-version-check", "--quiet"
) "Nie udalo sie zainstalowac bibliotek Python."

Info "[3/8] Buduje, podpisuje i sprawdza APK..."
New-Item -ItemType Directory -Path (Join-Path $Root "output") -Force | Out-Null
Remove-Item -LiteralPath $UnsignedApk, $FinalApk -Force -ErrorAction SilentlyContinue

Invoke-PythonChecked $PythonCommand @(
    (Join-Path $Root "tools\patch_apk.py"),
    "--input", $BaseApk,
    "--output", $UnsignedApk
) "Nie udalo sie zmodyfikowac APK."

Invoke-PythonChecked $PythonCommand @(
    (Join-Path $Root "tools\sign_apk_v2.py"),
    "--input", $UnsignedApk,
    "--output", $FinalApk,
    "--keystore", $KeyStore,
    "--password", $Password,
    "--alias", $Alias
) "Nie udalo sie podpisac APK."

Invoke-PythonChecked $PythonCommand @(
    (Join-Path $Root "tools\validate_apk.py"),
    $FinalApk
) "Walidacja APK nie powiodla sie."

Copy-Item -LiteralPath $FinalApk -Destination $RootApk -Force
Update-Sha256File
Ok "APK zostalo zbudowane i sprawdzone."

Info "[4/8] Tworze kompletna paczke ZIP..."
$TempPackage = Join-Path $env:TEMP ("Licznik_package_" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $TempPackage -Force | Out-Null

Get-ChildItem -LiteralPath $Root -Force | Where-Object {
    $_.Name -notin @(".git", ".tools", "output", $VersionFileName)
} | ForEach-Object {
    $Target = Join-Path $TempPackage $_.Name
    if ($_.PSIsContainer) {
        Copy-DirectoryContent $_.FullName $Target @(".git", ".tools", "output")
    }
    else {
        Copy-Item -LiteralPath $_.FullName -Destination $Target -Force
    }
}

Copy-Item -LiteralPath $RootApk -Destination (Join-Path $TempPackage $VersionFileName) -Force
Remove-Item -LiteralPath $Package -Force -ErrorAction SilentlyContinue
Compress-Archive -Path (Join-Path $TempPackage "*") -DestinationPath $Package -CompressionLevel Optimal
Remove-Item -LiteralPath $TempPackage -Recurse -Force
Ok "Paczka ZIP zostala utworzona."

Info "[5/8] Pobieram aktualne repozytorium..."
$TempRoot = Join-Path $env:TEMP ("Licznik_git_upload_" + [guid]::NewGuid().ToString("N"))
$RepoWorkPath = Join-Path $TempRoot "repo"
New-Item -ItemType Directory -Path $TempRoot -Force | Out-Null

Invoke-NativeChecked $GitExe @(
    "clone", "--branch", $BranchName, "--single-branch", $RepoUrl, $RepoWorkPath
) "Nie udalo sie pobrac repozytorium."

Info "[6/8] Kopiuje projekt i tworze commit..."
Copy-ProjectToRepository $RepoWorkPath
Set-Location $RepoWorkPath

Invoke-NativeChecked $GitExe @("config", "user.name", $GitUserName) "Nie udalo sie ustawic autora Git."
Invoke-NativeChecked $GitExe @("config", "user.email", $GitUserEmail) "Nie udalo sie ustawic e-maila autora Git."
Invoke-NativeChecked $GitExe @("config", "core.autocrlf", "false") "Nie udalo sie ustawic konfiguracji Git."
Invoke-NativeChecked $GitExe @("add", "-A") "Nie udalo sie dodac plikow do commita."

$Status = Invoke-NativeCapture $GitExe @("status", "--porcelain") "Nie udalo sie sprawdzic zmian Git."
if ([string]::IsNullOrWhiteSpace($Status)) {
    Warn "Brak zmian w plikach. Tworze pusty commit, aby ponownie uruchomic automatyczna publikacje."
    Invoke-NativeChecked $GitExe @("commit", "--allow-empty", "-m", $CommitMessage) "Nie udalo sie utworzyc pustego commita."
}
else {
    Invoke-NativeChecked $GitExe @("commit", "-m", $CommitMessage) "Nie udalo sie utworzyc commita."
}

$CommitSha = Invoke-NativeCapture $GitExe @("rev-parse", "HEAD") "Nie udalo sie odczytac identyfikatora commita."

Info "[7/8] Wysylam zmiany na GitHub..."
Invoke-NativeChecked $GitExe @("push", "origin", $BranchName) "Nie udalo sie wyslac projektu. Git powinien korzystac z danych zapisanych w Git Credential Manager."

Set-Location $Root
Remove-Item -LiteralPath $TempRoot -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item -LiteralPath $UnsignedApk -Force -ErrorAction SilentlyContinue
Ok "Projekt zostal wyslany na GitHub."

Wait-ForRelease $CommitSha

Write-Host ""
Write-Host "==============================================" -ForegroundColor Green
Write-Host " GOTOWE" -ForegroundColor Green
Write-Host " APK: $RootApk" -ForegroundColor Green
Write-Host " ZIP: $Package" -ForegroundColor Green
Write-Host " TAG: $Tag" -ForegroundColor Green
Write-Host "==============================================" -ForegroundColor Green
