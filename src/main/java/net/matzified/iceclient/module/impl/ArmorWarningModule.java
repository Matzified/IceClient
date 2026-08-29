package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.matzified.iceclient.setting.NumberSetting;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;

public class ArmorWarningModule extends Module {

    public final NumberSetting warningPercent = new NumberSetting("warningPercent", "Warning Threshold (%)", "Trigger warning when armor durability drops below percentage", 15.0, 5.0, 40.0, 5.0);

    public ArmorWarningModule() {
        super("armor_warning", "Low Armor Durability Warning", "Alerts when armor pieces are about to break in combat", Category.PVP, true, 8, 900);
        addSetting(warningPercent);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        boolean warning = false;
        String warningPiece = "";

        double threshold = warningPercent.getValue() / 100.0;

        for (ItemStack stack : mc.player.getArmorItems()) {
            if (!stack.isEmpty() && stack.isDamageable()) {
                int max = stack.getMaxDamage();
                int current = max - stack.getDamage();
                if ((double) current / max <= threshold) {
                    warning = true;
                    warningPiece = stack.getName().getString();
                    break;
                }
            }
        }

        if (!warning) return;

        String text = "⚠️ LOW ARMOR: " + warningPiece.toUpperCase();
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFFEF4444);
    }
}
