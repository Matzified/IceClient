package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class BedWarsStatsModule extends Module {

    private static int bedsBroken = 0;
    private static int finalKills = 0;

    public BedWarsStatsModule() {
        super("bedwars_stats", "BedWars / Minigames HUD", "Displays beds broken & final kills in minigames", Category.PVP, false, 8, 740);
    }

    public static void addBedBreak() { bedsBroken++; }
    public static void addFinalKill() { finalKills++; }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;

        String text = "🛏️ Beds: " + bedsBroken + " | 💀 Final Kills: " + finalKills;
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFFFBBF24);
    }
}
