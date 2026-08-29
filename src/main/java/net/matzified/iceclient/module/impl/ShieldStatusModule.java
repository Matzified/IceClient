package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class ShieldStatusModule extends Module {

    public ShieldStatusModule() {
        super("shield_status", "1.21 Shield Cooldown Status", "Shows shield durability and axe-disable cooldown timer", Category.PVP, true, 8, 865);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        boolean hasShield = mc.player.getMainHandStack().isOf(Items.SHIELD) || mc.player.getOffHandStack().isOf(Items.SHIELD);
        if (!hasShield) return;

        boolean onCooldown = mc.player.getItemCooldownManager().isCoolingDown(Items.SHIELD);
        float cd = mc.player.getItemCooldownManager().getCooldownProgress(Items.SHIELD, 0.0f);

        String text = onCooldown ? String.format("🛡️ SHIELD DISABLED (%.1fs)", cd * 5.0f) : "🛡️ Shield Ready";
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, onCooldown ? 0xFFF87171 : 0xFF34D399);
    }
}
