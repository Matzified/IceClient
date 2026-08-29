package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClockModule extends Module {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    public ClockModule() {
        super("clock", "Game & Real Clock", "Shows current real-world clock time", Category.HUD, false, 8, 440);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;

        String time = LocalTime.now().format(TIME_FMT);
        String text = "⏰ " + time;

        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFFE2E8F0);
    }
}
