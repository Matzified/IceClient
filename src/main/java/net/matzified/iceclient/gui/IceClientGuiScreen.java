package net.matzified.iceclient.gui;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.matzified.iceclient.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IceClientGuiScreen extends Screen {

    private final ModuleManager moduleManager = ModuleManager.getInstance();
    private Category currentCategory = Category.ALL;
    private String searchQuery = "";
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // Smooth open animation easing
    private final long openTime = System.currentTimeMillis();

    // Per-module toggle switch animation interpolation (0.0f -> 1.0f)
    private final Map<String, Float> switchAnimMap = new HashMap<>();

    public IceClientGuiScreen() {
        super(Text.literal("Ice Client Menu"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private float getEaseProgress() {
        long elapsed = System.currentTimeMillis() - openTime;
        float t = Math.min(1.0f, elapsed / 200.0f);
        return 1.0f - (float) Math.pow(1.0f - t, 3); // cubic easeOut
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float ease = getEaseProgress();

        // 1. Frosted Backdrop
        int bgAlpha = (int) (0xDA * ease);
        context.fill(0, 0, width, height, (bgAlpha << 24) | 0x07090E);

        int panelW = Math.min(920, width - 40);
        int panelH = Math.min(560, height - 40);

        int animatedH = (int) (panelH * (0.94f + 0.06f * ease));
        int panelX = (width - panelW) / 2;
        int panelY = (height - animatedH) / 2;

        // 2. Main Window Container (Sleek Glassmorphic Dark Panel)
        context.fill(panelX, panelY, panelX + panelW, panelY + animatedH, 0xF00D111A);
        
        // Neon Ice Cyan Accent Border
        context.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF38BDF8);
        context.fill(panelX, panelY + animatedH - 1, panelX + panelW, panelY + animatedH, 0x2238BDF8);
        context.fill(panelX, panelY, panelX + 1, panelY + animatedH, 0x2238BDF8);
        context.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + animatedH, 0x2238BDF8);

        // Sidebar Width
        int sidebarW = 180;

        // 3. Render Sidebar (Brand & Category Navigation)
        renderSidebar(context, panelX, panelY, sidebarW, animatedH, mouseX, mouseY);

        // 4. Render Main Content Header
        int contentX = panelX + sidebarW;
        int contentW = panelW - sidebarW;
        renderContentHeader(context, contentX, panelY, contentW, mouseX, mouseY);

        // 5. Render Module Grid
        int gridY = panelY + 54;
        int gridH = animatedH - 64;
        renderModulesGrid(context, contentX + 16, gridY, contentW - 32, gridH, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderSidebar(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY) {
        // Sidebar Background
        context.fill(x, y, x + w, y + h, 0xF50A0D14);
        context.fill(x + w - 1, y, x + w, y + h, 0x1A38BDF8);

        // Ice Client Logo & Branding
        context.drawTextWithShadow(textRenderer, "🧊 ICE CLIENT", x + 16, y + 16, 0xFF38BDF8);
        context.drawTextWithShadow(textRenderer, "1000 FPS COMPETITIVE", x + 16, y + 28, 0xFF64748B);

        context.fill(x + 12, y + 42, x + w - 12, y + 43, 0x1A38BDF8);

        // Category Navigation List
        int catY = y + 52;
        Category[] categories = Category.values();

        for (Category cat : categories) {
            boolean isSelected = (cat == currentCategory);
            boolean isHover = mouseX >= x + 8 && mouseX <= x + w - 8 && mouseY >= catY && mouseY <= catY + 30;

            int count = (int) moduleManager.getModulesByCategory(cat).size();

            if (isSelected) {
                // Active Pill
                context.fill(x + 8, catY, x + w - 8, catY + 30, 0x2638BDF8);
                context.fill(x + 8, catY + 4, x + 11, catY + 26, 0xFF38BDF8);
                context.drawTextWithShadow(textRenderer, cat.icon + "  " + cat.displayName, x + 18, catY + 11, 0xFF38BDF8);
            } else {
                if (isHover) {
                    context.fill(x + 8, catY, x + w - 8, catY + 30, 0x10FFFFFF);
                }
                int col = isHover ? 0xFFFFFFFF : 0xFF94A3B8;
                context.drawTextWithShadow(textRenderer, cat.icon + "  " + cat.displayName, x + 18, catY + 11, col);
            }

            // Count Badge
            String countStr = String.valueOf(count);
            int badgeW = textRenderer.getWidth(countStr) + 8;
            int badgeX = x + w - badgeW - 14;
            context.fill(badgeX, catY + 8, badgeX + badgeW, catY + 22, isSelected ? 0x4438BDF8 : 0x1A334155);
            context.drawTextWithShadow(textRenderer, countStr, badgeX + 4, catY + 11, isSelected ? 0xFF38BDF8 : 0xFF64748B);

            catY += 34;
        }

        // Footer Version Tag
        context.drawTextWithShadow(textRenderer, "v1.0.0 • Fabric 1.21.1", x + 16, y + h - 18, 0xFF475569);
    }

    private void renderContentHeader(DrawContext context, int x, int y, int w, int mouseX, int mouseY) {
        // Active Category Title
        String catTitle = currentCategory.displayName.toUpperCase() + " MODULES";
        context.drawTextWithShadow(textRenderer, catTitle, x + 18, y + 18, 0xFFF1F5F9);

        // Search Bar Box
        int searchW = 190;
        int searchX = x + w - searchW - 170;
        int searchY = y + 12;

        context.fill(searchX, searchY, searchX + searchW, searchY + 26, 0xFF141A26);
        context.fill(searchX, searchY, searchX + searchW, searchY + 1, 0x3338BDF8);
        context.fill(searchX, searchY + 25, searchX + searchW, searchY + 26, 0x3338BDF8);

        String searchDisplay = searchQuery.isEmpty() ? "🔍 Search modules..." : "🔍 " + searchQuery;
        int searchColor = searchQuery.isEmpty() ? 0xFF64748B : 0xFFFFFFFF;
        context.drawTextWithShadow(textRenderer, searchDisplay, searchX + 8, searchY + 9, searchColor);

        // Edit HUD Layout Button
        int btnW = 100;
        int btnX = x + w - btnW - 55;
        int btnY = y + 12;
        boolean btnHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + 26;
        context.fill(btnX, btnY, btnX + btnW, btnY + 26, btnHover ? 0xFF0284C7 : 0xFF0369A1);
        context.drawTextWithShadow(textRenderer, "🎯 Edit HUD", btnX + 16, btnY + 9, 0xFFFFFFFF);

        // Close Button
        int closeX = x + w - 38;
        int closeY = y + 12;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 26 && mouseY >= closeY && mouseY <= closeY + 26;
        context.fill(closeX, closeY, closeX + 26, closeY + 26, closeHover ? 0xFFDC2626 : 0xFF1E293B);
        context.drawTextWithShadow(textRenderer, "✕", closeX + 9, closeY + 9, 0xFFFFFFFF);

        // Header Separator Line
        context.fill(x, y + 46, x + w, y + 47, 0x1A38BDF8);
    }

    private void renderModulesGrid(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY) {
        List<Module> list = moduleManager.getModulesByCategory(currentCategory).stream()
                .filter(m -> searchQuery.isEmpty()
                        || m.getName().toLowerCase().contains(searchQuery.toLowerCase())
                        || m.getDescription().toLowerCase().contains(searchQuery.toLowerCase()))
                .collect(Collectors.toList());

        int cols = 2;
        int gap = 12;
        int cardW = (w - (gap * (cols - 1))) / cols;
        int cardH = 64;

        int totalRows = (int) Math.ceil(list.size() / (double) cols);
        int totalContentH = totalRows * (cardH + gap);
        maxScroll = Math.max(0, totalContentH - h);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        context.enableScissor(x, y, x + w, y + h);

        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int row = i / cols;
            int col = i % cols;

            int cx = x + col * (cardW + gap);
            int cy = y + row * (cardH + gap) - scrollOffset;

            if (cy + cardH < y || cy > y + h) continue;

            boolean isHover = mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH;

            // Card Container
            int bgCol = isHover ? 0xFF161D2B : 0xFF111722;
            context.fill(cx, cy, cx + cardW, cy + cardH, bgCol);

            // Card Border
            int borderCol = m.isEnabled() ? 0x6638BDF8 : (isHover ? 0x3338BDF8 : 0x1538BDF8);
            context.fill(cx, cy, cx + cardW, cy + 1, borderCol);
            context.fill(cx, cy + cardH - 1, cx + cardW, cy + cardH, borderCol);
            context.fill(cx, cy, cx + 1, cy + cardH, borderCol);
            context.fill(cx + cardW - 1, cy, cx + cardW, cy + cardH, borderCol);

            // Left Enabled Indicator Bar
            if (m.isEnabled()) {
                context.fill(cx, cy, cx + 3, cy + cardH, 0xFF38BDF8);
            }

            // Title
            context.drawTextWithShadow(textRenderer, m.getName(), cx + 12, cy + 11, m.isEnabled() ? 0xFFFFFFFF : 0xFFCBD5E1);

            // Description
            String desc = m.getDescription();
            if (textRenderer.getWidth(desc) > cardW - 105) {
                desc = textRenderer.trimToWidth(desc, cardW - 115) + "...";
            }
            context.drawTextWithShadow(textRenderer, desc, cx + 12, cy + 26, 0xFF64748B);

            // Settings Button (⚙️)
            int gearW = 24;
            int gearH = 22;
            int gearX = cx + cardW - 82;
            int gearY = cy + (cardH - gearH) / 2;
            boolean gearHover = mouseX >= gearX && mouseX <= gearX + gearW && mouseY >= gearY && mouseY <= gearY + gearH;
            context.fill(gearX, gearY, gearX + gearW, gearY + gearH, gearHover ? 0xFF0284C7 : 0xFF1E293B);
            context.drawTextWithShadow(textRenderer, "⚙️", gearX + 6, gearY + 7, 0xFFFFFFFF);

            // Animated iOS Toggle Switch
            int switchW = 44;
            int switchH = 22;
            int switchX = cx + cardW - switchW - 10;
            int switchY = cy + (cardH - switchH) / 2;

            float targetVal = m.isEnabled() ? 1.0f : 0.0f;
            float currentVal = switchAnimMap.getOrDefault(m.getId(), targetVal);
            currentVal += (targetVal - currentVal) * 0.25f;
            switchAnimMap.put(m.getId(), currentVal);

            int trackCol = m.isEnabled() ? 0xFF0284C7 : 0xFF242E42;
            context.fill(switchX, switchY, switchX + switchW, switchY + switchH, trackCol);

            int knobW = 16;
            int knobX = switchX + 2 + (int) (currentVal * (switchW - knobW - 4));
            context.fill(knobX, switchY + 2, knobX + knobW, switchY + switchH - 2, 0xFFFFFFFF);

            if (currentVal > 0.6f) {
                context.drawTextWithShadow(textRenderer, "ON", switchX + 5, switchY + 7, 0xFFFFFFFF);
            } else if (currentVal < 0.4f) {
                context.drawTextWithShadow(textRenderer, "OFF", switchX + 22, switchY + 7, 0xFF94A3B8);
            }
        }

        context.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelW = Math.min(920, width - 40);
            int panelH = Math.min(560, height - 40);
            int panelX = (width - panelW) / 2;
            int panelY = (height - panelH) / 2;
            int sidebarW = 180;
            int contentX = panelX + sidebarW;
            int contentW = panelW - sidebarW;

            // Close button
            int closeX = contentX + contentW - 38;
            int closeY = panelY + 12;
            if (mouseX >= closeX && mouseX <= closeX + 26 && mouseY >= closeY && mouseY <= closeY + 26) {
                close();
                return true;
            }

            // Edit HUD Button
            int btnW = 100;
            int btnX = contentX + contentW - btnW - 55;
            int btnY = panelY + 12;
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + 26) {
                if (client != null) {
                    client.setScreen(new HudPositionerScreen(this));
                }
                return true;
            }

            // Sidebar Category Clicks
            int catY = panelY + 52;
            for (Category cat : Category.values()) {
                if (mouseX >= panelX + 8 && mouseX <= panelX + sidebarW - 8 && mouseY >= catY && mouseY <= catY + 30) {
                    currentCategory = cat;
                    scrollOffset = 0;
                    return true;
                }
                catY += 34;
            }

            // Grid Module Clicks
            int gridX = contentX + 16;
            int gridY = panelY + 54;
            int gridW = contentW - 32;
            int gridH = panelH - 64;

            if (mouseX >= gridX && mouseX <= gridX + gridW && mouseY >= gridY && mouseY <= gridY + gridH) {
                List<Module> list = moduleManager.getModulesByCategory(currentCategory).stream()
                        .filter(m -> searchQuery.isEmpty()
                                || m.getName().toLowerCase().contains(searchQuery.toLowerCase())
                                || m.getDescription().toLowerCase().contains(searchQuery.toLowerCase()))
                        .collect(Collectors.toList());

                int cols = 2;
                int gap = 12;
                int cardW = (gridW - (gap * (cols - 1))) / cols;
                int cardH = 64;

                for (int i = 0; i < list.size(); i++) {
                    int row = i / cols;
                    int col = i % cols;
                    int cx = gridX + col * (cardW + gap);
                    int cy = gridY + row * (cardH + gap) - scrollOffset;

                    if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH) {
                        Module m = list.get(i);

                        // Settings Gear Click
                        int gearW = 24;
                        int gearH = 22;
                        int gearX = cx + cardW - 82;
                        int gearY = cy + (cardH - gearH) / 2;
                        if (mouseX >= gearX && mouseX <= gearX + gearW && mouseY >= gearY && mouseY <= gearY + gearH) {
                            if (client != null) {
                                client.setScreen(new ModuleSettingsScreen(this, m));
                            }
                            return true;
                        }

                        // Toggle Module
                        m.toggle();
                        moduleManager.saveConfig();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= (int) (verticalAmount * 24);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                scrollOffset = 0;
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr >= 32 && chr != 127) {
            searchQuery += chr;
            scrollOffset = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        moduleManager.saveConfig();
        super.close();
    }
}
