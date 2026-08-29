# 🤖 Agent Task Board & Work Backlog

This task board contains concrete, partitioned tasks designed for autonomous agents to pick up, implement, build, and deliver via Pull Requests.

---

## 📋 Task Backlog

| Task ID | Component | Title | Priority | Status | Target Files |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ICE-001** | `HUD` | Add Freelook / 360 Perspective Camera Module | High | 🟡 Ready | `src/main/java/net/matzified/iceclient/module/impl/FreelookModule.java` |
| **ICE-002** | `Cosmetics` | Animated Custom Cape Renderer & Texture Cache | High | 🟡 Ready | `src/main/java/net/matzified/iceclient/cosmetic/CapeRenderer.java` |
| **ICE-003** | `QoL` | Custom Keybind Hotkey Binding for Individual Modules | Medium | 🟡 Ready | `src/main/java/net/matzified/iceclient/gui/KeybindSettingWidget.java` |
| **ICE-004** | `Launcher` | Auto-Updater Check & Background Binary Patch Engine | Medium | 🟡 Ready | `launcher/src/main/java/net/matzified/iceclient/launcher/utils/AutoUpdater.java` |
| **ICE-005** | `Launcher` | Modpack Drag & Drop Import (.mrpack / .zip) | Medium | 🟡 Ready | `launcher/src/main/java/net/matzified/iceclient/launcher/utils/ModpackImporter.java` |
| **ICE-006** | `Performance` | FPS Benchmark Recorder & 1% Low Metric Overlay | Low | 🟡 Ready | `src/main/java/net/matzified/iceclient/module/impl/BenchmarkModule.java` |

---

## 📝 Agent Execution Protocol
1. Create a git feature branch: `git checkout -b feat/<task-id>-<short-description>`.
2. Implement code adhering strictly to `docs/TECHNICAL_SPECIFICATIONS.md`.
3. Compile and test build using `./scripts/build.ps1` or Gradle.
4. Hot-deploy to active profiles and verify zero crashes.
5. Push branch and open Pull Request: `gh pr create --title "[Task ID] Description" --body "..."`.
