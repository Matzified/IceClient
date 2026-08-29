package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class ReachModule extends Module {

    private static double lastReach = 0.0;
    private static long lastHitTime = 0;

    public ReachModule() {
        super("reach", "Reach Display", "Shows attack distance on your last target", Category.PVP, true, 8, 390);
    }

    public static void setReach(double reach) {
        lastReach = reach;
        lastHitTime = System.currentTimeMillis();
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;

        long now = System.currentTimeMillis();
        String text = (now - lastHitTime < 5000 && lastReach > 0.0)
                ? String.format("🎯 Reach: %.2fm", lastReach)
                : "🎯 Reach: 0.00m";

        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFF38BDF8);
    }
}
