package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class TotemCounterModule extends Module {

    public TotemCounterModule() {
        super("totem_count", "Totem Counter", "Displays total Totems of Undying in your inventory", Category.PVP, true, 8, 665);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        int count = 0;
        for (ItemStack item : mc.player.getInventory().main) {
            if (item.isOf(Items.TOTEM_OF_UNDYING)) count += item.getCount();
        }
        for (ItemStack item : mc.player.getInventory().offHand) {
            if (item.isOf(Items.TOTEM_OF_UNDYING)) count += item.getCount();
        }

        String text = "🛡️ Totems: " + count;
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, count > 0 ? 0xFFFBBF24 : 0xFFF87171);
    }
}
