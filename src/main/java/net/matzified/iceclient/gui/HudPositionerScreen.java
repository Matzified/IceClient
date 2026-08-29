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
    private boolean snapToGrid = true;

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
        // Frosted dark background
        context.fill(0, 0, width, height, 0x880A0D14);

        // Center Alignment Snap Crosshairs (subtle cyan dashed guide)
        int cx = width / 2;
        int cy = height / 2;
        context.fill(cx, 0, cx + 1, height, 0x2238BDF8);
        context.fill(0, cy, width, cy + 1, 0x2238BDF8);

        // Top Header Banner
        int bannerW = 380;
        int bannerH = 38;
        int bannerX = (width - bannerW) / 2;
        context.fill(bannerX, 10, bannerX + bannerW, 10 + bannerH, 0xF00D111A);
        context.fill(bannerX, 10, bannerX + bannerW, 12, 0xFF38BDF8);
        context.drawTextWithShadow(textRenderer, "🎯 DRAG & SNAP HUD POSITIONER", bannerX + 45, 18, 0xFF38BDF8);
        context.drawTextWithShadow(textRenderer, "Press [ESC] or [ENTER] to save layout", bannerX + 50, 32, 0xFF94A3B8);

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
            context.fill(mx + 1, my + 1, mx + mw - 1, my + mh - 1, isDragging ? 0x5534D399 : (isHover ? 0x3338BDF8 : 0x1838BDF8));

            // Module Label
            context.drawTextWithShadow(textRenderer, m.getName(), mx + 4, my + 4, 0xFFFFFFFF);

            // Live Coordinates readout pill when dragging
            if (isDragging) {
                String coordText = "X: " + mx + "  Y: " + my;
                int cw = textRenderer.getWidth(coordText) + 8;
                int pillX = mx;
                int pillY = Math.max(0, my - 16);
                context.fill(pillX, pillY, pillX + cw, pillY + 14, 0xEE111827);
                context.drawTextWithShadow(textRenderer, coordText, pillX + 4, pillY + 3, 0xFF38BDF8);
            }
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

            // Snap to screen center
            int cx = width / 2;
            int mw = draggingModule.getWidth();
            if (Math.abs(newX + (mw / 2) - cx) < 8) {
                newX = cx - (mw / 2);
            }

            // Snap to screen edges
            if (newX < 12) newX = 8;
            if (newX > width - mw - 12) newX = width - mw - 8;
            if (newY < 12) newY = 8;
            if (newY > height - draggingModule.getHeight() - 12) newY = height - draggingModule.getHeight() - 8;

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
