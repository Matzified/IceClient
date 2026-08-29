package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;

public class ArmorModule extends Module {

    public ArmorModule() {
        super("armor", "Armor & Equipment Status", "Shows durability of worn armor and main hand item", Category.PVP, true, 8, 185);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        int x = getX();
        int y = getY();
        int totalH = 0;

        Iterable<ItemStack> armor = mc.player.getArmorItems();
        int row = 0;

        for (ItemStack item : armor) {
            if (!item.isEmpty()) {
                context.drawItem(item, x, y + (row * 18));
                if (item.isDamageable()) {
                    int max = item.getMaxDamage();
                    int cur = max - item.getDamage();
                    int pct = (int) ((cur / (float) max) * 100);
                    int col = pct > 60 ? 0xFF34D399 : (pct > 25 ? 0xFFFBBF24 : 0xFFF87171);
                    context.drawTextWithShadow(font, pct + "%", x + 20, y + (row * 18) + 4, col);
                } else {
                    context.drawTextWithShadow(font, "100%", x + 20, y + (row * 18) + 4, 0xFF34D399);
                }
                row++;
                totalH += 18;
            }
        }

        ItemStack mainHand = mc.player.getMainHandStack();
        if (!mainHand.isEmpty()) {
            context.drawItem(mainHand, x, y + (row * 18));
            if (mainHand.isDamageable()) {
                int max = mainHand.getMaxDamage();
                int cur = max - mainHand.getDamage();
                int pct = (int) ((cur / (float) max) * 100);
                int col = pct > 60 ? 0xFF34D399 : (pct > 25 ? 0xFFFBBF24 : 0xFFF87171);
                context.drawTextWithShadow(font, pct + "%", x + 20, y + (row * 18) + 4, col);
            }
            row++;
            totalH += 18;
        }

        setWidth(60);
        setHeight(Math.max(18, totalH));
    }
}
