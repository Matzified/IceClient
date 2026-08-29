package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

public class CpsModule extends Module {

    private static final List<Long> leftClicks = new ArrayList<>();
    private static final List<Long> rightClicks = new ArrayList<>();

    public CpsModule() {
        super("cps", "CPS Counter", "Displays Left & Right click CPS speed", Category.PVP, true, 8, 30);
    }

    public static void registerClick(int button) {
        long now = System.currentTimeMillis();
        if (button == 0) {
            leftClicks.add(now);
        } else if (button == 1) {
            rightClicks.add(now);
        }
    }

    private int getCps(List<Long> clicks) {
        long now = System.currentTimeMillis();
        clicks.removeIf(time -> now - time > 1000);
        return clicks.size();
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;

        int lCps = getCps(leftClicks);
        int rCps = getCps(rightClicks);
        String text = "⚔ CPS: " + lCps + " | " + rCps;

        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFF38BDF8);
    }
}
