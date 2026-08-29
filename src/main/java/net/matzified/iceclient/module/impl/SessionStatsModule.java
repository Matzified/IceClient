package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class SessionStatsModule extends Module {

    private static final long sessionStart = System.currentTimeMillis();
    private static int kills = 0;
    private static int deaths = 0;

    public SessionStatsModule() {
        super("session_stats", "Session Stats", "Displays time played, kills, and session statistics", Category.HUD, false, 8, 715);
    }

    public static void addKill() { kills++; }
    public static void addDeath() { deaths++; }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;

        long elapsedSec = (System.currentTimeMillis() - sessionStart) / 1000L;
        long mins = elapsedSec / 60L;
        long hours = mins / 60L;
        String timeStr = hours > 0 ? String.format("%dh %dm", hours, mins % 60) : String.format("%dm %ds", mins, elapsedSec % 60);

        String text = "⏱️ Playtime: " + timeStr + " | K/D: " + kills + "/" + deaths;
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFF38BDF8);
    }
}
