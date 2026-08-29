package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class FullbrightModule extends Module {

    public FullbrightModule() {
        super("fullbright", "Fullbright / Gamma Boost", "Brightens dark caves and nighttime environments", Category.VISUAL, false, 8, 515);
    }

    @Override
    public void onTick() {
        if (mc.options != null && isEnabled()) {
            if (mc.options.getGamma().getValue() < 10.0) {
                mc.options.getGamma().setValue(12.0);
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.options != null) {
            mc.options.getGamma().setValue(1.0);
        }
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;

        String text = "💡 Fullbright: ON";
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFFFBBF24);
    }
}
