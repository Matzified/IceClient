package net.matzified.iceclient.module;

import net.matzified.iceclient.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {

    protected final MinecraftClient mc = MinecraftClient.getInstance();
    private final String id;
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private int x;
    private int y;
    private int width = 100;
    private int height = 18;

    protected final List<Setting<?>> settings = new ArrayList<>();

    // Universal default settings for EVERY module
    public final ColorSetting colorSetting;
    public final BooleanSetting backgroundSetting;
    public final BooleanSetting shadowSetting;
    public final ModeSetting borderSetting;
    public final NumberSetting scaleSetting;

    public Module(String id, String name, String description, Category category, boolean defaultEnabled, int defaultX, int defaultY) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.enabled = defaultEnabled;
        this.x = defaultX;
        this.y = defaultY;

        // Register universal settings on every single module
        this.colorSetting = new ColorSetting("textColor", "Text Color Theme", "Choose the primary color theme for this module", 0xFF38BDF8);
        this.backgroundSetting = new BooleanSetting("glassBackground", "Glass Background", "Render dark translucent glass backdrop box", true);
        this.shadowSetting = new BooleanSetting("textShadow", "Text Shadow", "Draw drop shadow for better visibility", true);
        this.borderSetting = new ModeSetting("borderStyle", "Border Glow Style", "Select box border style", new String[]{"Cyan Glow", "Emerald", "Subtle", "None"}, "Cyan Glow");
        this.scaleSetting = new NumberSetting("scale", "Scale", "HUD element scale multiplier", 1.0, 0.6, 1.8, 0.1);

        settings.add(colorSetting);
        settings.add(backgroundSetting);
        settings.add(shadowSetting);
        settings.add(borderSetting);
        settings.add(scaleSetting);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public void toggle() {
        setEnabled(!this.enabled);
    }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public List<Setting<?>> getSettings() { return settings; }
    public void addSetting(Setting<?> s) { settings.add(s); }

    public Setting<?> getSetting(String id) {
        for (Setting<?> s : settings) {
            if (s.getId().equalsIgnoreCase(id)) return s;
        }
        return null;
    }

    public int getTextColor() {
        return colorSetting != null ? colorSetting.getValue() : 0xFF38BDF8;
    }

    public boolean hasBackground() {
        return backgroundSetting == null || backgroundSetting.getValue();
    }

    public boolean hasTextShadow() {
        return shadowSetting == null || shadowSetting.getValue();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}

    public abstract void render(DrawContext context, RenderTickCounter tickCounter);

    /**
     * Helper to draw a glassmorphic background box with customizable styles.
     */
    protected void drawGlassBox(DrawContext context, int x, int y, int w, int h) {
        if (!hasBackground()) return;

        // Translucent midnight backdrop
        context.fill(x, y, x + w, y + h, 0xCC0D1117);

        String border = borderSetting != null ? borderSetting.getValue() : "Cyan Glow";
        if ("None".equalsIgnoreCase(border)) return;

        int accentCol = "Emerald".equalsIgnoreCase(border) ? 0xFF34D399 : 0xFF38BDF8;
        int subtleCol = "Subtle".equalsIgnoreCase(border) ? 0x22FFFFFF : 0x3338BDF8;

        // Accent border line
        context.fill(x, y, x + 2, y + h, accentCol);
        // Subtle outline
        context.fill(x + 2, y, x + w, y + 1, subtleCol);
        context.fill(x + 2, y + h - 1, x + w, y + h, subtleCol);
        context.fill(x + w - 1, y, x + w, y + h, subtleCol);
    }
}
