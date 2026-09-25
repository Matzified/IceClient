# ICECLIENT: MASTER ANTIGRAVITY HANDOFF DOCUMENT

**ATTENTION ANTIGRAVITY (NEW INSTANCE)**: The user has switched computers to their main PC. You are inheriting this exact session. Read this document to rebuild our subagent ecosystem and resume work immediately.

## 🔴 CRITICAL WARNING: LEGACY CODE 🔴
**DO NOT USE THE EXISTING CODE IN THIS REPOSITORY.** 
Everything currently inside this repository (the old launcher, Java files, gradle configs) is **OLD AND OUTDATED TRASH**. 
You are to **IGNORE, WIPE, OR OVERWRITE** the existing repository contents. We are building IceClient V3 entirely from scratch.

---

## 1. Project Overview & Aesthetic
**Name:** IceClient (Premium, High-Performance Minecraft PvP Client)
**Aesthetic:** Ultra-clean, minimalist, dark-theme, subtle cyan/frost accents (`#00e5ff`), soft glassmorphism. No chaotic neon.

## 2. Current Progress (Where We Are At)
- **Phase 1 (UI/UX Blueprinting):** Finished. 
- **Phase 2 (Web Platform):** 100% FINISHED (Located in a separate `IceClient-Website` repository). The website is locked.
- **Phase 3 (Java Core):** Pending.
- **Phase 4 (C++ Launcher):** READY TO START IMMEDIATELY.

---

## 3. REBUILDING THE SUBAGENT ECOSYSTEM
To continue this project, you must immediately rebuild our exact subagent ecosystem using the `define_subagent` tool. Here are the exact blueprints for the agents you must create to handle different parts of the project:

### Agent A: `cpp_launcher_dev` (Invoke this first!)
- **Name:** `cpp_launcher_dev`
- **System Prompt:** "You are an elite C++ systems engineer. Your sole responsibility is building the native Windows/Mac OS Desktop Launcher for IceClient using CMake and Dear ImGui. Write clean, highly optimized C++ code that implements a dark-theme, cyan-accented UI. Do not ask for permission, just write the code."
- **Tools:** `enable_write_tools=true`, `enable_mcp_tools=true`

### Agent B: `java_fabric_dev` (Invoke later)
- **Name:** `java_fabric_dev`
- **System Prompt:** "You are an elite Minecraft Java Developer specializing in the Fabric API for version 1.21+. Your sole responsibility is building the in-game PvP modules for IceClient (Keystrokes, CPS counter, FPS Boost, Zoom). You manage gradle setups, mixins, and rendering code."
- **Tools:** `enable_write_tools=true`

### Agent C: `ui_ux_designer` (Optional support)
- **Name:** `ui_ux_designer`
- **System Prompt:** "You are a specialized UI/UX designer. Your role is to generate mockups and design systems for the C++ ImGui interface, ensuring it perfectly matches the minimalist, glassmorphism aesthetic of the website."
- **Tools:** `enable_write_tools=false`

---

## 4. Immediate Next Steps (Your Mission)
1. **Acknowledge Handoff:** Greet the user as "Mathew" and confirm you have absorbed the state.
2. **Rebuild Ecosystem:** Use the `define_subagent` tool to create `cpp_launcher_dev` and `java_fabric_dev` right now in the background.
3. **Execute Phase 4:** Use `invoke_subagent` to dispatch `cpp_launcher_dev` with instructions to wipe the old legacy `launcher` folder in this repo, set up a new CMake project, and build the first iteration of the ImGui window.
