package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class FpsModule extends Module {

    public FpsModule() {
        super("fps", "FPS Display", "Shows live frame rate with dynamic color scaling", Category.HUD, true, 8, 8);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;
        int fps = mc.getCurrentFps();

        String label = "FPS: ";
        String value = String.valueOf(fps);

        int color = fps >= 200 ? 0xFF34D399 : (fps >= 60 ? 0xFF38BDF8 : (fps >= 30 ? 0xFFFBBF24 : 0xFFF87171));
        String fullText = "⚡ " + label + value;
        int padding = 6;
        int w = font.getWidth(fullText) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, "⚡ ", getX() + padding, getY() + 5, 0xFF38BDF8);
        context.drawTextWithShadow(font, label, getX() + padding + font.getWidth("⚡ "), getY() + 5, 0xFFE2E8F0);
        context.drawTextWithShadow(font, value, getX() + padding + font.getWidth("⚡ " + label), getY() + 5, color);
    }
}
