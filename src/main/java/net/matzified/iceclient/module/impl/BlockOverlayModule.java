package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.block.BlockState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class BlockOverlayModule extends Module {

    public BlockOverlayModule() {
        super("block_overlay", "Block Info & Overlay", "Shows material name and hardness of the block you are looking at", Category.UTILITY, true, 8, 940);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.textRenderer == null || mc.world == null || mc.crosshairTarget == null) return;
        TextRenderer font = mc.textRenderer;

        if (mc.crosshairTarget.getType() != HitResult.Type.BLOCK) return;
        BlockHitResult hit = (BlockHitResult) mc.crosshairTarget;
        BlockState state = mc.world.getBlockState(hit.getBlockPos());
        if (state.isAir()) return;

        String blockName = state.getBlock().getName().getString();
        float hardness = state.getHardness(mc.world, hit.getBlockPos());

        String text = String.format("🧱 %s (Hardness: %.1f)", blockName, hardness);
        int padding = 6;
        int w = font.getWidth(text) + (padding * 2);
        int h = 18;
        setWidth(w);
        setHeight(h);

        drawGlassBox(context, getX(), getY(), w, h);
        context.drawTextWithShadow(font, text, getX() + padding, getY() + 5, 0xFF38BDF8);
    }
}
