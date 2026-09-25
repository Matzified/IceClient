# ICECLIENT: MASTER AI STATE HANDOFF DOCUMENT

**ATTENTION NEW AI AGENT**: If you are reading this, the user has switched computers to their main PC to continue development on IceClient. You are inheriting a massive, highly successful project. Read this document carefully to synchronize your context.

## 🔴 CRITICAL WARNING: LEGACY CODE 🔴
**DO NOT USE THE EXISTING CODE IN THIS REPOSITORY.** 
The user has explicitly stated that everything currently inside this repository (the old launcher, Java files, gradle configs, etc.) is **OLD AND OUTDATED**. 
You are to **IGNORE, WIPE, OR OVERWRITE** the existing repository contents. We are building IceClient V3 entirely from scratch.

## 1. Project Overview & Aesthetic
**Name:** IceClient
**Type:** Premium, High-Performance Minecraft PvP Client
**Aesthetic:** Ultra-clean, minimalist, dark-theme, subtle cyan/frost accents (#00e5ff), soft glassmorphism, no generic layouts. The user absolutely HATES chaotic neon or cluttered designs. Everything must be sleek and highly professional.

## 2. Current Progress (What is Finished)
- **Phase 1 (UI/UX Blueprinting):** Finished. 
- **Phase 2 (Web Platform):** 100% FINISHED (Located in a separate `IceClient-Website` repository).
  - Built a custom React/Vite frontend.
  - The website is completely locked. Do not edit website code.

## 3. Immediate Next Steps (Your Mission)
The user is ready to begin **Phase 4: The C++ Desktop Launcher**.

**Action Required:**
1. Wipe the old legacy folders in this repository if they interfere with the new C++ project.
2. Initialize a brand new C++ project environment using **ImGui** (Dear ImGui) for the user interface.
3. The Launcher needs to mirror the exact aesthetic of the website (Dark theme, `#0a0a0a` backgrounds, cyan `#00e5ff` accents, rounded corners, minimalist layout).

## 4. Subagent Protocol & Orchestration
You must utilize the Google Antigravity SDK tools (`define_subagent` and `invoke_subagent`) to orchestrate this build. Do not write the C++ code manually in the main thread.

**Step 1: Define the C++ Agent**
You must define a highly specialized C++ Developer Agent using the `define_subagent` tool:
- **Name:** `cpp_developer`
- **System Prompt:** "You are an elite C++ systems engineer specializing in Windows/Mac OS native software and Dear ImGui. Your task is to build a high-performance desktop launcher for IceClient. You must write extremely clean, optimized C++ code, manage CMake build systems, and implement a dark-theme, cyan-accented UI."
- **Tools:** Equip it with `enable_write_tools=true` and `enable_mcp_tools=true`.

**Step 2: Invoke the Agent**
Once defined, use `invoke_subagent` to launch `cpp_developer` and instruct it to:
1. Delete the legacy `launcher` folder in this repo.
2. Setup CMake and Dear ImGui.
3. Build the first iteration of the launcher window.

## 5. Instructions for the New AI
Acknowledge that you have read this handoff document by greeting the user as "Mathew". Confirm that you understand the old code is TRASH. Instantly invoke the C++ Developer Subagent to begin destroying the old code and setting up the new ImGui environment.
