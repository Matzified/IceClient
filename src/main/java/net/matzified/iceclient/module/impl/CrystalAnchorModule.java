package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class CrystalAnchorModule extends Module {

    public CrystalAnchorModule() {
        super("crystal_anchor", "Crystal & Anchor Counter", "Counts Crystals, Obsidian, Anchors, and Glowstone for modern PvP", Category.PVP, true, 8, 840);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        int crystals = 0;
        int obsidian = 0;
        int anchors = 0;
        int glowstone = 0;

        for (ItemStack item : mc.player.getInventory().main) {
            if (item.isOf(Items.END_CRYSTAL)) crystals += item.getCount();
            else if (item.isOf(Items.OBSIDIAN)) obsidian += item.getCount();
            else if (item.isOf(Items.RESPAWN_ANCHOR)) anchors += item.getCount();
            else if (item.isOf(Items.GLOWSTONE)) glowstone += item.getCount();
        }
        for (ItemStack item : mc.player.getInventory().offHand) {
            if (item.isOf(Items.END_CRYSTAL)) crystals += item.getCount();
            else if (item.isOf(Items.OBSIDIAN)) obsidian += item.getCount();
            else if (item.isOf(Items.RESPAWN_ANCHOR)) anchors += item.getCount();
            else if (item.isOf(Items.GLOWSTONE)) glowstone += item.getCount();
        }

        if (crystals == 0 && obsidian == 0 && anchors == 0 && glowstone == 0) return;

        String text = String.format("🔮 Cry: %d | Obs: %d | Anc: %d", crystals, obsidian, anchors);
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFFE879F9);
    }
}
