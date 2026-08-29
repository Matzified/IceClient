package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class ToggleSprintModule extends Module {

    public ToggleSprintModule() {
        super("toggle_sprint", "Toggle Sprint / Sneak", "Shows Sprinting and Sneaking status on the HUD", Category.UTILITY, true, 8, 490);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        boolean sprinting = mc.player.isSprinting();
        boolean sneaking = mc.player.isSneaking();
        if (!sprinting && !sneaking) return;

        String text = sprinting ? "[Sprinting (Key)]" : "[Sneaking (Key)]";
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, sprinting ? 0xFF34D399 : 0xFFFBBF24);
    }
}
