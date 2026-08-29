# 🧊 Ice Client - Developer & Agent Guidelines

Welcome to the **Ice Client** codebase. This repository contains the complete ecosystem for Ice Client:
1. **Fabric 1.21.1 Client Mod** (`src/main/java/net/matzified/iceclient`)
2. **Standalone Custom Launcher** (`launcher/src/main/java/net/matzified/iceclient/launcher`)

---

## 🛠️ Architecture Overview

### 1. In-Game Mod Core (`src/main/java/net/matzified/iceclient`)
- **`IceClient.java`**: Mod entrypoint, registers Right Shift keybinding and tick events.
- **`module/Module.java`**: Base class for all 32+ HUD/PvP modules.
- **`module/ModuleManager.java`**: Registry and JSON configuration persistence (`config/iceclient/modules.json`).
- **`gui/IceClientGuiScreen.java`**: Right Shift ClickGUI with search, category tabs, and module cards.
- **`gui/ModuleSettingsScreen.java`**: Individual per-module settings (colors, scale, glass backgrounds, shadows).
- **`gui/HudPositionerScreen.java`**: Drag-and-drop HUD layout editor.
- **`mixin/`**: Safe, non-invasive Mixins:
  - `GameMenuScreenMixin.java`: Injects `🧊 Ice Client Mods` button into ESC pause menu.
  - `TitleScreenMixin.java`: Injects `🧊 Ice Client` button on the main title screen.
  - `KeyboardMixin.java`: Instant GLFW Right Shift keyboard interceptor.
  - `MinecraftClientMixin.java`: Hooks attacks for CPS, Reach, and Combo tracking.

### 2. Standalone Launcher (`launcher/`)
- **`MinecraftLaunchEngine.java`**: Multi-threaded downloader for Mojang assets, libraries, natives, Fabric Loader, and automated 1000 FPS JVM argument engine.
- **`ProfileManager.java`**: Manages Minecraft versions (1.21 - 1.21.11, 1.20, etc.) and memory presets.
- **`ui/`**: Glassmorphic dark UI panels with responsive animations and live launch overlays.

---

## ⚡ Build & Deployment Runbook

### Build Command (Root Project)
```powershell
& "C:\Users\mathe\.gradle\wrapper\dists\gradle-8.9-bin\90cnw93cvbtalezasaz0blq0a\gradle-8.9\bin\gradle.bat" :compileJava :classes :jar :remapJar :launcher:compileJava :launcher:classes :launcher:jar
```

### Auto-Deploy Mod to Active Profile
```powershell
Copy-Item -Path ".\build\libs\IceClient-1.0.0.jar" -Destination "$env:APPDATA\.iceclient\profiles\1.21.1-Default\mods\IceClient-1.0.0.jar" -Force
```

---

## 🛡️ Coding & Anti-Cheat Rules
- **No UTF-8 BOM Headers**: Always write Java source files as plain UTF-8 without BOM to prevent Windows `javac` error `illegal character: '\ufeff'`.
- **Fair-Play Only**: Never add packet manipulation, automation (auto-clicker, killaura), fly, speed, or xray. Keep all modules 100% client-side and Hypixel-compliant.
- **Universal Mixin Targets**: Prefer injecting into lifecycle methods like `Screen.init()` rather than private version-specific methods.
