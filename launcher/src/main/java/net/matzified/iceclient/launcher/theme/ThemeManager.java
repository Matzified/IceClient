package net.matzified.iceclient.launcher.theme;

import net.matzified.iceclient.launcher.profile.ProfileManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThemeManager {

    public enum ClientTheme {
        BLUE("Ice Blue", "/assets/logo.png", new Color(56, 189, 248), new Color(2, 132, 199), new Color(13, 15, 20), new Color(24, 28, 38), new Color(56, 189, 248, 120)),
        CHROME("Chrome", "/assets/ChromeGem.png", new Color(226, 232, 240), new Color(148, 163, 184), new Color(15, 17, 21), new Color(28, 31, 38), new Color(203, 213, 225, 120)),
        GREEN("Forest Green", "/assets/ForestGem.png", new Color(34, 197, 94), new Color(16, 185, 129), new Color(11, 18, 14), new Color(20, 32, 24), new Color(34, 197, 94, 120)),
        RED("Volcano Red", "/assets/VolcanoGem.png", new Color(239, 68, 68), new Color(220, 38, 38), new Color(20, 12, 14), new Color(36, 20, 24), new Color(239, 68, 68, 120)),
        BLACK("Shadow Black", "/assets/ShadowGem.png", new Color(168, 85, 247), new Color(126, 34, 206), new Color(8, 9, 12), new Color(18, 20, 28), new Color(168, 85, 247, 120));

        private final String displayName;
        private final String assetPath;
        private final Color accentColor;
        private final Color secondaryColor;
        private final Color bgColor;
        private final Color cardBgColor;
        private final Color borderColor;

        ClientTheme(String displayName, String assetPath, Color accentColor, Color secondaryColor, Color bgColor, Color cardBgColor, Color borderColor) {
            this.displayName = displayName;
            this.assetPath = assetPath;
            this.accentColor = accentColor;
            this.secondaryColor = secondaryColor;
            this.bgColor = bgColor;
            this.cardBgColor = cardBgColor;
            this.borderColor = borderColor;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getAssetPath() {
            return assetPath;
        }

        public Color getAccentColor() {
            return accentColor;
        }

        public Color getSecondaryColor() {
            return secondaryColor;
        }

        public Color getBgColor() {
            return bgColor;
        }

        public Color getCardBgColor() {
            return cardBgColor;
        }

        public Color getBorderColor() {
            return borderColor;
        }
    }

    private static ThemeManager instance;
    private ClientTheme currentTheme = ClientTheme.BLUE;
    private final File themeFile;
    private final Map<String, Image> imageCache = new HashMap<>();
    private final List<ThemeChangeListener> listeners = new ArrayList<>();

    public interface ThemeChangeListener {
        void onThemeChanged(ClientTheme newTheme);
    }

    private ThemeManager() {
        File rootDir = ProfileManager.getInstance().getRootDir();
        themeFile = new File(rootDir, "theme.txt");
        loadTheme();
    }

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    private void loadTheme() {
        if (themeFile.exists()) {
            try (FileReader reader = new FileReader(themeFile)) {
                char[] buf = new char[64];
                int len = reader.read(buf);
                if (len > 0) {
                    String name = new String(buf, 0, len).trim().toUpperCase();
                    for (ClientTheme t : ClientTheme.values()) {
                        if (t.name().equalsIgnoreCase(name) || t.getDisplayName().equalsIgnoreCase(name)) {
                            currentTheme = t;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void saveTheme() {
        try (FileWriter writer = new FileWriter(themeFile)) {
            writer.write(currentTheme.name());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ClientTheme getCurrentTheme() {
        return currentTheme;
    }

    public void setTheme(ClientTheme theme) {
        if (theme == null || this.currentTheme == theme) return;
        this.currentTheme = theme;
        saveTheme();
        for (ThemeChangeListener l : listeners) {
            try {
                l.onThemeChanged(theme);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void addListener(ThemeChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public Image getThemeGemImage(ClientTheme theme) {
        String path = theme.getAssetPath();
        if (imageCache.containsKey(path)) {
            return imageCache.get(path);
        }
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                Image img = ImageIO.read(is);
                if (img != null) {
                    imageCache.put(path, img);
                    return img;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Image getCurrentGemImage() {
        return getThemeGemImage(currentTheme);
    }

    public ImageIcon getThemedIcon(int width, int height) {
        Image img = getCurrentGemImage();
        if (img != null) {
            Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }
        return null;
    }

    public ImageIcon getThemedIconFor(ClientTheme theme, int width, int height) {
        Image img = getThemeGemImage(theme);
        if (img != null) {
            Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);
        }
        return null;
    }
}
