# Ice Client Fast Build & Deploy Script
$ErrorActionPreference = "Stop"

Write-Host "🧊 [Ice Client] Compiling Core Mod & Launcher..." -ForegroundColor Cyan

$gradle = "C:\Users\mathe\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat"
& $gradle :compileJava :classes :jar :remapJar :launcher:compileJava :launcher:classes :launcher:jar

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ [Ice Client] Build Successful!" -ForegroundColor Green
    
    $profilesDir = "$env:APPDATA\.iceclient\profiles"
    if (Test-Path $profilesDir) {
        Get-ChildItem -Path $profilesDir -Directory | ForEach-Object {
            $modsDir = Join-Path $_.FullName "mods"
            if (!(Test-Path $modsDir)) { New-Item -ItemType Directory -Path $modsDir -Force | Out-Null }
            Copy-Item -Path ".\build\libs\IceClient-1.0.0.jar" -Destination (Join-Path $modsDir "IceClient-1.0.0.jar") -Force
            Write-Host "🚀 Hot-deployed mod to: $($_.Name)" -ForegroundColor Magenta
        }
    }
} else {
    Write-Host "❌ [Ice Client] Build Failed!" -ForegroundColor Red
}
