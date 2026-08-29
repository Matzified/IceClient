package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.matzified.iceclient.setting.BooleanSetting;
import net.matzified.iceclient.setting.ModeSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class CoordsModule extends Module {

    public final ModeSetting format = new ModeSetting("format", "Coordinates Format", "Choose layout format", new String[]{"XYZ Single Line", "XYZ Separated", "Compact"}, "XYZ Single Line");
    public final BooleanSetting showBiome = new BooleanSetting("showBiome", "Show Biome Name", "Append current biome", false);

    public CoordsModule() {
        super("coords", "Coordinates", "Displays your live XYZ coordinates in the world", Category.HUD, true, 8, 56);
        addSetting(format);
        addSetting(showBiome);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        int x = (int) Math.floor(mc.player.getX());
        int y = (int) Math.floor(mc.player.getY());
        int z = (int) Math.floor(mc.player.getZ());

        String text;
        if ("Compact".equals(format.getValue())) {
            text = x + ", " + y + ", " + z;
        } else if ("XYZ Separated".equals(format.getValue())) {
            text = "X: " + x + "  Y: " + y + "  Z: " + z;
        } else {
            text = "XYZ: " + x + " " + y + " " + z;
        }

        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, getTextColor());
    }
}
