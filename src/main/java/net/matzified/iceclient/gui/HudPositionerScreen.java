package net.matzified.iceclient.gui;

import net.matzified.iceclient.module.Module;
import net.matzified.iceclient.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class HudPositionerScreen extends Screen {

    private final Screen parent;
    private final ModuleManager moduleManager = ModuleManager.getInstance();
    private Module draggingModule = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public HudPositionerScreen(Screen parent) {
        super(Text.literal("HUD Positioner"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Semi-transparent backdrop with subtle grid
        context.fill(0, 0, width, height, 0x880A0D14);

        // Header instructions
        int bannerW = 340;
        int bannerH = 34;
        int bannerX = (width - bannerW) / 2;
        context.fill(bannerX, 10, bannerX + bannerW, 10 + bannerH, 0xEE111622);
        context.fill(bannerX, 10, bannerX + bannerW, 12, 0xFF38BDF8);
        context.drawTextWithShadow(textRenderer, "🎯 DRAG HUD MODULES TO REPOSITION", bannerX + 24, 18, 0xFF38BDF8);
        context.drawTextWithShadow(textRenderer, "Press [ESC] or [ENTER] when done", bannerX + 45, 30, 0xFF94A3B8);

        // Render each enabled module with a cyan draggable box outline
        for (Module m : moduleManager.getModules()) {
            if (!m.isEnabled()) continue;

            int mx = m.getX();
            int my = m.getY();
            int mw = m.getWidth();
            int mh = m.getHeight();

            boolean isHover = mouseX >= mx && mouseX <= mx + mw && mouseY >= my && mouseY <= my + mh;
            boolean isDragging = draggingModule == m;

            // Bounding box highlight
            int borderCol = isDragging ? 0xFF34D399 : (isHover ? 0xFF38BDF8 : 0x8838BDF8);
            context.fill(mx, my, mx + mw, my + 1, borderCol);
            context.fill(mx, my + mh - 1, mx + mw, my + mh, borderCol);
            context.fill(mx, my, mx + 1, my + mh, borderCol);
            context.fill(mx + mw - 1, my, mx + mw, my + mh, borderCol);

            // Translucent fill
            context.fill(mx + 1, my + 1, mx + mw - 1, my + mh - 1, isDragging ? 0x6634D399 : (isHover ? 0x4438BDF8 : 0x2238BDF8));

            // Module Label
            context.drawTextWithShadow(textRenderer, m.getName(), mx + 4, my + 4, 0xFFFFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Module m : moduleManager.getModules()) {
                if (!m.isEnabled()) continue;
                int mx = m.getX();
                int my = m.getY();
                int mw = m.getWidth();
                int mh = m.getHeight();

                if (mouseX >= mx && mouseX <= mx + mw && mouseY >= my && mouseY <= my + mh) {
                    draggingModule = m;
                    dragOffsetX = (int) (mouseX - mx);
                    dragOffsetY = (int) (mouseY - my);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingModule != null) {
            int newX = (int) (mouseX - dragOffsetX);
            int newY = (int) (mouseY - dragOffsetY);
            newX = Math.max(0, Math.min(newX, width - draggingModule.getWidth()));
            newY = Math.max(0, Math.min(newY, height - draggingModule.getHeight()));
            draggingModule.setX(newX);
            draggingModule.setY(newY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingModule != null) {
            draggingModule = null;
            moduleManager.saveConfig();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            moduleManager.saveConfig();
            if (client != null) {
                client.setScreen(parent);
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
