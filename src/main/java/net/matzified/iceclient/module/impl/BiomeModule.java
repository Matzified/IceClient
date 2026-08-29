package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;

public class BiomeModule extends Module {

    public BiomeModule() {
        super("biome", "Biome Display", "Shows current Minecraft world biome name", Category.HUD, true, 8, 540);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.world == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        RegistryEntry<Biome> biomeEntry = mc.world.getBiome(mc.player.getBlockPos());
        String biomeName = biomeEntry.getKey().map(k -> {
            String path = k.getValue().getPath();
            return path.replace("_", " ").toUpperCase();
        }).orElse("UNKNOWN");

        String text = "🌲 Biome: " + biomeName;
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFF34D399);
    }
}
