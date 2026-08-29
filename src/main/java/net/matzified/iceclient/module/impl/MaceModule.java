package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class MaceModule extends Module {

    public MaceModule() {
        super("mace_hud", "1.21 Mace Stomp Multiplier", "Calculates height bonus damage and fall distance for the 1.21 Mace", Category.PVP, true, 8, 765);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        ItemStack mainHand = mc.player.getMainHandStack();
        boolean holdingMace = mainHand.isOf(Items.MACE);

        float fallDist = mc.player.fallDistance;
        // In 1.21, Mace damage scales with fall distance
        float bonusDmg = fallDist > 1.5f ? (fallDist * 3.0f) : 0.0f;

        if (!holdingMace && fallDist <= 0.5f) {
            return;
        }

        String text = String.format("🔨 Mace: +%.1f DMG (%.1fm Fall)", bonusDmg, fallDist);
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        int col = fallDist > 5.0f ? 0xFFF87171 : (fallDist > 2.0f ? 0xFFFBBF24 : 0xFF38BDF8);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, col);
    }
}
