<#
.SYNOPSIS
    Installs the semsea CLI on Windows.

.DESCRIPTION
    Copies the bundled launcher + JAR under %LOCALAPPDATA%\Programs\semsea\bin
    and adds that directory to the current user's PATH. The application's
    own data (config, database, logs) is created lazily under %APPDATA%\semsea
    on the first `semsea` invocation.

.PARAMETER InstallDir
    Override the install directory. Default: %LOCALAPPDATA%\Programs\semsea.

.EXAMPLE
    .\install.ps1
    .\install.ps1 -InstallDir 'D:\Tools\semsea'
#>
#Requires -Version 5.0
[CmdletBinding()]
param(
    [string] $InstallDir = (Join-Path $env:LOCALAPPDATA 'Programs\semsea')
)

$ErrorActionPreference = 'Stop'

function Write-Section($msg) { Write-Host ""; Write-Host "==> $msg" -ForegroundColor Cyan }

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sourceBin = Join-Path $scriptDir 'bin'

if (-not (Test-Path $sourceBin)) {
    Write-Error "Could not find 'bin/' next to this script. Did you extract the full archive?"
    exit 1
}

# 1. Java check (java prints '-version' to stderr; mute hard-fail on that)
Write-Section "Checking Java"
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Error "Java is not on PATH. Install JDK 21 or newer and retry."
    exit 1
}
$prevPref = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    $javaVersionLine = (& java -version 2>&1 | Out-String).Trim() -split "`r?`n" | Select-Object -First 1
    Write-Host "    $javaVersionLine"
}
finally {
    $ErrorActionPreference = $prevPref
}

# 2. Copy files
Write-Section "Installing to $InstallDir"
$targetBin = Join-Path $InstallDir 'bin'
New-Item -ItemType Directory -Force -Path $targetBin | Out-Null
Copy-Item -Force -Recurse -Path (Join-Path $sourceBin '*') -Destination $targetBin

# 3. PATH update (user scope, so no admin needed)
Write-Section "Updating PATH"
$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
$pathEntries = if ($userPath) { $userPath -split ';' } else { @() }
if ($pathEntries -notcontains $targetBin) {
    $newPath = if ($userPath) { "$userPath;$targetBin" } else { $targetBin }
    [Environment]::SetEnvironmentVariable('Path', $newPath, 'User')
    Write-Host "    Added $targetBin to user PATH."
    Write-Host "    Open a NEW shell for the change to take effect."
}
else {
    Write-Host "    $targetBin is already on user PATH."
}

# 4. Done
Write-Section "Done"
Write-Host "    semsea installed to $InstallDir"
Write-Host "    Data directory     $env:APPDATA\semsea (created on first run)"
Write-Host ""
Write-Host "Open a new shell and try:" -ForegroundColor Green
Write-Host "    semsea --help" -ForegroundColor Green
Write-Host ""
Write-Host "Before using 'semsea attach', bring up Ollama + ChromaDB."
Write-Host "See SERVICES.md (next to this script) for step-by-step setup,"
Write-Host "then verify with 'semsea heartbeat'."
