package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;

public class ItemCountModule extends Module {

    public ItemCountModule() {
        super("item_count", "Held Item Counter", "Counts total inventory stacks of your currently held item", Category.PVP, true, 8, 690);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        ItemStack mainHand = mc.player.getMainHandStack();
        if (mainHand.isEmpty()) return;

        int totalCount = 0;
        for (ItemStack item : mc.player.getInventory().main) {
            if (item.isOf(mainHand.getItem())) {
                totalCount += item.getCount();
            }
        }
        for (ItemStack item : mc.player.getInventory().offHand) {
            if (item.isOf(mainHand.getItem())) {
                totalCount += item.getCount();
            }
        }

        String itemName = mainHand.getName().getString();
        String text = "📦 " + itemName + " x" + totalCount;

        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFF38BDF8);
    }
}
