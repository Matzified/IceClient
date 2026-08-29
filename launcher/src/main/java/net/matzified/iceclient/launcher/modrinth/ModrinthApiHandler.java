package net.matzified.iceclient.launcher.modrinth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.matzified.iceclient.launcher.model.Profile;
import net.matzified.iceclient.launcher.profile.ProfileManager;
import net.matzified.iceclient.launcher.utils.ModpackImporter;

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

public class ModrinthApiHandler {

    public enum ProjectType {
        MODS("mod", "Mods"),
        MODPACKS("modpack", "Modpacks"),
        RESOURCEPACKS("resourcepack", "Resource Packs"),
        SHADERS("shader", "Shaders"),
        DATAPACKS("datapack", "Data Packs");

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

            // Facets array: categories:fabric AND project_type:type AND versions:mcVersion
            String facetsStr = "[[\"project_type:" + projectType.apiType + "\"],[\"categories:fabric\"],[\"versions:" + mcVersion + "\"]]";
            String facetsParam = URLEncoder.encode(facetsStr, StandardCharsets.UTF_8);

            String urlStr = "https://api.modrinth.com/v2/search?query=" + encodedQuery + "&facets=" + facetsParam + "&limit=20";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "IceClientLauncher/1.0.0");

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

    public static boolean installModrinthAsset(ModResult item, String mcVersion, Runnable onCompleteCallback) {
        try {
            Profile activeProfile = ProfileManager.getInstance().getActiveProfile();
            if (activeProfile == null) return false;

            String versionsParam = URLEncoder.encode("[\"" + mcVersion + "\"]", StandardCharsets.UTF_8);
            String loadersParam = URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
            String versionsUrl = "https://api.modrinth.com/v2/project/" + item.projectId + "/version?game_versions=" + versionsParam + "&loaders=" + loadersParam;

            URL url = new URL(versionsUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "IceClientLauncher/1.0.0");

            if (conn.getResponseCode() == 200) {
                JsonArray versions = JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonArray();
                if (versions.size() > 0) {
                    JsonObject latestVersion = versions.get(0).getAsJsonObject();
                    JsonArray files = latestVersion.getAsJsonArray("files");

                    if (files.size() > 0) {
                        JsonObject fileObj = files.get(0).getAsJsonObject();
                        String downloadUrl = fileObj.get("url").getAsString();
                        String filename = fileObj.get("filename").getAsString();

                        File targetDir;
                        switch (item.projectType) {
                            case RESOURCEPACKS:
                                targetDir = new File(activeProfile.getResourcePacksDir());
                                break;
                            case SHADERS:
                                targetDir = new File(activeProfile.getShaderPacksDir());
                                break;
                            case DATAPACKS:
                                targetDir = new File(activeProfile.getDataPacksDir());
                                break;
                            case MODPACKS:
                                File tempFile = new File(ProfileManager.getInstance().getRootDir(), filename);
                                downloadFileFromUrl(downloadUrl, tempFile);
                                Profile importedPack = ModpackImporter.importModpackFile(tempFile, item.title);
                                if (importedPack != null) {
                                    ProfileManager.getInstance().addProfile(importedPack);
                                }
                                tempFile.delete();
                                return true;
                            case MODS:
                            default:
                                targetDir = new File(activeProfile.getModDir());
                                break;
                        }

                        targetDir.mkdirs();
                        File destination = new File(targetDir, filename);
                        downloadFileFromUrl(downloadUrl, destination);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void downloadFileFromUrl(String urlStr, File targetFile) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "IceClientLauncher/1.0.0");
        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}