# ICECLIENT: AI STATE HANDOFF DOCUMENT

**ATTENTION NEW AI AGENT**: If you are reading this, the user has switched computers to their main PC to continue development on IceClient. You are inheriting a massive, highly successful project. Read this document carefully to synchronize your context.

## 1. Project Overview
**Name:** IceClient
**Type:** Premium, High-Performance Minecraft PvP Client
**Aesthetic:** Ultra-clean, minimalist, dark-theme, subtle cyan/frost accents (#00e5ff), soft glassmorphism, no generic layouts. 

## 2. Current Progress (What is Finished)
- **Phase 1 (UI/UX Blueprinting):** Completely finished. We established a strict design system. 
- **Phase 2 (Web Platform):** 100% FINISHED.
  - Built a custom React/Vite frontend.
  - Implemented interactive FPS Calculator.
  - Implemented click-to-expand feature cards with hover physics.
  - Handled Microsoft Auth (MSAL) mocking bypass for Minecraft authentication.
  - All custom pixel-art logos and favicons are perfectly integrated.
  - **The website is locked. Do not edit the website code.**

## 3. Immediate Next Steps (Your Mission)
The user is ready to begin **Phase 4: The C++ Desktop Launcher**.

**Action Required:**
1. You must immediately create a new directory for the launcher software outside of the website folder (e.g., `../IceClient-Launcher`).
2. You must initialize a C++ project environment using **ImGui** (Dear ImGui) for the user interface.
3. The Launcher needs to mirror the exact aesthetic of the website (Dark theme, `#0a0a0a` backgrounds, cyan `#00e5ff` accents, rounded corners, minimalist layout).
4. **Subagent Handoff:** You should spin up a `C++ Developer Agent` (using your subagent tools) dedicated entirely to writing the ImGui C++ code, linking the OpenGL/DirectX backend, and compiling the executable. 

## 4. Subagent Protocol
The previous session utilized highly specialized subagents to isolate tasks and maintain pristine code quality:
- `Website Designer Agent`: (Retired) Built the React frontend.
- **`C++ Developer Agent`**: (PENDING) You must create/invoke this agent to handle all native ImGui code and OS-level operations for the Launcher. 
- **`Java Core Agent`**: (PENDING) After the launcher is done, you will invoke this agent to build the Fabric 1.21+ PvP modules (Keystrokes, CPS, FPS Boost, etc.).

## 5. Instructions for the New AI
Acknowledge that you have read this handoff document by greeting the user as "Mathew" and instantly asking for permission to initialize the C++ ImGui environment for the Desktop Launcher. Do not waste time re-evaluating the website. **Start writing C++ immediately.**
