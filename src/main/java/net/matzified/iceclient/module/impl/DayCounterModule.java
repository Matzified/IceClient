package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class DayCounterModule extends Module {

    public DayCounterModule() {
        super("day_counter", "Day Counter", "Displays the in-game Minecraft world day number", Category.HUD, false, 8, 590);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.world == null) return;
        TextRenderer font = mc.textRenderer;

        long day = mc.world.getTimeOfDay() / 24000L;
        String text = "📅 Day: " + day;

        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFF38BDF8);
    }
}
