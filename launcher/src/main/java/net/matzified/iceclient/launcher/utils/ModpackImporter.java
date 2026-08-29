package net.matzified.iceclient.launcher.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.matzified.iceclient.launcher.model.Profile;
import net.matzified.iceclient.launcher.profile.ProfileManager;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModpackImporter {

    public static Profile importModpackFile(File packFile, String profileName) {
        if (!packFile.exists()) return null;

        String id = "profile_pack_" + UUID.randomUUID().toString().substring(0, 8);
        File profilesDir = new File(ProfileManager.getInstance().getRootDir(), "profiles");
        String folderName = profileName.replaceAll("[^a-zA-Z0-9.-]", "_") + "-" + UUID.randomUUID().toString().substring(0, 4);
        File gameDir = new File(profilesDir, folderName);
        gameDir.mkdirs();

        Profile p = new Profile(
                id,
                profileName,
                "Imported modpack: " + packFile.getName(),
                "1.21.1",
                "0.16.0",
                4,
                ProfileManager.DEFAULT_JVM_ARGS,
                gameDir.getAbsolutePath(),
                "📦",
                "#10B981"
        );

        if (packFile.getName().endsWith(".mrpack")) {
            parseMrpackFile(packFile, p);
        } else {
            unzipToDirectory(packFile, gameDir);
        }

        return p;
    }

    private static void parseMrpackFile(File mrpackFile, Profile profile) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(mrpackFile))) {
            ZipEntry entry;
            JsonObject indexObj = null;

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equalsIgnoreCase("modrinth.index.json")) {
                    indexObj = JsonParser.parseReader(new InputStreamReader(zis)).getAsJsonObject();
                    break;
                }
            }

            if (indexObj != null) {
                if (indexObj.has("dependencies")) {
                    JsonObject deps = indexObj.getAsJsonObject("dependencies");
                    if (deps.has("minecraft")) {
                        profile.setMcVersion(deps.get("minecraft").getAsString());
                    }
                }

                if (indexObj.has("files")) {
                    JsonArray files = indexObj.getAsJsonArray("files");
                    File modsDir = new File(profile.getModDir());
                    modsDir.mkdirs();

                    for (JsonElement elem : files) {
                        JsonObject fileObj = elem.getAsJsonObject();
                        String path = fileObj.get("path").getAsString();
                        JsonArray downloads = fileObj.getAsJsonArray("downloads");

                        if (downloads != null && downloads.size() > 0) {
                            String downloadUrl = downloads.get(0).getAsString();
                            File targetFile = new File(profile.getGameDir(), path);
                            targetFile.getParentFile().mkdirs();
                            downloadFile(downloadUrl, targetFile);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Second pass: extract overrides folder if present
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(mrpackFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith("overrides/") && !entry.isDirectory()) {
                    String subPath = name.substring("overrides/".length());
                    File target = new File(profile.getGameDir(), subPath);
                    target.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(target)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void unzipToDirectory(File zipFile, File destDir) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File file = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void downloadFile(String urlStr, File targetFile) {
        try {
            URL url = java.net.URI.create(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "IceClientLauncher/1.0.0");
            try (InputStream in = conn.getInputStream()) {
                Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
