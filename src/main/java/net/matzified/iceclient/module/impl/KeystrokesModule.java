package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class KeystrokesModule extends Module {

    public KeystrokesModule() {
        super("keystrokes", "Keystrokes", "Displays WASD, Space, and Mouse clicks with active animations", Category.PVP, true, 8, 100);
        setWidth(74);
        setHeight(76);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.options == null) return;
        TextRenderer font = mc.textRenderer;

        int x = getX();
        int y = getY();
        int keySize = 22;
        int gap = 2;

        boolean wPressed = mc.options.forwardKey.isPressed();
        boolean aPressed = mc.options.leftKey.isPressed();
        boolean sPressed = mc.options.backKey.isPressed();
        boolean dPressed = mc.options.rightKey.isPressed();
        boolean spacePressed = mc.options.jumpKey.isPressed();
        boolean lmbPressed = mc.options.attackKey.isPressed();
        boolean rmbPressed = mc.options.useKey.isPressed();

        // W key
        drawKey(context, font, "W", x + keySize + gap, y, keySize, keySize, wPressed);

        // A, S, D keys
        drawKey(context, font, "A", x, y + keySize + gap, keySize, keySize, aPressed);
        drawKey(context, font, "S", x + keySize + gap, y + keySize + gap, keySize, keySize, sPressed);
        drawKey(context, font, "D", x + (keySize + gap) * 2, y + keySize + gap, keySize, keySize, dPressed);

        // LMB, RMB
        int mouseW = (keySize * 3 + gap * 2 - gap) / 2;
        drawKey(context, font, "LMB", x, y + (keySize + gap) * 2, mouseW, 14, lmbPressed);
        drawKey(context, font, "RMB", x + mouseW + gap, y + (keySize + gap) * 2, mouseW, 14, rmbPressed);

        // Spacebar
        int totalW = keySize * 3 + gap * 2;
        drawKey(context, font, "——", x, y + (keySize + gap) * 2 + 16, totalW, 10, spacePressed);

        setWidth(totalW);
        setHeight((keySize + gap) * 2 + 28);
    }

    private void drawKey(DrawContext context, TextRenderer font, String name, int x, int y, int w, int h, boolean pressed) {
        int bg = pressed ? 0xDD38BDF8 : 0xAA0D1117;
        int textCol = pressed ? 0xFF0D1117 : 0xFFFFFFFF;

        context.fill(x, y, x + w, y + h, bg);
        context.fill(x, y, x + w, y + 1, 0x5538BDF8);
        context.fill(x, y + h - 1, x + w, y + h, 0x5538BDF8);
        context.fill(x, y, x + 1, y + h, 0x5538BDF8);
        context.fill(x + w - 1, y, x + w, y + h, 0x5538BDF8);

        int tw = font.getWidth(name);
        context.drawTextWithShadow(font, name, x + (w - tw) / 2, y + (h - 8) / 2, textCol);
    }
}
