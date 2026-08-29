package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class ComboModule extends Module {

    private static int comboCount = 0;
    private static long lastHitTime = 0;

    public ComboModule() {
        super("combo", "Combo Counter", "Displays consecutive attack hit streak", Category.PVP, true, 8, 415);
    }

    public static void registerHit() {
        long now = System.currentTimeMillis();
        if (now - lastHitTime < 2500) {
            comboCount++;
        } else {
            comboCount = 1;
        }
        lastHitTime = now;
    }

    public static void resetCombo() {
        comboCount = 0;
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;

        long now = System.currentTimeMillis();
        if (now - lastHitTime > 2500) {
            comboCount = 0;
        }

        String text = "💥 Combo: " + comboCount + (comboCount == 1 ? " Hit" : " Hits");
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, comboCount > 3 ? 0xFFFBBF24 : 0xFF38BDF8);
    }
}
