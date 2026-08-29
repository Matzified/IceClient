package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class SpeedometerModule extends Module {

    private double prevX = 0;
    private double prevZ = 0;
    private double currentBps = 0.0;
    private long lastTime = 0;

    public SpeedometerModule() {
        super("speedometer", "Speedometer (BPS)", "Displays movement speed in Blocks Per Second", Category.HUD, false, 8, 365);
    }

    @Override
    public void onTick() {
        if (mc.player != null) {
            long now = System.currentTimeMillis();
            if (lastTime > 0) {
                double dt = (now - lastTime) / 1000.0;
                if (dt > 0) {
                    double dx = mc.player.getX() - prevX;
                    double dz = mc.player.getZ() - prevZ;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    currentBps = dist / dt;
                }
            }
            prevX = mc.player.getX();
            prevZ = mc.player.getZ();
            lastTime = now;
        }
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;

        String text = String.format("🏃 Speed: %.1f BPS", currentBps);
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFF38BDF8);
    }
}
