package net.matzified.iceclient.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.matzified.iceclient.module.Module;
import net.matzified.iceclient.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class IceClientHud implements HudRenderCallback {

    private final ModuleManager moduleManager = ModuleManager.getInstance();

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        // Hide modules when F1 (hudHidden) or F3 (debugHud active) is on
        if (client.options.hudHidden || (client.getDebugHud() != null && client.getDebugHud().shouldShowDebugHud())) {
            return;
        }

        for (Module module : moduleManager.getModules()) {
            if (module.isEnabled()) {
                module.render(drawContext, tickCounter);
            }
        }
    }
}
