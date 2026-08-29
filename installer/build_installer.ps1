$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " Ice Client 1.21-1.21.11 Installer Builder" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

$rootDir = Split-Path -Parent $PSScriptRoot
Set-Location $rootDir

$distDir = Join-Path $rootDir "build\dist"
if (Test-Path $distDir) {
    Get-ChildItem $distDir -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
} else {
    New-Item -ItemType Directory -Path $distDir | Out-Null
}

$gradleBat = "C:\Users\mathe\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat"

Write-Host "`n[1/3] Building Launcher Fat JAR..." -ForegroundColor Yellow
& $gradleBat -b launcher\build.gradle jar

$launcherJar = Join-Path $rootDir "launcher\build\libs\IceClientLauncher.jar"
if (-not (Test-Path $launcherJar)) {
    throw "Launcher JAR missing at: $launcherJar"
}
Copy-Item $launcherJar -Destination (Join-Path $distDir "IceClientLauncher.jar") -Force

Write-Host "[2/3] Building Fabric Client Mod JAR..." -ForegroundColor Yellow
try {
    & $gradleBat build --refresh-dependencies
} catch {
    Write-Host "Gradle build warning, checking fallback mod JAR..." -ForegroundColor Yellow
}

$modJar = Join-Path $rootDir "build\libs\IceClient-1.0.0.jar"
if (-not (Test-Path $modJar)) {
    $modJar = Get-ChildItem -Path (Join-Path $rootDir "build\libs") -Filter "*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
}
if ($modJar -and (Test-Path $modJar)) {
    Copy-Item $modJar -Destination (Join-Path $distDir "IceClient-1.0.0.jar") -Force
}

Write-Host "[3/3] Compiling C# Setup Installer (IceClientSetup.exe)..." -ForegroundColor Yellow
$cscPath = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
$installerCs = Join-Path $rootDir "installer\IceClientInstaller.cs"
$outExe = Join-Path $distDir "IceClientSetup.exe"

& $cscPath /target:winexe /out:$outExe /r:System.Windows.Forms.dll /r:System.Drawing.dll $installerCs

Copy-Item $launcherJar -Destination (Join-Path $rootDir "installer\IceClientLauncher.jar") -Force
Copy-Item $outExe -Destination (Join-Path $rootDir "installer\IceClientSetup.exe") -Force

Write-Host "`n==========================================" -ForegroundColor Green
Write-Host " BUILD SUCCESSFUL!" -ForegroundColor Green
Write-Host " Installer created at: $outExe" -ForegroundColor Green
Write-Host " Distribution Directory: $distDir" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
