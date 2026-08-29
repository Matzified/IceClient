package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class MemoryModule extends Module {

    public MemoryModule() {
        super("memory", "Memory / RAM Display", "Shows live JVM heap usage and max allocated RAM", Category.PERFORMANCE, true, 8, 340);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMb = (totalMemory - freeMemory) / (1024 * 1024);
        long maxMb = runtime.maxMemory() / (1024 * 1024);

        int pct = (int) ((usedMb / (float) maxMb) * 100);
        int col = pct < 60 ? 0xFF34D399 : (pct < 85 ? 0xFFFBBF24 : 0xFFF87171);

        String text = "💾 RAM: " + usedMb + "MB / " + maxMb + "MB (" + pct + "%)";
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, col);
    }
}
