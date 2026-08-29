package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.world.LightType;

public class LightLevelModule extends Module {

    public LightLevelModule() {
        super("light_level", "Light Level Display", "Displays block and sky light levels at feet", Category.HUD, false, 8, 565);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.world == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        int blockLight = mc.world.getLightLevel(LightType.BLOCK, mc.player.getBlockPos());
        int skyLight = mc.world.getLightLevel(LightType.SKY, mc.player.getBlockPos());
        int totalLight = Math.max(blockLight, skyLight);

        int col = totalLight <= 0 ? 0xFFF87171 : (totalLight < 7 ? 0xFFFBBF24 : 0xFF34D399);
        String text = "💡 Light: " + totalLight + " (B: " + blockLight + ", S: " + skyLight + ")";

        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, col);
    }
}
