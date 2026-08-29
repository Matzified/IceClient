package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;

public class ArmorWarningModule extends Module {

    public ArmorWarningModule() {
        super("armor_warning", "Low Armor Durability Warning", "Alerts when any armor piece drops below 15% durability", Category.PVP, true, 8, 890);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        boolean hasLowArmor = false;
        String pieceName = "";
        int lowestPct = 100;

        for (ItemStack armor : mc.player.getArmorItems()) {
            if (!armor.isEmpty() && armor.isDamageable()) {
                int max = armor.getMaxDamage();
                int cur = max - armor.getDamage();
                int pct = (int) ((cur / (float) max) * 100);
                if (pct <= 15) {
                    hasLowArmor = true;
                    if (pct < lowestPct) {
                        lowestPct = pct;
                        pieceName = armor.getName().getString();
                    }
                }
            }
        }

        if (!hasLowArmor) return;

        String text = "⚠️ LOW ARMOR: " + pieceName + " (" + lowestPct + "%)";
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        context.fill(getX(), getY(), getX() + w, getY() + h, 0xCC7F1D1D);
        context.fill(getX(), getY(), getX() + 2, getY() + h, 0xFFEF4444);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFFFCA5A5);
    }
}
