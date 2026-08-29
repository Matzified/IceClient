package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;

import java.util.Collection;

public class PotionStatusModule extends Module {

    public PotionStatusModule() {
        super("potions", "Potion Status", "Displays active potion buffs and remaining duration", Category.HUD, true, 8, 280);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
        if (effects.isEmpty()) return;

        int x = getX();
        int y = getY();
        int row = 0;
        int maxW = 80;

        for (StatusEffectInstance effect : effects) {
            String name = effect.getEffectType().value().getName().getString();
            String duration = StatusEffectUtil.getDurationText(effect, 1.0f, mc.world != null ? mc.world.getTickManager().getTickRate() : 20.0f).getString();
            String full = name + " " + duration;
            maxW = Math.max(maxW, font.getWidth(full) + 12);

            drawGlassBox(context, x, y + (row * 18), maxW, 16);
            context.drawTextWithShadow(font, full, x + 6, y + (row * 18) + 4, 0xFF38BDF8);
            row++;
        }

        setWidth(maxW);
        setHeight(Math.max(18, row * 18));
    }
}
