package net.matzified.iceclient.mixin;

import net.matzified.iceclient.gui.IceClientGuiScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {

    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void iceclient$addIceClientButton(CallbackInfo ci) {
        int buttonW = 204;
        int buttonH = 20;
        int x = this.width / 2 - 102;
        int y = this.height / 4 + 8; // Above standard buttons

        this.addDrawableChild(
            ButtonWidget.builder(
                Text.literal("§b🧊 Ice Client Mods §7(R-Shift)"),
                button -> {
                    if (this.client != null) {
                        this.client.setScreen(new IceClientGuiScreen());
                    }
                }
            )
            .dimensions(x, y - 24, buttonW, buttonH)
            .build()
        );
    }
}
