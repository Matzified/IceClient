---
name: create-module
description: Procedure for creating and registering new HUD and PvP modules with custom settings.
---

# Create Module Superpower

Follow these steps when creating a new module for Ice Client:

## 1. Create Module Implementation Class
Create `src/main/java/net/matzified/iceclient/module/impl/<Name>Module.java`:
- Inherit from `net.matzified.iceclient.module.Module`.
- Call `super(id, name, description, Category, defaultEnabled, defaultX, defaultY)`.
- Override `render(DrawContext context, RenderTickCounter tickCounter)`.
- Use `drawGlassBox(context, getX(), getY(), w, h)` and `context.drawTextWithShadow(...)`.
- Add any module-specific `Setting<?>` (e.g. `BooleanSetting`, `ColorSetting`, `ModeSetting`, `NumberSetting`) using `addSetting(...)`.

## 2. Register Module in `ModuleManager.java`
In `net.matzified.iceclient.module.ModuleManager.registerAllModules()`:
```java
register(new <Name>Module());
```

## 3. Compile and Hot-Deploy
Run the build-deploy workflow to package and test the module in-game.
