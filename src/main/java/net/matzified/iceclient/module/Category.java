package net.matzified.iceclient.module;

public enum Category {
    ALL("All Modules", "🌐"),
    HUD("HUD & Info", "📊"),
    PVP("Combat & PvP", "⚔️"),
    PERFORMANCE("Performance", "⚡"),
    UTILITY("Utility", "🛠️"),
    VISUAL("Visual & Aesthetics", "🎨");

    public final String displayName;
    public final String icon;

    Category(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }
}
