package net.matzified.iceclient.mixin;

import net.matzified.iceclient.module.impl.OverlayModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {

    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private static void onRenderFireOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        OverlayModule module = OverlayModule.getInstance();
        if (module != null && module.isEnabled()) {
            double offset = module.getScreenFireOffset();
            if (offset > 0.0) {
                matrices.translate(0.0, -offset, 0.0);
            }
        }
    }
}
