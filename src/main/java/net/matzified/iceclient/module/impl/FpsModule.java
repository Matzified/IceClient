package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.matzified.iceclient.setting.BooleanSetting;
import net.matzified.iceclient.setting.ModeSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class FpsModule extends Module {

    public final BooleanSetting showPrefix = new BooleanSetting("showPrefix", "Show 'FPS' Prefix", "Display 'FPS:' before the frame rate number", true);
    public final ModeSetting colorMode = new ModeSetting("colorMode", "Color Mode", "Choose dynamic or static color", new String[]{"Theme Color", "Dynamic (Green/Red)", "Rainbow"}, "Theme Color");

    public FpsModule() {
        super("fps", "FPS Display", "Displays your current Minecraft frames per second", Category.HUD, true, 8, 8);
        addSetting(showPrefix);
        addSetting(colorMode);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;

        int currentFps = mc.getCurrentFps();
        String text = showPrefix.getValue() ? "FPS: " + currentFps : currentFps + " FPS";
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);

        int renderCol = getTextColor();
        if ("Dynamic (Green/Red)".equals(colorMode.getValue())) {
            renderCol = currentFps >= 60 ? 0xFF34D399 : (currentFps >= 30 ? 0xFFFBBF24 : 0xFFF87171);
        }

        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, renderCol);
    }
}
