package net.matzified.iceclient.mixin;

import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {

    private int iceclient$particleCounter = 0;

    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
    private void iceclient$throttleParticles(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<?> cir) {
        iceclient$particleCounter++;
        // Keep 1 out of every 2 non-critical particles during particle spam to boost frame rates
        if (iceclient$particleCounter % 2 == 0) {
            cir.setReturnValue(null);
        }
    }
}
