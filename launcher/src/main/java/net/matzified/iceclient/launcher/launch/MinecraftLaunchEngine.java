package net.matzified.iceclient.launcher.launch;

import com.google.gson.*;
import net.matzified.iceclient.launcher.auth.Account;
import net.matzified.iceclient.launcher.model.Profile;
import net.matzified.iceclient.launcher.profile.ProfileManager;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.*;

/**
 * High-Performance Minecraft + Fabric Launch Engine.
 * Downloads client.jar, libraries, assets (with multi-threaded 32-worker pool),
 * extracts Windows natives, auto-installs Ice Client optimization mods,
 * and launches the game process with live stdout/stderr piping.
 */
public class MinecraftLaunchEngine {

    private static final String VERSION_MANIFEST =
            "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String FABRIC_META =
            "https://meta.fabricmc.net/v2/versions/loader";
    private static final String ASSETS_BASE =
            "https://resources.download.minecraft.net/";

    private final HttpClient http;
    private final File rootDir;
    private Consumer<String> statusCallback;
    private Consumer<Integer> progressCallback;
    private volatile boolean cancelled = false;

    // Core 1000 FPS Ice Optimized Performance Suite — projectId → display name
    private static final String[][] ICE_MODS = {
        {"AANobbMI", "Sodium"},                  // Next-gen Vulkan-style quad chunk rendering
        {"gvQqBUqZ", "Lithium"},                 // Physics, AI, & tick loop calculation accelerator
        {"P7dR8mSH", "Fabric API"},              // Core Fabric modding framework
        {"uXXizFIs", "FerriteCore"},             // Cuts Minecraft memory usage in half, 0 GC lag
        {"NNAgCjsB", "Entity Culling"},          // Async raytraced frustum entity & tile culling
        {"5ZwdcRci", "ImmediatelyFast"},         // GPU batch buffer pass for HUD, text, chat, GUI
        {"Xvi2z84A", "Enhanced Block Entities"}, // Converts tile entities (chests/signs) to static meshes
        {"nmDcB62a", "ModernFix"},               // Memory leak fixer, world loading optimizer
        {"fQEb0iXm", "Krypton"},                 // Low-overhead network pipeline & packet optimizer
        {"YL57xq9U", "Iris Shaders"}             // Hardware accelerated shader engine
    };

    public MinecraftLaunchEngine() {
        this.rootDir = ProfileManager.getInstance().getRootDir();
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
    }

    public void setStatusCallback(Consumer<String> cb) { this.statusCallback = cb; }
    public void setProgressCallback(Consumer<Integer> cb) { this.progressCallback = cb; }
    public void cancel() { this.cancelled = true; }
    public boolean isCancelled() { return cancelled; }

    // =====================================================================
    //  Main Entry Point
    // =====================================================================

    public Process launch(Profile profile, Account account) throws Exception {
        cancelled = false;
        String mcVersion = profile.getMcVersion();
        int ramGb = profile.getRamGb();

        File versionsDir  = new File(rootDir, "versions");
        File librariesDir = new File(rootDir, "libraries");
        File assetsDir    = new File(rootDir, "assets");
        versionsDir.mkdirs();
        librariesDir.mkdirs();
        assetsDir.mkdirs();

        // ---- Step 1: Mojang version manifest --------------------------------
        status("Resolving Minecraft " + mcVersion + " version manifest...");
        progress(5);
        JsonObject versionJson = resolveMinecraftVersionJson(mcVersion, versionsDir);
        if (cancelled) return null;

        // ---- Step 2: Fabric Loader profile JSON -----------------------------
        status("Resolving Fabric loader for Minecraft " + mcVersion + "...");
        progress(12);
        JsonObject fabricJson = resolveFabricProfileJson(mcVersion, versionsDir);
        if (cancelled) return null;

        // ---- Step 3: Download client.jar ------------------------------------
        status("Verifying Minecraft client.jar...");
        progress(18);
        File clientJar = downloadClientJar(mcVersion, versionJson, versionsDir);
        if (cancelled) return null;

        // ---- Step 4: Parallel Assets Download (32 concurrent workers) --------
        status("Synchronizing assets and sound indexes...");
        progress(25);
        String assetIndexId = downloadAssetsParallel(versionJson, assetsDir);
        if (cancelled) return null;

        // ---- Step 5: Parallel Libraries Download (16 concurrent workers) -----
        status("Verifying game libraries & Fabric dependencies...");
        progress(60);
        List<File> classpath = downloadLibrariesParallel(versionJson, fabricJson, librariesDir);
        classpath.add(0, clientJar);
        if (cancelled) return null;

        // ---- Step 6: Native Libraries Extraction ----------------------------
        status("Extracting Windows native libraries...");
        progress(78);
        File nativesDir = extractNatives(versionJson, librariesDir, mcVersion, versionsDir);
        if (cancelled) return null;

        // ---- Step 7: Ice Client optimization mods ---------------------------
        status("Verifying Ice Client optimization mods...");
        progress(88);
        ensureOptimizationMods(profile, mcVersion);
        if (cancelled) return null;

        // ---- Step 8: Build Command & Launch ---------------------------------
        status("Starting Minecraft instance...");
        progress(95);
        String mainClass = (fabricJson != null && fabricJson.has("mainClass"))
                ? fabricJson.get("mainClass").getAsString()
                : (versionJson.has("mainClass") ? versionJson.get("mainClass").getAsString() : "net.fabricmc.loader.impl.launch.knot.KnotClient");

        return buildAndLaunch(profile, account, classpath, nativesDir,
                              assetsDir, assetIndexId, mainClass, ramGb);
    }

    // =====================================================================
    //  Step 1 — Mojang Version Manifest & Version JSON
    // =====================================================================

    private JsonObject resolveMinecraftVersionJson(String mcVersion, File versionsDir) throws Exception {
        File cacheFile = new File(versionsDir, mcVersion + "/" + mcVersion + ".json");
        if (cacheFile.exists() && cacheFile.length() > 500) {
            try (FileReader r = new FileReader(cacheFile)) {
                return JsonParser.parseReader(r).getAsJsonObject();
            }
        }

        String manifestBody = httpGet(VERSION_MANIFEST);
        JsonObject manifest = JsonParser.parseString(manifestBody).getAsJsonObject();
        JsonArray versions  = manifest.getAsJsonArray("versions");

        String versionUrl = null;
        for (JsonElement el : versions) {
            JsonObject v = el.getAsJsonObject();
            if (v.get("id").getAsString().equals(mcVersion)) {
                versionUrl = v.get("url").getAsString();
                break;
            }
        }

        // Fallback for custom version numbers (e.g. 1.21.5 - 1.21.11) to latest 1.21 release
        if (versionUrl == null && mcVersion.startsWith("1.21")) {
            for (JsonElement el : versions) {
                JsonObject v = el.getAsJsonObject();
                String id = v.get("id").getAsString();
                if (id.startsWith("1.21") && "release".equals(v.get("type").getAsString())) {
                    versionUrl = v.get("url").getAsString();
                    status("Mapping " + mcVersion + " to official release " + id + "...");
                    break;
                }
            }
        }

        if (versionUrl == null) {
            throw new IOException("Minecraft " + mcVersion + " not found in Mojang version manifest.");
        }

        String versionBody  = httpGet(versionUrl);
        JsonObject versionJson = JsonParser.parseString(versionBody).getAsJsonObject();

        cacheFile.getParentFile().mkdirs();
        Files.writeString(cacheFile.toPath(), versionBody);
        return versionJson;
    }

    // =====================================================================
    //  Step 2 — Fabric Profile JSON
    // =====================================================================

    private JsonObject resolveFabricProfileJson(String mcVersion, File versionsDir) {
        try {
            String loadersBody = httpGet(FABRIC_META + "/" + mcVersion);
            JsonArray loaders  = JsonParser.parseString(loadersBody).getAsJsonArray();
            if (loaders.isEmpty()) return null;

            String loaderVersion = null;
            for (JsonElement el : loaders) {
                JsonObject loader = el.getAsJsonObject().getAsJsonObject("loader");
                if (loader.has("stable") && loader.get("stable").getAsBoolean()) {
                    loaderVersion = loader.get("version").getAsString();
                    break;
                }
            }
            if (loaderVersion == null) {
                loaderVersion = loaders.get(0).getAsJsonObject()
                        .getAsJsonObject("loader").get("version").getAsString();
            }

            String profileId   = "fabric-loader-" + loaderVersion + "-" + mcVersion;
            File   cacheFile   = new File(versionsDir, profileId + "/" + profileId + ".json");

            if (cacheFile.exists() && cacheFile.length() > 500) {
                try (FileReader r = new FileReader(cacheFile)) {
                    return JsonParser.parseReader(r).getAsJsonObject();
                }
            }

            String profileUrl  = FABRIC_META + "/" + mcVersion + "/" + loaderVersion + "/profile/json";
            String profileBody = httpGet(profileUrl);
            JsonObject fabricJson = JsonParser.parseString(profileBody).getAsJsonObject();

            cacheFile.getParentFile().mkdirs();
            Files.writeString(cacheFile.toPath(), profileBody);
            return fabricJson;
        } catch (Exception e) {
            System.err.println("Notice: Could not load Fabric profile (" + e.getMessage() + "), using standard launch.");
            return null;
        }
    }

    // =====================================================================
    //  Step 3 — Client JAR
    // =====================================================================

    private File downloadClientJar(String mcVersion, JsonObject versionJson, File versionsDir) throws Exception {
        File clientJar = new File(versionsDir, mcVersion + "/" + mcVersion + ".jar");
        if (clientJar.exists() && clientJar.length() > 100_000) return clientJar;

        JsonObject downloads = versionJson.getAsJsonObject("downloads");
        JsonObject client    = downloads.getAsJsonObject("client");
        String url           = client.get("url").getAsString();

        clientJar.getParentFile().mkdirs();
        downloadFile(url, clientJar);
        return clientJar;
    }

    // =====================================================================
    //  Step 4 — Parallel Assets Download (32 Threads)
    // =====================================================================

    private String downloadAssetsParallel(JsonObject versionJson, File assetsDir) throws Exception {
        JsonObject assetIndex   = versionJson.getAsJsonObject("assetIndex");
        String     assetIndexId = assetIndex.get("id").getAsString();
        String     indexUrl     = assetIndex.get("url").getAsString();

        File indexDir  = new File(assetsDir, "indexes");
        indexDir.mkdirs();
        File indexFile = new File(indexDir, assetIndexId + ".json");

        String indexBody;
        if (!indexFile.exists() || indexFile.length() < 100) {
            indexBody = httpGet(indexUrl);
            Files.writeString(indexFile.toPath(), indexBody);
        } else {
            indexBody = Files.readString(indexFile.toPath());
        }

        File objectsDir = new File(assetsDir, "objects");
        objectsDir.mkdirs();

        // Local fast-copy from existing .minecraft if available
        File mcAssets = null;
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            File mcDir = new File(appData, ".minecraft/assets/objects");
            if (mcDir.isDirectory()) mcAssets = mcDir;
        }

        JsonObject objects = JsonParser.parseString(indexBody)
                .getAsJsonObject().getAsJsonObject("objects");

        int total = objects.size();
        AtomicInteger done = new AtomicInteger(0);
        List<Map.Entry<String, JsonElement>> missingEntries = new ArrayList<>();

        for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
            String hash   = entry.getValue().getAsJsonObject().get("hash").getAsString();
            String prefix = hash.substring(0, 2);
            File   dest   = new File(objectsDir, prefix + "/" + hash);

            if (dest.exists() && dest.length() > 0) {
                done.incrementAndGet();
                continue;
            }

            // Check if local .minecraft has it
            if (mcAssets != null) {
                File localSrc = new File(mcAssets, prefix + "/" + hash);
                if (localSrc.exists() && localSrc.length() > 0) {
                    dest.getParentFile().mkdirs();
                    try {
                        Files.copy(localSrc.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        done.incrementAndGet();
                        continue;
                    } catch (Exception ignored) {}
                }
            }

            missingEntries.add(entry);
        }

        if (!missingEntries.isEmpty() && !cancelled) {
            int missingTotal = missingEntries.size();
            status("Downloading " + missingTotal + " Minecraft assets in parallel...");

            ExecutorService pool = Executors.newFixedThreadPool(32);
            List<Future<?>> futures = new ArrayList<>();

            for (Map.Entry<String, JsonElement> entry : missingEntries) {
                futures.add(pool.submit(() -> {
                    if (cancelled) return;
                    String hash   = entry.getValue().getAsJsonObject().get("hash").getAsString();
                    String prefix = hash.substring(0, 2);
                    File   dest   = new File(objectsDir, prefix + "/" + hash);

                    dest.getParentFile().mkdirs();
                    try {
                        downloadFile(ASSETS_BASE + prefix + "/" + hash, dest);
                    } catch (Exception ignored) {}

                    int d = done.incrementAndGet();
                    if (d % 100 == 0 || d == total) {
                        int pct = 25 + (int)(30.0 * d / total);
                        progress(pct);
                        status("Loading assets... (" + d + "/" + total + ")");
                    }
                }));
            }

            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.MINUTES);
        }

        return assetIndexId;
    }

    // =====================================================================
    //  Step 5 — Parallel Libraries Download (16 Threads)
    // =====================================================================

    private List<File> downloadLibrariesParallel(JsonObject versionJson, JsonObject fabricJson,
                                                 File librariesDir) throws Exception {
        List<File>  classpath  = Collections.synchronizedList(new ArrayList<>());
        Set<String> seen       = Collections.synchronizedSet(new HashSet<>());
        List<JsonObject> libs  = new ArrayList<>();

        if (fabricJson != null && fabricJson.has("libraries")) {
            for (JsonElement el : fabricJson.getAsJsonArray("libraries")) {
                libs.add(el.getAsJsonObject());
            }
        }

        if (versionJson.has("libraries")) {
            for (JsonElement el : versionJson.getAsJsonArray("libraries")) {
                libs.add(el.getAsJsonObject());
            }
        }

        int total = libs.size();
        AtomicInteger done = new AtomicInteger(0);

        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Future<?>> futures = new ArrayList<>();

        for (JsonObject lib : libs) {
            futures.add(pool.submit(() -> {
                if (cancelled) return;

                if (lib.has("rules") && !checkRules(lib.getAsJsonArray("rules"))) {
                    done.incrementAndGet();
                    return;
                }

                if (lib.has("downloads")) {
                    JsonObject dl = lib.getAsJsonObject("downloads");
                    if (dl.has("artifact")) {
                        JsonObject artifact = dl.getAsJsonObject("artifact");
                        String path = artifact.get("path").getAsString();
                        String url  = artifact.get("url").getAsString();
                        if (seen.add(path)) {
                            File f = downloadLibraryFile(url, path, librariesDir);
                            if (f != null) classpath.add(f);
                        }
                    }
                } else if (lib.has("name")) {
                    String name    = lib.get("name").getAsString();
                    String baseUrl = lib.has("url") ? lib.get("url").getAsString()
                                                    : "https://repo1.maven.org/maven2/";
                    String path    = mavenNameToPath(name);
                    if (seen.add(path)) {
                        String url = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + path;
                        File f = downloadLibraryFile(url, path, librariesDir);
                        if (f != null) classpath.add(f);
                    }
                }

                int d = done.incrementAndGet();
                if (d % 5 == 0 || d == total) {
                    progress(60 + (int)(18.0 * d / total));
                    status("Verifying libraries... (" + d + "/" + total + ")");
                }
            }));
        }

        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.MINUTES);

        return classpath;
    }

    private File downloadLibraryFile(String url, String path, File librariesDir) {
        File dest = new File(librariesDir, path);
        if (dest.exists() && dest.length() > 0) return dest;
        dest.getParentFile().mkdirs();
        try {
            downloadFile(url, dest);
            return dest;
        } catch (Exception e) {
            return dest.exists() ? dest : null;
        }
    }

    // =====================================================================
    //  Step 6 — Natives Extraction
    // =====================================================================

    private File extractNatives(JsonObject versionJson, File librariesDir,
                                String mcVersion, File versionsDir) throws Exception {
        File nativesDir = new File(versionsDir, mcVersion + "/natives");
        nativesDir.mkdirs();

        if (!versionJson.has("libraries")) return nativesDir;

        for (JsonElement el : versionJson.getAsJsonArray("libraries")) {
            if (cancelled) break;
            JsonObject lib = el.getAsJsonObject();
            if (!lib.has("downloads")) continue;
            JsonObject downloads = lib.getAsJsonObject("downloads");
            if (!downloads.has("classifiers")) continue;
            JsonObject classifiers = downloads.getAsJsonObject("classifiers");

            String nativeKey = "natives-windows";
            if (!classifiers.has(nativeKey)) nativeKey = "natives-windows-64";
            if (!classifiers.has(nativeKey)) continue;

            JsonObject nativeArt = classifiers.getAsJsonObject(nativeKey);
            String path = nativeArt.get("path").getAsString();
            String url  = nativeArt.get("url").getAsString();

            File nativeJar = new File(librariesDir, path);
            if (!nativeJar.exists()) {
                nativeJar.getParentFile().mkdirs();
                try { downloadFile(url, nativeJar); } catch (Exception e) { continue; }
            }

            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(nativeJar))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (entry.isDirectory() || name.contains("META-INF") || name.endsWith(".class")) {
                        zis.closeEntry();
                        continue;
                    }
                    if (name.endsWith(".dll") || name.endsWith(".so") || name.endsWith(".dylib")) {
                        File out = new File(nativesDir, new File(name).getName());
                        if (!out.exists()) {
                            try (FileOutputStream fos = new FileOutputStream(out)) {
                                byte[] buf = new byte[8192];
                                int n;
                                while ((n = zis.read(buf)) != -1) fos.write(buf, 0, n);
                            }
                        }
                    }
                    zis.closeEntry();
                }
            } catch (Exception ignored) {}
        }
        return nativesDir;
    }

    // =====================================================================
    //  Step 7 — Ice Client Optimization Mods & Tuning
    // =====================================================================

    private void ensureOptimizationMods(Profile profile, String mcVersion) {
        File modsDir = profile.getModsDir();
        modsDir.mkdirs();

        // 1. Ensure Ice Client Core Mod is deployed to this profile
        ProfileManager.getInstance().ensureIceClientModInProfile(profile);
        status("Deployed Ice Client core mod.");

        // 2. Pre-configure optimized options.txt for 1000 FPS competitive gaming
        File optionsFile = new File(profile.getGameDir(), "options.txt");
        if (!optionsFile.exists()) {
            try {
                String optimizedOptions =
                        "version:3955\n" +
                        "graphicsMode:fast\n" +
                        "renderDistance:12\n" +
                        "simulationDistance:8\n" +
                        "maxFps:0\n" +               // 0 = Unlimited FPS
                        "vsync:false\n" +
                        "mipmapLevels:0\n" +
                        "biomeBlendRadius:0\n" +
                        "particles:minimal\n" +
                        "clouds:false\n" +
                        "entityDistanceScaling:0.8\n" +
                        "entityShadows:false\n" +
                        "bobView:true\n" +
                        "gamma:1.0\n";
                Files.writeString(optionsFile.toPath(), optimizedOptions);
            } catch (Exception ignored) {}
        }

        File[] existing = modsDir.listFiles(f -> f.getName().endsWith(".jar"));
        if (existing != null && existing.length >= 3) {
            status("Ice Optimized performance suite active.");
            return;
        }

        for (String[] mod : ICE_MODS) {
            if (cancelled) break;
            status("Downloading Ice Optimized suite: " + mod[1] + "...");
            try {
                downloadModrinthMod(mod[0], mcVersion, modsDir);
            } catch (Exception e) {
                System.err.println("Notice: Skipped optional mod " + mod[1] + ": " + e.getMessage());
            }
        }
    }

    private void downloadModrinthMod(String projectId, String mcVersion, File modsDir) throws Exception {
        String ev = URLEncoder.encode("[\"" + mcVersion + "\"]", StandardCharsets.UTF_8);
        String el = URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
        String url = "https://api.modrinth.com/v2/project/" + projectId
                     + "/version?game_versions=" + ev + "&loaders=" + el;

        String body = httpGet(url);
        JsonArray versions = JsonParser.parseString(body).getAsJsonArray();
        if (versions.isEmpty()) return;

        JsonArray files = versions.get(0).getAsJsonObject().getAsJsonArray("files");
        if (files.isEmpty()) return;

        JsonObject fileObj = files.get(0).getAsJsonObject();
        for (JsonElement fe : files) {
            JsonObject f = fe.getAsJsonObject();
            if (f.has("primary") && f.get("primary").getAsBoolean()) {
                fileObj = f;
                break;
            }
        }

        String downloadUrl = fileObj.get("url").getAsString();
        String filename    = fileObj.get("filename").getAsString();
        File   dest        = new File(modsDir, filename);
        if (!dest.exists()) downloadFile(downloadUrl, dest);
    }

    // =====================================================================
    //  Step 8 — Build Command & Launch Process
    // =====================================================================

    private Process buildAndLaunch(Profile profile, Account account,
                                   List<File> classpath, File nativesDir, File assetsDir,
                                   String assetIndexId, String mainClass, int ramGb) throws Exception {
        String javaExe = resolveJavaExecutable();

        // Build classpath
        StringJoiner cp = new StringJoiner(File.pathSeparator);
        for (File f : classpath) {
            if (f != null && f.exists()) cp.add(f.getAbsolutePath());
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe);

        // Memory allocation
        cmd.add("-Xmx" + ramGb + "G");
        cmd.add("-Xms" + Math.max(1, ramGb / 2) + "G");

        // Custom JVM args from profile
        if (profile.getJvmArgs() != null && !profile.getJvmArgs().isBlank()) {
            for (String arg : profile.getJvmArgs().split("\\s+")) {
                if (!arg.isBlank()) cmd.add(arg);
            }
        }

        // Native libraries & system properties
        cmd.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
        cmd.add("-Dfile.encoding=UTF-8");
        cmd.add("-Djna.tmpdir=" + nativesDir.getAbsolutePath());
        cmd.add("-Dio.netty.native.workdir=" + nativesDir.getAbsolutePath());
        cmd.add("-Diceclient.brand=IceOptimized");
        cmd.add("-Diceclient.version=1.0.0");

        // VulkanMod low-overhead graphics compatibility
        File modsFolder = profile.getModsDir();
        boolean hasVulkan = modsFolder.exists() && modsFolder.listFiles((d, name) -> name.toLowerCase().contains("vulkan")) != null && modsFolder.listFiles((d, name) -> name.toLowerCase().contains("vulkan")).length > 0;
        if (hasVulkan) {
            cmd.add("-Dorg.lwjgl.util.NoChecks=true");
            cmd.add("-Dorg.lwjgl.opengl.disableChecks=true");
            cmd.add("-Diceclient.vulkan=true");
        }

        // Classpath
        cmd.add("-cp");
        cmd.add(cp.toString());

        // Main class
        cmd.add(mainClass);

        // Minecraft game arguments
        String username    = account != null ? account.getUsername()    : "IcePlayer";
        String uuid        = account != null ? account.getUuid()        : "00000000-0000-0000-0000-000000000000";
        String accessToken = account != null ? account.getAccessToken() : "offline";

        String[] gameArgs = {
            "--username",    username,
            "--uuid",        uuid,
            "--accessToken", accessToken,
            "--version",     "Ice Optimized " + profile.getMcVersion(),
            "--gameDir",     profile.getGameDir(),
            "--assetsDir",   assetsDir.getAbsolutePath(),
            "--assetIndex",  assetIndexId,
            "--userType",    "msa",
            "--versionType", "Ice Client"
        };
        Collections.addAll(cmd, gameArgs);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(profile.getGameDir()));
        pb.redirectErrorStream(true);

        progress(100);
        status("Ice Optimized Minecraft is running!");

        Process proc = pb.start();

        // Background thread to log Minecraft output live
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Minecraft] " + line);
                }
            } catch (Exception ignored) {}
        }, "minecraft-stdout-reader").start();

        return proc;
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    private String resolveJavaExecutable() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            File exe = new File(javaHome, "bin/java.exe");
            if (exe.exists()) return exe.getAbsolutePath();
            exe = new File(javaHome, "bin/java");
            if (exe.exists()) return exe.getAbsolutePath();
        }
        return "java";
    }

    private boolean checkRules(JsonArray rules) {
        boolean allow = false;
        for (JsonElement el : rules) {
            JsonObject rule   = el.getAsJsonObject();
            String     action = rule.get("action").getAsString();
            if (rule.has("os")) {
                JsonObject os     = rule.getAsJsonObject("os");
                String     osName = os.has("name") ? os.get("name").getAsString() : "";
                boolean isWin  = System.getProperty("os.name", "").toLowerCase().contains("win");
                boolean isMac  = System.getProperty("os.name", "").toLowerCase().contains("mac");
                boolean isLin  = !isWin && !isMac;
                boolean match  = (osName.equals("windows") && isWin)
                              || (osName.equals("osx")     && isMac)
                              || (osName.equals("linux")   && isLin)
                              || osName.isEmpty();
                if (action.equals("allow"))    allow = match;
                if (action.equals("disallow") && match) return false;
            } else {
                if (action.equals("allow")) allow = true;
            }
        }
        return allow;
    }

    private String mavenNameToPath(String name) {
        String[] parts = name.split(":");
        if (parts.length < 3) return name.replace(":", "/") + ".jar";
        String groupPath = parts[0].replace(".", "/");
        String artifact  = parts[1];
        String version   = parts[2];
        String classifier = parts.length >= 4 ? "-" + parts[3] : "";
        return groupPath + "/" + artifact + "/" + version + "/" + artifact + "-" + version + classifier + ".jar";
    }

    private String httpGet(String urlStr) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(urlStr))
                .header("User-Agent", "IceClientLauncher/1.0.0 (contact@matzified.iceclient)")
                .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400)
            throw new IOException("HTTP " + resp.statusCode() + " for " + urlStr);
        return resp.body();
    }

    private void downloadFile(String urlStr, File target) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(urlStr))
                .header("User-Agent", "IceClientLauncher/1.0.0 (contact@matzified.iceclient)")
                .GET().build();
        http.send(req, HttpResponse.BodyHandlers.ofFile(target.toPath(),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING));
    }

    private void status(String msg) {
        if (statusCallback != null) statusCallback.accept(msg);
    }

    private void progress(int pct) {
        if (progressCallback != null) progressCallback.accept(Math.min(100, Math.max(0, pct)));
    }
}
