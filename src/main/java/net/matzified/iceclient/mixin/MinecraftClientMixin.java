package net.matzified.iceclient.mixin;

import net.matzified.iceclient.module.impl.ComboModule;
import net.matzified.iceclient.module.impl.CpsModule;
import net.matzified.iceclient.module.impl.ReachModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow public ClientPlayerEntity player;
    @Shadow public HitResult crosshairTarget;

    @Inject(method = "doAttack", at = @At("HEAD"))
    private void iceclient$onLeftClick(CallbackInfoReturnable<Boolean> cir) {
        CpsModule.registerClick(false);

        if (crosshairTarget != null && crosshairTarget.getType() == HitResult.Type.ENTITY && player != null) {
            EntityHitResult entityHit = (EntityHitResult) crosshairTarget;
            Entity target = entityHit.getEntity();
            if (target != null) {
                double dist = player.getEyePos().distanceTo(entityHit.getPos());
                ReachModule.setReach(dist);
                ComboModule.registerHit();
            }
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void iceclient$onRightClick(CallbackInfo ci) {
        CpsModule.registerClick(true);
    }
}
