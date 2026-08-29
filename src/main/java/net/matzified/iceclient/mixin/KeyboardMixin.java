package net.matzified.iceclient.mixin;

import net.matzified.iceclient.gui.IceClientGuiScreen;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {

    @Shadow @Final private MinecraftClient client;

    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void iceclient$onRightShiftPressed(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT && action == GLFW.GLFW_PRESS) {
            if (client != null) {
                // Don't intercept if typing in chat
                if (client.currentScreen instanceof ChatScreen) {
                    return;
                }

                if (client.currentScreen == null || client.currentScreen instanceof GameMenuScreen) {
                    client.setScreen(new IceClientGuiScreen());
                    ci.cancel();
                } else if (client.currentScreen instanceof IceClientGuiScreen) {
                    client.setScreen(null);
                    ci.cancel();
                }
            }
        }
    }
}
