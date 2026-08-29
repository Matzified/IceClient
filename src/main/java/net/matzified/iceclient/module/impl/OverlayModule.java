package net.matzified.iceclient.module.impl;

import net.matzified.iceclient.module.Category;
import net.matzified.iceclient.module.Module;
import net.matzified.iceclient.setting.BooleanSetting;
import net.matzified.iceclient.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * 🧊 Overlay Customizer Module.
 * Allows competitive players to adjust:
 * - Screen fire overlay height (Low Fire)
 * - Ground fire height
 * - First-person held shield height offset (Low Shield)
 * - Small Totem / Low Totem scale & height offset
 * - Pumpkin and Portal overlay obstructions
 */
public class OverlayModule extends Module {

    private static OverlayModule INSTANCE;

    private final NumberSetting screenFireHeight;
    private final BooleanSetting lowShield;
    private final NumberSetting shieldHeight;
    private final BooleanSetting smallTotem;
    private final NumberSetting totemScale;
    private final NumberSetting totemHeight;
    private final NumberSetting groundFireHeight;
    private final BooleanSetting noPumpkin;
    private final BooleanSetting noPortal;

    public OverlayModule() {
        super(
                "overlay",
                "Overlay",
                "Customize screen fire, low shield, and small totem for competitive visibility",
                Category.VISUAL,
                true,
                0,
                0
        );
        INSTANCE = this;

        this.screenFireHeight = new NumberSetting(
                "screen_fire_height",
                "Screen Fire Height",
                "Offset of the burning fire overlay on your screen (Lower = better view)",
                0.35,
                0.0,
                1.0,
                0.05
        );

        this.lowShield = new BooleanSetting(
                "low_shield",
                "Low Shield",
                "Lowers the held shield in first person for maximum screen visibility",
                true
        );

        this.shieldHeight = new NumberSetting(
                "shield_height",
                "Shield Height Offset",
                "How low the shield sits in your hand",
                0.35,
                0.0,
                1.0,
                0.05
        );

        this.smallTotem = new BooleanSetting(
                "small_totem",
                "Small Totem",
                "Shrinks and lowers held Totem of Undying to prevent screen blocking",
                true
        );

        this.totemScale = new NumberSetting(
                "totem_scale",
                "Totem Scale",
                "Scale size of the held Totem of Undying (0.65 = compact)",
                0.65,
                0.2,
                1.0,
                0.05
        );

        this.totemHeight = new NumberSetting(
                "totem_height",
                "Totem Height Offset",
                "How low the totem sits in your hand",
                0.30,
                0.0,
                1.0,
                0.05
        );

        this.groundFireHeight = new NumberSetting(
                "ground_fire_height",
                "Ground Fire Height",
                "Height scale of fire blocks on the ground",
                0.5,
                0.1,
                1.0,
                0.05
        );

        this.noPumpkin = new BooleanSetting(
                "no_pumpkin",
                "No Pumpkin Overlay",
                "Removes carved pumpkin visual obstruction",
                false
        );

        this.noPortal = new BooleanSetting(
                "no_portal",
                "No Portal Overlay",
                "Removes the nauseating nether portal overlay effect",
                false
        );

        addSetting(screenFireHeight);
        addSetting(lowShield);
        addSetting(shieldHeight);
        addSetting(smallTotem);
        addSetting(totemScale);
        addSetting(totemHeight);
        addSetting(groundFireHeight);
        addSetting(noPumpkin);
        addSetting(noPortal);
    }

    public static OverlayModule getInstance() {
        return INSTANCE;
    }

    public double getScreenFireOffset() {
        if (!isEnabled()) return 0.0;
        return screenFireHeight.getValue();
    }

    public boolean isLowShieldActive() {
        return isEnabled() && lowShield.getValue();
    }

    public double getShieldOffset() {
        if (!isEnabled() || !lowShield.getValue()) return 0.0;
        return shieldHeight.getValue();
    }

    public boolean isSmallTotemActive() {
        return isEnabled() && smallTotem.getValue();
    }

    public double getTotemScale() {
        if (!isEnabled() || !smallTotem.getValue()) return 1.0;
        return totemScale.getValue();
    }

    public double getTotemOffset() {
        if (!isEnabled() || !smallTotem.getValue()) return 0.0;
        return totemHeight.getValue();
    }

    public double getGroundFireScale() {
        if (!isEnabled()) return 1.0;
        return groundFireHeight.getValue();
    }

    public boolean isNoPumpkin() {
        return isEnabled() && noPumpkin.getValue();
    }

    public boolean isNoPortal() {
        return isEnabled() && noPortal.getValue();
    }

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        // Render hooks handled in mixins
    }
}
