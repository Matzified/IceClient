package net.matzified.iceclient.gui;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.matzified.iceclient.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Collectors;

public class IceClientGuiScreen extends Screen {

    private final ModuleManager moduleManager = ModuleManager.getInstance();
    private Category currentCategory = Category.ALL;
    private String searchQuery = "";
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public IceClientGuiScreen() {
        super(Text.literal("Ice Client Menu"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark translucent frosted background
        context.fill(0, 0, width, height, 0xD00A0D14);

        int panelW = Math.min(840, width - 40);
        int panelH = Math.min(540, height - 40);
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;

        // Main Glass Panel Container
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE111622);
        // Outer Cyan Glow Border
        context.fill(panelX, panelY, panelX + panelW, panelY + 2, 0xFF38BDF8);
        context.fill(panelX, panelY + panelH - 2, panelX + panelW, panelY + panelH, 0x3338BDF8);
        context.fill(panelX, panelY, panelX + 2, panelY + panelH, 0x3338BDF8);
        context.fill(panelX + panelW - 2, panelY, panelX + panelW, panelY + panelH, 0x3338BDF8);

        // Header
        renderHeader(context, panelX, panelY, panelW, mouseX, mouseY);

        // Category Tabs Bar
        renderCategoryTabs(context, panelX, panelY + 48, panelW, mouseX, mouseY);

        // Modules Grid
        renderModulesGrid(context, panelX + 20, panelY + 95, panelW - 40, panelH - 110, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHeader(DrawContext context, int x, int y, int w, int mouseX, int mouseY) {
        // Title
        context.drawTextWithShadow(textRenderer, "🧊 ICE CLIENT", x + 20, y + 18, 0xFF38BDF8);
        context.drawTextWithShadow(textRenderer, "MOD MENU", x + 106, y + 18, 0xFF94A3B8);

        // Search Bar Box
        int searchW = 180;
        int searchX = x + w - searchW - 200;
        int searchY = y + 12;
        context.fill(searchX, searchY, searchX + searchW, searchY + 24, 0xFF182030);
        context.fill(searchX, searchY, searchX + searchW, searchY + 1, 0x5538BDF8);
        context.fill(searchX, searchY + 23, searchX + searchW, searchY + 24, 0x5538BDF8);
        String searchDisplay = searchQuery.isEmpty() ? "🔍 Search modules..." : "🔍 " + searchQuery;
        int searchColor = searchQuery.isEmpty() ? 0xFF64748B : 0xFFFFFFFF;
        context.drawTextWithShadow(textRenderer, searchDisplay, searchX + 8, searchY + 8, searchColor);

        // Edit HUD Layout Button
        int btnW = 110;
        int btnX = x + w - btnW - 75;
        int btnY = y + 12;
        boolean btnHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + 24;
        context.fill(btnX, btnY, btnX + btnW, btnY + 24, btnHover ? 0xFF0284C7 : 0xFF0369A1);
        context.drawTextWithShadow(textRenderer, "🎯 Edit HUD", btnX + 22, btnY + 8, 0xFFFFFFFF);

        // Close Button
        int closeX = x + w - 45;
        int closeY = y + 12;
        boolean closeHover = mouseX >= closeX && mouseX <= closeX + 26 && mouseY >= closeY && mouseY <= closeY + 24;
        context.fill(closeX, closeY, closeX + 26, closeY + 24, closeHover ? 0xFFDC2626 : 0xFF222B3D);
        context.drawTextWithShadow(textRenderer, "✕", closeX + 9, closeY + 8, 0xFFFFFFFF);

        // Header Separator Line
        context.fill(x, y + 46, x + w, y + 47, 0x2238BDF8);
    }

    private void renderCategoryTabs(DrawContext context, int x, int y, int w, int mouseX, int mouseY) {
        int tabX = x + 20;
        Category[] categories = Category.values();

        for (Category cat : categories) {
            String label = cat.icon + " " + cat.displayName;
            int tabW = textRenderer.getWidth(label) + 18;
            boolean isSelected = cat == currentCategory;
            boolean isHover = mouseX >= tabX && mouseX <= tabX + tabW && mouseY >= y + 4 && mouseY <= y + 32;

            if (isSelected) {
                context.fill(tabX, y + 6, tabX + tabW, y + 30, 0x3338BDF8);
                context.fill(tabX, y + 30, tabX + tabW, y + 32, 0xFF38BDF8);
                context.drawTextWithShadow(textRenderer, label, tabX + 9, y + 14, 0xFF38BDF8);
            } else {
                int col = isHover ? 0xFFFFFFFF : 0xFF94A3B8;
                if (isHover) context.fill(tabX, y + 6, tabX + tabW, y + 30, 0x15FFFFFF);
                context.drawTextWithShadow(textRenderer, label, tabX + 9, y + 14, col);
            }

            tabX += tabW + 8;
        }

        // Tabs separator line
        context.fill(x, y + 38, x + w, y + 39, 0x2238BDF8);
    }

    private void renderModulesGrid(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY) {
        List<Module> list = moduleManager.getModulesByCategory(currentCategory).stream()
                .filter(m -> searchQuery.isEmpty()
                        || m.getName().toLowerCase().contains(searchQuery.toLowerCase())
                        || m.getDescription().toLowerCase().contains(searchQuery.toLowerCase()))
                .collect(Collectors.toList());

        int cols = 2;
        int gap = 14;
        int cardW = (w - (gap * (cols - 1))) / cols;
        int cardH = 68;

        int totalRows = (int) Math.ceil(list.size() / (double) cols);
        int totalContentH = totalRows * (cardH + gap);
        maxScroll = Math.max(0, totalContentH - h);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        // Enable Scissor for smooth scrolling area
        context.enableScissor(x, y, x + w, y + h);

        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int row = i / cols;
            int col = i % cols;

            int cx = x + col * (cardW + gap);
            int cy = y + row * (cardH + gap) - scrollOffset;

            if (cy + cardH < y || cy > y + h) continue;

            boolean isHover = mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH;

            // Card Background
            int bgCol = isHover ? 0xFF1E2638 : 0xFF141926;
            context.fill(cx, cy, cx + cardW, cy + cardH, bgCol);

            // Card Border
            int borderCol = m.isEnabled() ? 0x9938BDF8 : (isHover ? 0x4438BDF8 : 0x2238BDF8);
            context.fill(cx, cy, cx + cardW, cy + 1, borderCol);
            context.fill(cx, cy + cardH - 1, cx + cardW, cy + cardH, borderCol);
            context.fill(cx, cy, cx + 1, cy + cardH, borderCol);
            context.fill(cx + cardW - 1, cy, cx + cardW, cy + cardH, borderCol);

            // Left status indicator bar
            if (m.isEnabled()) {
                context.fill(cx, cy, cx + 3, cy + cardH, 0xFF38BDF8);
            }

            // Title & Category Badge
            context.drawTextWithShadow(textRenderer, m.getName(), cx + 12, cy + 12, 0xFFFFFFFF);

            // Category Pill
            String catName = m.getCategory().displayName;
            int catW = textRenderer.getWidth(catName) + 8;
            int catX = cx + cardW - catW - 74;
            context.fill(catX, cy + 10, catX + catW, cy + 22, 0xFF1E293B);
            context.drawTextWithShadow(textRenderer, catName, catX + 4, cy + 12, 0xFF94A3B8);

            // Description
            String desc = m.getDescription();
            if (textRenderer.getWidth(desc) > cardW - 84) {
                desc = textRenderer.trimToWidth(desc, cardW - 96) + "...";
            }
            context.drawTextWithShadow(textRenderer, desc, cx + 12, cy + 30, 0xFF94A3B8);

            // Settings Gear Button
            int gearW = 24;
            int gearH = 22;
            int gearX = cx + cardW - 84;
            int gearY = cy + (cardH - gearH) / 2;
            boolean gearHover = mouseX >= gearX && mouseX <= gearX + gearW && mouseY >= gearY && mouseY <= gearY + gearH;
            context.fill(gearX, gearY, gearX + gearW, gearY + gearH, gearHover ? 0xFF0284C7 : 0xFF1E293B);
            context.drawTextWithShadow(textRenderer, "⚙️", gearX + 6, gearY + 7, 0xFFFFFFFF);

            // Toggle Switch Button
            int switchW = 46;
            int switchH = 22;
            int switchX = cx + cardW - switchW - 10;
            int switchY = cy + (cardH - switchH) / 2;

            if (m.isEnabled()) {
                context.fill(switchX, switchY, switchX + switchW, switchY + switchH, 0xFF0284C7);
                context.fill(switchX + switchW - 18, switchY + 2, switchX + switchW - 2, switchY + switchH - 2, 0xFFFFFFFF);
                context.drawTextWithShadow(textRenderer, "ON", switchX + 5, switchY + 7, 0xFFFFFFFF);
            } else {
                context.fill(switchX, switchY, switchX + switchW, switchY + switchH, 0xFF334155);
                context.fill(switchX + 2, switchY + 2, switchX + 18, switchY + switchH - 2, 0xFF94A3B8);
                context.drawTextWithShadow(textRenderer, "OFF", switchX + 22, switchY + 7, 0xFF94A3B8);
            }
        }

        context.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int panelW = Math.min(840, width - 40);
            int panelH = Math.min(540, height - 40);
            int panelX = (width - panelW) / 2;
            int panelY = (height - panelH) / 2;

            // Close button clicked
            int closeX = panelX + panelW - 45;
            int closeY = panelY + 12;
            if (mouseX >= closeX && mouseX <= closeX + 26 && mouseY >= closeY && mouseY <= closeY + 24) {
                close();
                return true;
            }

            // Edit HUD Button clicked
            int btnW = 110;
            int btnX = panelX + panelW - btnW - 75;
            int btnY = panelY + 12;
            if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + 24) {
                if (client != null) {
                    client.setScreen(new HudPositionerScreen(this));
                }
                return true;
            }

            // Category Tab Clicked
            int tabX = panelX + 20;
            int tabY = panelY + 48;
            for (Category cat : Category.values()) {
                String label = cat.icon + " " + cat.displayName;
                int tabW = textRenderer.getWidth(label) + 18;
                if (mouseX >= tabX && mouseX <= tabX + tabW && mouseY >= tabY + 4 && mouseY <= tabY + 32) {
                    currentCategory = cat;
                    scrollOffset = 0;
                    return true;
                }
                tabX += tabW + 8;
            }

            // Check Module Card Click
            int gridX = panelX + 20;
            int gridY = panelY + 95;
            int gridW = panelW - 40;
            int gridH = panelH - 110;

            if (mouseX >= gridX && mouseX <= gridX + gridW && mouseY >= gridY && mouseY <= gridY + gridH) {
                List<Module> list = moduleManager.getModulesByCategory(currentCategory).stream()
                        .filter(m -> searchQuery.isEmpty()
                                || m.getName().toLowerCase().contains(searchQuery.toLowerCase())
                                || m.getDescription().toLowerCase().contains(searchQuery.toLowerCase()))
                        .collect(Collectors.toList());

                int cols = 2;
                int gap = 14;
                int cardW = (gridW - (gap * (cols - 1))) / cols;
                int cardH = 68;

                for (int i = 0; i < list.size(); i++) {
                    int row = i / cols;
                    int col = i % cols;
                    int cx = gridX + col * (cardW + gap);
                    int cy = gridY + row * (cardH + gap) - scrollOffset;

                    if (mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH) {
                        Module m = list.get(i);

                        // Check if Gear / Settings button clicked
                        int gearW = 24;
                        int gearH = 22;
                        int gearX = cx + cardW - 84;
                        int gearY = cy + (cardH - gearH) / 2;
                        if (mouseX >= gearX && mouseX <= gearX + gearW && mouseY >= gearY && mouseY <= gearY + gearH) {
                            if (client != null) {
                                client.setScreen(new ModuleSettingsScreen(this, m));
                            }
                            return true;
                        }

                        // Otherwise toggle the module
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

        // Simple search query input handling
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
