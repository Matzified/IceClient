package net.matzified.iceclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.matzified.iceclient.gui.IceClientGuiScreen;
import net.matzified.iceclient.hud.IceClientHud;
import net.matzified.iceclient.module.ModuleManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IceClient implements ClientModInitializer {
    public static final String MOD_ID = "iceclient";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding rightShiftKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Ice Client Modular Suite v1.0.0 for Fabric 1.21.1!");

        // 1. Initialize In-Engine Base Game Optimization Core
        net.matzified.iceclient.optimizer.IceOptimizerEngine.getInstance().init();

        // 2. Register Right Shift Keybind for Mod Menu
        rightShiftKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.iceclient.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.iceclient"
        ));

        // 3. Client Tick Event (Handle Right Shift, Optimizer & Module Ticks)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (rightShiftKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new IceClientGuiScreen());
                }
            }
            ModuleManager.getInstance().onTick();
            net.matzified.iceclient.optimizer.IceOptimizerEngine.getInstance().onTick();
        });

        // 3. Register Modular HUD Render Callback
        HudRenderCallback.EVENT.register(new IceClientHud());
    }
}
