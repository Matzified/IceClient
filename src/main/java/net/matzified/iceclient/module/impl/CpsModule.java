package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.matzified.iceclient.setting.BooleanSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

public class CpsModule extends Module {

    private static final List<Long> leftClicks = new ArrayList<>();
    private static final List<Long> rightClicks = new ArrayList<>();

    public final BooleanSetting showLmb = new BooleanSetting("showLmb", "Show Left Clicks (LMB)", "Track left mouse click rate", true);
    public final BooleanSetting showRmb = new BooleanSetting("showRmb", "Show Right Clicks (RMB)", "Track right mouse click rate", true);

    public CpsModule() {
        super("cps", "CPS Counter", "Displays Left & Right Mouse Clicks Per Second", Category.HUD, true, 8, 32);
        addSetting(showLmb);
        addSetting(showRmb);
    }

    public static void registerClick(int button) {
        registerClick(button == 1);
    }

    public static void registerClick(boolean rightClick) {
        long now = System.currentTimeMillis();
        if (rightClick) {
            rightClicks.add(now);
        } else {
            leftClicks.add(now);
        }
    }

    private static int getCps(List<Long> list) {
        long now = System.currentTimeMillis();
        list.removeIf(time -> now - time > 1000);
        return list.size();
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;

        int lmb = getCps(leftClicks);
        int rmb = getCps(rightClicks);

        String text;
        if (showLmb.getValue() && showRmb.getValue()) {
            text = "CPS: " + lmb + " | " + rmb;
        } else if (showLmb.getValue()) {
            text = "LMB: " + lmb + " CPS";
        } else if (showRmb.getValue()) {
            text = "RMB: " + rmb + " CPS";
        } else {
            text = lmb + " CPS";
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
