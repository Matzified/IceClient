package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class ChatCustomizerModule extends Module {

    public ChatCustomizerModule() {
        super("custom_chat", "Custom Chat & Transparency", "Translucent glass chat box with smooth scrolling and shadow", Category.UTILITY, true, 8, 1040);
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        // Active modifier
    }
}
