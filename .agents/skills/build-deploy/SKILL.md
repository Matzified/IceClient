---
name: build-deploy
description: Fast build and deploy workflow for Ice Client mod and launcher.
---

# Build & Deploy Superpower

Use this workflow to compile the client mod, standalone launcher, and hot-deploy the mod to the local active profile.

## Commands

### 1. Compile Mod & Launcher Jars
```powershell
& "C:\Users\mathe\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat" :compileJava :classes :jar :remapJar :launcher:compileJava :launcher:classes :launcher:jar
```

### 2. Hot-Deploy Mod to Profiles
```powershell
Get-ChildItem -Path "$env:APPDATA\.iceclient\profiles" -Directory | ForEach-Object {
    $modsDir = Join-Path $_.FullName "mods"
    if (!(Test-Path $modsDir)) { New-Item -ItemType Directory -Path $modsDir -Force }
    Copy-Item -Path ".\build\libs\IceClient-1.0.0.jar" -Destination (Join-Path $modsDir "IceClient-1.0.0.jar") -Force
}
```

### 3. Launch Launcher
```powershell
& "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.1-hotspot\bin\java.exe" -jar ".\launcher\build\libs\IceClientLauncher.jar"
```
