package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class TargetHudModule extends Module {

    private static LivingEntity lastTarget = null;
    private static long lastTargetTime = 0;

    public TargetHudModule() {
        super("target_hud", "Target HUD", "Displays opponent health bar, distance, and name in combat", Category.PVP, true, 300, 200);
        setWidth(140);
        setHeight(42);
    }

    public static void setTarget(LivingEntity target) {
        lastTarget = target;
        lastTargetTime = System.currentTimeMillis();
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        // Check if looking at an entity
        if (mc.crosshairTarget != null && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            EntityHitResult hit = (EntityHitResult) mc.crosshairTarget;
            Entity ent = hit.getEntity();
            if (ent instanceof LivingEntity) {
                lastTarget = (LivingEntity) ent;
                lastTargetTime = System.currentTimeMillis();
            }
        }

        if (lastTarget == null || System.currentTimeMillis() - lastTargetTime > 4000 || !lastTarget.isAlive()) {
            return;
        }

        int x = getX();
        int y = getY();
        int w = 140;
        int h = 42;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, x, y, w, h);

        // Name
        String name = lastTarget.getName().getString();
        if (font.getWidth(name) > w - 16) name = font.trimToWidth(name, w - 24) + "...";
        context.drawTextWithShadow(font, name, x + 8, y + 6, 0xFFFFFFFF);

        // Health details
        float hp = lastTarget.getHealth();
        float maxHp = lastTarget.getMaxHealth();
        int hpPct = (int) ((hp / Math.max(1.0f, maxHp)) * 100);

        String hpText = String.format("%.1f / %.1f HP", hp, maxHp);
        context.drawTextWithShadow(font, hpText, x + 8, y + 18, 0xFF94A3B8);

        // Health Progress Bar
        int barW = w - 16;
        int barH = 6;
        int barX = x + 8;
        int barY = y + 30;

        context.fill(barX, barY, barX + barW, barY + barH, 0xFF1E293B);
        int fillW = (int) (barW * (Math.min(100, Math.max(0, hpPct)) / 100.0f));
        int hpCol = hpPct > 50 ? 0xFF34D399 : (hpPct > 20 ? 0xFFFBBF24 : 0xFFF87171);
        context.fill(barX, barY, barX + fillW, barY + barH, hpCol);
    }
}
