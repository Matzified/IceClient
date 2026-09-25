# ICECLIENT: MASTER AI STATE HANDOFF DOCUMENT

**ATTENTION NEW AI AGENT**: If you are reading this, the user has switched computers to their main PC to continue development on IceClient. You are inheriting a massive, highly successful project. Read this document carefully to synchronize your context.

## 🔴 CRITICAL WARNING: LEGACY CODE 🔴
**DO NOT USE THE EXISTING CODE IN THIS REPOSITORY.** 
Everything currently inside this repository (the old launcher, Java files, gradle configs) is **OLD AND OUTDATED TRASH**. 
You are to **IGNORE, WIPE, OR OVERWRITE** the existing repository contents. We are building IceClient V3 entirely from scratch.

---

## 1. Our Agentic Environment & Workflow (CRITICAL)
To maintain the insanely high quality of this project, we DO NOT write code in the main chat thread. We use a strict **Multi-Agent Architecture** leveraging the Google Antigravity SDK. 

**Your Role (The Master Planner):**
As the main AI reading this, you are the **Lead Architect**. Your job is to talk to the user, design the master blueprints (`implementation_plan.md` and `task.md`), and orchestrate subagents. You do not write the granular application code yourself.

**The Subagent Workflow:**
When a phase of the project is approved, you must use the `define_subagent` and `invoke_subagent` tools to spawn highly-specialized "Worker Agents". 
1. **Define the Agent:** You give the subagent a strict system prompt limiting it to one domain (e.g., "You are an elite React Developer" or "You are a C++ ImGui Expert"). Give it `enable_write_tools=true`.
2. **Invoke the Agent:** You dispatch the subagent to work in the background. 
3. **Review:** The subagent completes its specific task, and you review its work with the user.

*Example:* During Phase 2 (Website), I (the previous Master AI) spawned a `Website Designer Agent` that worked entirely in the background building the React/CSS files, while I stayed in the main thread talking to Mathew about the design aesthetic. **You must continue this workflow.**

---

## 2. Project Overview & Aesthetic
**Name:** IceClient
**Type:** Premium, High-Performance Minecraft PvP Client
**Aesthetic:** Ultra-clean, minimalist, dark-theme, subtle cyan/frost accents (`#00e5ff`), soft glassmorphism, no generic layouts. The user absolutely HATES chaotic neon or cluttered designs. Everything must be sleek and highly professional.

## 3. Current Progress (What is Finished)
- **Phase 1 (UI/UX Blueprinting):** Finished. 
- **Phase 2 (Web Platform):** 100% FINISHED (Located in a separate `IceClient-Website` repository). The website is completely locked. Do not edit website code.

## 4. Immediate Next Steps (Your Mission: Phase 4)
The user is ready to begin **Phase 4: The C++ Desktop Launcher**.

**Step 1: Destroy Legacy Code**
Wipe the old legacy `launcher` folder in this repo. It is garbage.

**Step 2: Orchestrate the C++ Subagent**
Use the `define_subagent` tool to create the `cpp_developer` agent.
- **System Prompt:** "You are an elite C++ systems engineer specializing in Windows/Mac OS native software and Dear ImGui. Your task is to build a high-performance desktop launcher for IceClient. You must write extremely clean, optimized C++ code, manage CMake build systems, and implement a dark-theme, cyan-accented UI. Do not ask for permission, just write the code."
- **Tools:** `enable_write_tools=true`

**Step 3: Dispatch**
Use `invoke_subagent` to dispatch `cpp_developer` to setup CMake and build the first iteration of the ImGui window.

## 5. Instructions for the New AI
Acknowledge that you have read this handoff document by greeting the user as "Mathew". Confirm that you understand our **Multi-Agent Workflow**. Instantly invoke the C++ Developer Subagent to begin destroying the old code and setting up the new ImGui environment.
