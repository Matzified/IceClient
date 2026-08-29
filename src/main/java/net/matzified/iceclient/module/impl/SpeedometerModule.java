package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.matzified.iceclient.setting.ModeSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class SpeedometerModule extends Module {

    private double prevX = 0;
    private double prevZ = 0;
    private double currentSpeed = 0;

    public final ModeSetting unit = new ModeSetting("unit", "Speed Unit", "Unit of measurement", new String[]{"m/s (bps)", "km/h", "mph"}, "m/s (bps)");

    public SpeedometerModule() {
        super("speedometer", "Speedometer", "Displays your horizontal movement speed in real-time", Category.HUD, false, 8, 176);
        addSetting(unit);
    }

    @Override
    public void onTick() {
        if (mc.player != null) {
            double dx = mc.player.getX() - prevX;
            double dz = mc.player.getZ() - prevZ;
            currentSpeed = Math.sqrt(dx * dx + dz * dz) * 20.0;
            prevX = mc.player.getX();
            prevZ = mc.player.getZ();
        }
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;

        double displaySpeed = currentSpeed;
        String unitStr = "m/s";
        if ("km/h".equals(unit.getValue())) {
            displaySpeed = currentSpeed * 3.6;
            unitStr = "km/h";
        } else if ("mph".equals(unit.getValue())) {
            displaySpeed = currentSpeed * 2.237;
            unitStr = "mph";
        }

        String text = String.format("Speed: %.2f %s", displaySpeed, unitStr);
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, getTextColor());
    }
}
