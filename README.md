# 🧊 Ice Client - Minecraft 1.21 - 1.21.11 Fabric Client & Installer

**Ice Client** is a smooth, lightweight, high-performance Fabric client mod and launcher designed for **Minecraft 1.21 through 1.21.11**. It delivers massive FPS boosts through custom entity frustum culling, particle throttling, smooth memory management, and a clean launcher interface with dedicated storage.

---

## ✨ Features

- 🚀 **Isolated Storage System (`.iceclient`)**: All client profiles, configs, saves, and mods are stored cleanly in `%APPDATA%\.iceclient` rather than default `.minecraft`.
- 💻 **Windows Executable Setup (`IceClientSetup.exe`)**: Standalone `.exe` installer that automatically sets up the `.iceclient` folder structure, extracts launcher binaries, creates Desktop and Start Menu shortcuts, and provisions default profile templates.
- ⚡ **Ultra-Smooth Out-Of-The-Box Performance**:
  - **Entity Frustum & Occlusion Culling**: Culls hidden entities outside the camera view field.
  - **Particle Throttler**: Smooths frame rates during explosions, potion effects, and heavy PvP.
  - **Glassmorphic FPS & RAM HUD**: Real-time FPS, RAM monitor, and XYZ coordinate HUD.
  - **Optimized G1GC Flags**: Low garbage-collection pause times for stutter-free gameplay.
- 🎮 **Profile & Memory Manager**: Create, edit, and switch profiles targeting Minecraft versions **1.21, 1.21.1, 1.21.2, 1.21.3, 1.21.4, and 1.21.11** with custom RAM allocation (2 GB to 16 GB).
- 🧩 **Modrinth 1-Click Mod Store & Local Mod Manager**: Browse, search, and install Fabric mods directly into active `.iceclient` profiles, with 1-click enable/disable and File Explorer integration.

---

## 🛠️ Building & Running

### Build Installer (.exe) & Launcher
Run the PowerShell build script to generate `build/dist/IceClientSetup.exe`:

```powershell
powershell -ExecutionPolicy Bypass -File .\installer\build_installer.ps1
```

### Run Installer
Launch `build/dist/IceClientSetup.exe` or `installer/IceClientSetup.exe` to install Ice Client directly into `%APPDATA%\.iceclient`.

---

## 📜 License
MIT License - Created by [Matzified](https://github.com/Matzified/IceClient).
