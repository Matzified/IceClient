package net.matzified.iceclient.launcher.modrinth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.matzified.iceclient.launcher.model.Profile;
import net.matzified.iceclient.launcher.profile.ProfileManager;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ModrinthApiHandler {

    public enum ProjectType {
        MODS("mod", "Mods"),
        RESOURCEPACKS("resourcepack", "Resource Packs"),
        DATAPACKS("datapack", "Data Packs"),
        SHADERS("shader", "Shaders");

        public final String apiType;
        public final String displayName;

        ProjectType(String apiType, String displayName) {
            this.apiType = apiType;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static class ModResult {
        public String title;
        public String description;
        public String projectId;
        public String downloads;
        public String iconUrl;
        public String author;
        public ProjectType projectType;
        public boolean isInstalled = false;

        public ModResult(String title, String description, String projectId, String downloads, String iconUrl, String author, ProjectType projectType) {
            this.title = title;
            this.description = description;
            this.projectId = projectId;
            this.downloads = downloads;
            this.iconUrl = iconUrl;
            this.author = author;
            this.projectType = projectType;
        }
    }

    public static List<ModResult> searchModrinth(String query, String mcVersion, ProjectType projectType) {
        List<ModResult> results = new ArrayList<>();
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            // Filter for Fabric and active version if provided
            String facetsStr = "[[\"project_type:" + projectType.apiType + "\"],[\"categories:fabric\"]]";
            String facetsParam = URLEncoder.encode(facetsStr, StandardCharsets.UTF_8);

            String urlStr = "https://api.modrinth.com/v2/search?query=" + encodedQuery + "&facets=" + facetsParam + "&limit=24";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "IceClientLauncher/1.0.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() == 200) {
                JsonObject json = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
                JsonArray hits = json.getAsJsonArray("hits");

                for (JsonElement element : hits) {
                    JsonObject hit = element.getAsJsonObject();
                    String title = hit.has("title") ? hit.get("title").getAsString() : "Unknown";
                    String description = hit.has("description") ? hit.get("description").getAsString() : "";
                    String projectId = hit.get("project_id").getAsString();
                    String downloads = hit.has("downloads") ? String.format("%,d", hit.get("downloads").getAsInt()) : "0";
                    String iconUrl = hit.has("icon_url") && !hit.get("icon_url").isJsonNull() ? hit.get("icon_url").getAsString() : "";
                    String author = hit.has("author") ? hit.get("author").getAsString() : "Community";

                    results.add(new ModResult(title, description, projectId, downloads, iconUrl, author, projectType));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * Finds the best recommended version of the mod matching the active profile's Minecraft version
     * and downloads the JAR into the profile's mods folder.
     */
    public static void installModAsync(ModResult item, String mcVersion, Consumer<Boolean> callback) {
        new Thread(() -> {
            try {
                Profile activeProfile = ProfileManager.getInstance().getActiveProfile();
                if (activeProfile == null) {
                    callback.accept(false);
                    return;
                }

                File gameDir = new File(activeProfile.getGameDir());
                File targetDir;
                switch (item.projectType) {
                    case RESOURCEPACKS:
                        targetDir = new File(gameDir, "resourcepacks");
                        break;
                    case SHADERS:
                        targetDir = new File(gameDir, "shaderpacks");
                        break;
                    case DATAPACKS:
                        targetDir = new File(gameDir, "datapacks");
                        break;
                    default:
                        targetDir = new File(gameDir, "mods");
                        break;
                }
                if (!targetDir.exists()) targetDir.mkdirs();

                // 1. Try querying specific version
                String versionsUrl = "https://api.modrinth.com/v2/project/" + item.projectId + "/version";
                URL url = new URL(versionsUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "IceClientLauncher/1.0.0");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                if (conn.getResponseCode() == 200) {
                    JsonArray versions = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonArray();
                    JsonObject bestVersion = null;

                    // Find version matching exact mcVersion or 1.21 branch
                    for (JsonElement vEl : versions) {
                        JsonObject vObj = vEl.getAsJsonObject();
                        JsonArray gameVersions = vObj.getAsJsonArray("game_versions");
                        for (JsonElement gv : gameVersions) {
                            String gvStr = gv.getAsString();
                            if (gvStr.equals(mcVersion) || (mcVersion.startsWith("1.21") && gvStr.startsWith("1.21"))) {
                                bestVersion = vObj;
                                break;
                            }
                        }
                        if (bestVersion != null) break;
                    }

                    // Fallback to most recent version if no exact version string match
                    if (bestVersion == null && versions.size() > 0) {
                        bestVersion = versions.get(0).getAsJsonObject();
                    }

                    if (bestVersion != null) {
                        JsonArray files = bestVersion.getAsJsonArray("files");
                        if (files.size() > 0) {
                            JsonObject fileObj = files.get(0).getAsJsonObject();
                            String downloadUrl = fileObj.get("url").getAsString();
                            String filename = fileObj.get("filename").getAsString();

                            File destFile = new File(targetDir, filename);

                            // Download file
                            URL downloadHttp = new URL(downloadUrl);
                            try (InputStream in = downloadHttp.openStream()) {
                                Files.copy(in, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }

                            item.isInstalled = true;
                            callback.accept(true);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            callback.accept(false);
        }).start();
    }
}