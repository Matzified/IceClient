package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class AttackCooldownModule extends Module {

    public AttackCooldownModule() {
        super("attack_cooldown", "1.21+ Attack Cooldown", "Displays modern weapon attack charge percentage and readiness", Category.PVP, true, 8, 790);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        float charge = mc.player.getAttackCooldownProgress(0.0f);
        int pct = (int) (charge * 100);

        String text = pct >= 100 ? "⚔ Attack: 100% READY" : String.format("⚔ Charge: %d%%", pct);
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        int col = pct >= 100 ? 0xFF34D399 : (pct > 60 ? 0xFFFBBF24 : 0xFFF87171);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, col);
    }
}
