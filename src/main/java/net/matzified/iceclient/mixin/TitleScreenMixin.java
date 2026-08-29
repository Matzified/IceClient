package net.matzified.iceclient.mixin;

import net.matzified.iceclient.gui.IceClientGuiScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void iceclient$addTitleIceClientButton(CallbackInfo ci) {
        int buttonW = 120;
        int buttonH = 20;
        int x = 8;
        int y = 8;

        this.addDrawableChild(
            ButtonWidget.builder(
                Text.literal("§b🧊 Ice Client"),
                button -> {
                    if (this.client != null) {
                        this.client.setScreen(new IceClientGuiScreen());
                    }
                }
            )
            .dimensions(x, y, buttonW, buttonH)
            .build()
        );
    }
}
