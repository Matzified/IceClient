package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class PlayerCountModule extends Module {

    public PlayerCountModule() {
        super("player_count", "Online Player Count", "Shows total players connected to the server", Category.HUD, false, 8, 615);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;

        int count = 1;
        if (mc.getNetworkHandler() != null) {
            count = mc.getNetworkHandler().getPlayerList().size();
        }

        String text = "👥 Online: " + count + (count == 1 ? " Player" : " Players");
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFF38BDF8);
    }
}
