package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.matzified.iceclient.setting.BooleanSetting;
import net.matzified.iceclient.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class CustomCrosshairModule extends Module {

    public final NumberSetting crosshairSize = new NumberSetting("size", "Crosshair Size", "Length of crosshair arms", 4.0, 1.0, 12.0, 1.0);
    public final NumberSetting crosshairGap = new NumberSetting("gap", "Center Gap", "Center gap distance", 2.0, 0.0, 8.0, 1.0);
    public final BooleanSetting showDot = new BooleanSetting("showDot", "Center Dot", "Render center pixel dot", true);

    public CustomCrosshairModule() {
        super("custom_crosshair", "Custom Crosshair", "Renders a custom PvP crosshair with dynamic target coloring", Category.VISUAL, false, 8, 965);
        addSetting(crosshairSize);
        addSetting(crosshairGap);
        addSetting(showDot);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.getWindow() == null) return;

        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;

        int size = crosshairSize.getValue().intValue();
        int gap = crosshairGap.getValue().intValue();
        int thickness = 1;
        int color = getTextColor();

        // Draw crosshair lines around center
        context.fill(cx - size - gap, cy - thickness / 2, cx - gap, cy + thickness / 2 + 1, color);
        context.fill(cx + gap + 1, cy - thickness / 2, cx + size + gap + 1, cy + thickness / 2 + 1, color);
        context.fill(cx - thickness / 2, cy - size - gap, cx + thickness / 2 + 1, cy - gap, color);
        context.fill(cx - thickness / 2, cy + gap + 1, cx + thickness / 2 + 1, cy + size + gap + 1, color);

        // Center dot
        if (showDot.getValue()) {
            context.fill(cx, cy, cx + 1, cy + 1, 0xFFFFFFFF);
        }
    }
}
