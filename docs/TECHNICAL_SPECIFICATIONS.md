# 🧊 Ice Client - Technical Specifications & Architecture

## 1. System Architecture

```
                               ┌─────────────────────────────┐
                               │     Ice Client Launcher     │
                               │  (Java Swing + FlatLaf +    │
                               │   Multi-threaded Engine)    │
                               └──────────────┬──────────────┘
                                              │ Launches Process
                                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Minecraft 1.21.1 + Fabric Loader                      │
│                                                                             │
│  ┌─────────────────────────┐  ┌──────────────────────┐  ┌────────────────┐ │
│  │   Ice Client Mod Core   │  │   Sodium + Iris +    │  │  Fabric API +  │ │
│  │   (32+ Modular Suite)   │  │  FerriteCore Engine  │  │  EntityCulling │ │
│  └───────────┬─────────────┘  └──────────────────────┘  └────────────────┘ │
│              │                                                              │
│              ├─► HUD Engine (HudRenderCallback)                             │
│              ├─► ClickGUI Engine (IceClientGuiScreen + ModuleSettingsScreen)│
│              ├─► Settings System (Setting<T>, Color, Mode, Number, Boolean) │
│              └─► Mixin Interceptors (Keyboard, Pause Menu, Title, Attacks)  │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. In-Game Mod Core (`net.matzified.iceclient`)

### 2.1 Module API Contract
All modules extend `net.matzified.iceclient.module.Module`:
- **`getId()`**: Unique lowercase identifier (e.g. `fps`, `cps`, `mace_hud`).
- **`getName()`**: Display name.
- **`getCategory()`**: One of `Category.HUD`, `Category.PVP`, `Category.PERFORMANCE`, `Category.UTILITY`, `Category.VISUAL`.
- **`render(DrawContext context, RenderTickCounter tickCounter)`**: Renders HUD content.
- **`getSettings()`**: List of `Setting<?>` associated with this module.

### 2.2 Mixin Specifications
- **`KeyboardMixin`**: Intercepts `GLFW_KEY_RIGHT_SHIFT` at `Keyboard.onKey()` to immediately toggle `IceClientGuiScreen`.
- **`GameMenuScreenMixin`**: Injects `🧊 Ice Client Mods` button into `Screen.init()` on the ESC pause menu.
- **`TitleScreenMixin`**: Injects `🧊 Ice Client` button into `Screen.init()` on the main title screen.
- **`MinecraftClientMixin`**: Injects into `doAttack` and `doItemUse` for CPS registration, Reach distance calculation, and Combo streak tracking.

---

## 3. Configuration & Schema

### `config/iceclient/modules.json` Schema
```json
{
  "fps": {
    "enabled": true,
    "x": 8,
    "y": 8,
    "settings": {
      "textColor": -13058824,
      "glassBackground": true,
      "textShadow": true,
      "borderStyle": "Cyan Glow",
      "scale": 1.0
    }
  }
}
```

---

## 4. Launcher Engine Specifications (`net.matzified.iceclient.launcher`)

### 4.1 1000 FPS JVM Preset Matrix
- **`⚡ 1000 FPS Ultra (Aikar G1GC Mega-Throughput)`**:
  `-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=30 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=35 -XX:G1MaxNewSizePercent=45 -XX:G1ReservePercent=15 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Diceclient.ultrafps=true`
- **`🚀 1000 FPS Zero-Lag (Java 21 Generational ZGC)`**:
  `-XX:+UseZGC -XX:+ZGenerational -XX:+AlwaysPreTouch -XX:+DisableExplicitGC -XX:+UnlockExperimentalVMOptions -Diceclient.ultrafps=true`

### 4.2 Auto-Download Engine
- 32-worker concurrent pool for Mojang assets.
- 16-worker concurrent pool for libraries and native binaries.
- Auto-harvests existing `.minecraft/assets` cache to eliminate duplicate downloads.
