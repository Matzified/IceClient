package net.matzified.iceclient.setting;

public class ColorSetting extends Setting<Integer> {
    public static final int[] PALETTE = {
            0xFF38BDF8, // Ice Cyan
            0xFF34D399, // Emerald Green
            0xFFF87171, // Crimson Red
            0xFFC084FC, // Neon Purple
            0xFFFBBF24, // Amber Yellow
            0xFFFB923C, // Sunset Orange
            0xFFF472B6, // Pink
            0xFFFFFFFF, // Pure White
            0xFF94A3B8  // Slate Gray
    };

    public ColorSetting(String id, String name, String description, int defaultColor) {
        super(id, name, description, defaultColor);
    }
}
