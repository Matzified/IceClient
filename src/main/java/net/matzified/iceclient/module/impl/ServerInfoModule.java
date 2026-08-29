package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.render.RenderTickCounter;

public class ServerInfoModule extends Module {

    public ServerInfoModule() {
        super("server_ip", "Server Address Display", "Displays connected multiplayer server IP", Category.HUD, false, 8, 465);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null) return;
        TextRenderer font = mc.textRenderer;

        String server = "Singleplayer";
        if (mc.getCurrentServerEntry() != null) {
            ServerInfo info = mc.getCurrentServerEntry();
            server = info.address;
        }

        String text = "🌐 " + server;
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFF38BDF8);
    }
}
