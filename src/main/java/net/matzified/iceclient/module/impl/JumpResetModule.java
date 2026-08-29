package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class JumpResetModule extends Module {

    private static long lastDamageTime = 0;
    private static boolean jumpedAfterDamage = false;
    private static int resetScore = 0;

    public JumpResetModule() {
        super("jump_reset", "Jump Reset Indicator", "Evaluates your jump reset timing when taking knockback", Category.PVP, true, 8, 915);
    }

    public static void onDamageTaken() {
        lastDamageTime = System.currentTimeMillis();
        jumpedAfterDamage = false;
    }

    @Override
    public void onTick() {
        if (mc.player != null && lastDamageTime > 0) {
            long dt = System.currentTimeMillis() - lastDamageTime;
            if (dt <= 300 && !jumpedAfterDamage) {
                if (mc.options.jumpKey.isPressed() || !mc.player.isOnGround()) {
                    jumpedAfterDamage = true;
                    resetScore = Math.max(50, 100 - (int)(dt / 3));
                }
            }
        }
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.player == null) return;
        TextRenderer font = mc.textRenderer;

        long now = System.currentTimeMillis();
        if (now - lastDamageTime > 4000) {
            return;
        }

        String text;
        int col;
        if (jumpedAfterDamage) {
            text = String.format("🎯 Jump Reset: %d%% (Perfect)", resetScore);
            col = 0xFF34D399;
        } else {
            text = "🎯 Jump Reset: Ready";
            col = 0xFF38BDF8;
        }

        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, col);
    }
}
