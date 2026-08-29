package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class WindChargeModule extends Module {

    public WindChargeModule() {
        super("wind_charge", "1.21 Wind Charge Counter", "Counts 1.21 Wind Charge and Breeze Rod items in inventory", Category.PVP, true, 8, 815);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        int count = 0;
        for (ItemStack item : mc.player.getInventory().main) {
            if (item.isOf(Items.WIND_CHARGE)) count += item.getCount();
        }
        for (ItemStack item : mc.player.getInventory().offHand) {
            if (item.isOf(Items.WIND_CHARGE)) count += item.getCount();
        }

        String text = "💨 Wind Charges: " + count;
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, count > 0 ? 0xFF38BDF8 : 0xFF94A3B8);
    }
}
