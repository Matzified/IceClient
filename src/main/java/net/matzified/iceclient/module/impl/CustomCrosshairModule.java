package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class CustomCrosshairModule extends Module {

    public CustomCrosshairModule() {
        super("custom_crosshair", "Custom Crosshair", "Renders a custom PvP crosshair with dynamic target coloring", Category.VISUAL, false, 8, 965);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.getWindow() == null) return;

        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;

        int size = 4;
        int gap = 2;
        int thickness = 1;
        int color = 0xFF38BDF8; // Ice Cyan Crosshair

        // Draw crosshair lines around center
        context.fill(cx - size - gap, cy - thickness / 2, cx - gap, cy + thickness / 2 + 1, color);
        context.fill(cx + gap + 1, cy - thickness / 2, cx + size + gap + 1, cy + thickness / 2 + 1, color);
        context.fill(cx - thickness / 2, cy - size - gap, cx + thickness / 2 + 1, cy - gap, color);
        context.fill(cx - thickness / 2, cy + gap + 1, cx + thickness / 2 + 1, cy + size + gap + 1, color);

        // Center dot
        context.fill(cx, cy, cx + 1, cy + 1, 0xFFFFFFFF);
    }
}
