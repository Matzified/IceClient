package net.matzified.iceclient.launcher.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class AutoUpdater {

    public static final String CURRENT_VERSION = "1.0.0";
    private static final String VERSION_URL = "https://raw.githubusercontent.com/Matzified/IceClient/main/version.json";

    public static void checkForUpdatesAsync(JFrame parentFrame) {
        new Thread(() -> {
            try {
                URL url = new URL(VERSION_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "IceClient-Launcher");

                if (conn.getResponseCode() == 200) {
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
                    String remoteVersion = json.has("version") ? json.get("version").getAsString() : CURRENT_VERSION;
                    String releaseNotes = json.has("releaseNotes") ? json.get("releaseNotes").getAsString() : "";
                    String downloadUrl = json.has("downloadUrl") ? json.get("downloadUrl").getAsString() : null;

                    if (isNewerVersion(remoteVersion, CURRENT_VERSION)) {
                        SwingUtilities.invokeLater(() -> promptUpdate(parentFrame, remoteVersion, releaseNotes, downloadUrl));
                    }
                }
            } catch (Exception ignored) {
                // Silently ignore if offline or repository is private/initial setup
            }
        }).start();
    }

    private static boolean isNewerVersion(String remote, String current) {
        try {
            String[] rParts = remote.replaceAll("[^0-9.]", "").split("\\.");
            String[] cParts = current.replaceAll("[^0-9.]", "").split("\\.");

            int length = Math.max(rParts.length, cParts.length);
            for (int i = 0; i < length; i++) {
                int rVal = i < rParts.length ? Integer.parseInt(rParts[i]) : 0;
                int cVal = i < cParts.length ? Integer.parseInt(cParts[i]) : 0;
                if (rVal > cVal) return true;
                if (rVal < cVal) return false;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static void promptUpdate(JFrame parentFrame, String remoteVersion, String releaseNotes, String downloadUrl) {
        String msg = "🚀 A new Ice Client update is available on GitHub!\n\n" +
                "Installed Version: v" + CURRENT_VERSION + "\n" +
                "Latest GitHub Version: v" + remoteVersion + "\n\n" +
                "Release Notes:\n" + releaseNotes + "\n\n" +
                "Would you like to auto-update now?";

        int choice = JOptionPane.showConfirmDialog(
                parentFrame,
                msg,
                "Ice Client GitHub Auto-Updater",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION && downloadUrl != null) {
            downloadAndApplyUpdate(parentFrame, downloadUrl);
        }
    }

    private static void downloadAndApplyUpdate(JFrame parentFrame, String downloadUrl) {
        new Thread(() -> {
            try {
                URL url = new URL(downloadUrl);
                File currentJar = new File(AutoUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                File tempNewJar = new File(currentJar.getParentFile(), "IceClientLauncher_new.jar");

                try (InputStream in = url.openStream()) {
                    Files.copy(in, tempNewJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(parentFrame,
                            "Update downloaded successfully from GitHub!\nRestarting launcher...",
                            "Update Ready",
                            JOptionPane.INFORMATION_MESSAGE);

                    try {
                        ProcessBuilder pb = new ProcessBuilder("java", "-jar", tempNewJar.getAbsolutePath());
                        pb.start();
                        System.exit(0);
                    } catch (Exception e) {
                        try {
                            Desktop.getDesktop().browse(new URI("https://github.com/Matzified/IceClient"));
                        } catch (Exception ignored) {}
                    }
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(parentFrame,
                            "Failed to auto-download update: " + e.getMessage() + "\nOpening GitHub releases page...",
                            "Update Error",
                            JOptionPane.ERROR_MESSAGE);
                    try {
                        Desktop.getDesktop().browse(new URI("https://github.com/Matzified/IceClient"));
                    } catch (Exception ignored) {}
                });
            }
        }).start();
    }
}
