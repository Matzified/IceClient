package net.matzified.iceclient.launcher.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.matzified.iceclient.launcher.model.Profile;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ProfileManager {

    private static ProfileManager instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File rootDir;
    private final File profilesFile;
    private final List<Profile> profiles = new ArrayList<>();
    private Profile activeProfile;

    public static final String[] SUPPORTED_VERSIONS = {
            "1.21.11", "1.21.10", "1.21.9", "1.21.8", "1.21.7", "1.21.6", "1.21.5", "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21",
            "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5"
    };

    public static final String DEFAULT_JVM_ARGS =
            "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=30 -XX:+UnlockExperimentalVMOptions -XX:+DisableExplicitGC -XX:+AlwaysPreTouch -XX:G1NewSizePercent=35 -XX:G1MaxNewSizePercent=45 -XX:G1ReservePercent=15 -XX:G1HeapWastePercent=5 -XX:G1MixedGCCountTarget=4 -XX:InitiatingHeapOccupancyPercent=15 -XX:G1MixedGCLiveThresholdPercent=90 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:SurvivorRatio=32 -XX:+PerfDisableSharedMem -XX:MaxTenuringThreshold=1 -Diceclient.ultrafps=true";

    private ProfileManager() {
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            rootDir = new File(appData, ".iceclient");
        } else {
            rootDir = new File(System.getProperty("user.home"), ".iceclient");
        }

        ensureRootDirStructure();
        profilesFile = new File(rootDir, "profiles.json");
        loadProfiles();
    }

    public static synchronized ProfileManager getInstance() {
        if (instance == null) {
            instance = new ProfileManager();
        }
        return instance;
    }

    private void ensureRootDirStructure() {
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
        new File(rootDir, "mods").mkdirs();
        new File(rootDir, "profiles").mkdirs();
        new File(rootDir, "versions").mkdirs();
    }

    public void loadProfiles() {
        profiles.clear();
        if (profilesFile.exists()) {
            try (FileReader reader = new FileReader(profilesFile)) {
                Type listType = new TypeToken<ArrayList<Profile>>(){}.getType();
                List<Profile> loaded = gson.fromJson(reader, listType);
                if (loaded != null && !loaded.isEmpty()) {
                    profiles.addAll(loaded);
                    for (Profile p : profiles) {
                        if ("1.21.11".equals(p.getMcVersion())) {
                            p.setMcVersion("1.21.1");
                        }
                        if (p.getName() != null && p.getName().contains("Default")) {
                            p.setName("Ice Optimized (1.21.1)");
                            p.setDescription("Custom tuned high-performance Minecraft 1.21.1 with Sodium, Lithium, FerriteCore & Ice HUD");
                        }
                        p.ensureDirectories();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (profiles.isEmpty()) {
            createDefaultProfiles();
        }

        if (activeProfile == null && !profiles.isEmpty()) {
            activeProfile = profiles.get(0);
        }
    }

    private void createDefaultProfiles() {
        File profilesDir = new File(rootDir, "profiles");

        Profile p1 = new Profile(
                "ice_optimized_1211",
                "Ice Optimized (1.21.1)",
                "Custom tuned high-performance Minecraft 1.21.1 with Sodium, Lithium, FerriteCore & Ice HUD",
                "1.21.1",
                "0.16.0",
                4,
                DEFAULT_JVM_ARGS,
                new File(profilesDir, "Ice-Optimized-1.21.1").getAbsolutePath(),
                "🧊",
                "#0284C7"
        );

        Profile p2 = new Profile(
                "ice_pvp_121",
                "Ice PvP (1.21)",
                "Vanilla Fabric 1.21 tuned for low latency competitive PvP",
                "1.21",
                "0.16.0",
                4,
                DEFAULT_JVM_ARGS,
                new File(profilesDir, "Ice-PvP-1.21").getAbsolutePath(),
                "⚡",
                "#38BDF8"
        );

        Profile p3 = new Profile(
                "ice_highperf_1214",
                "Ice Ultra (1.21.4)",
                "Minecraft 1.21.4 high performance profile with extra RAM allocation",
                "1.21.4",
                "0.16.0",
                6,
                DEFAULT_JVM_ARGS,
                new File(profilesDir, "Ice-Ultra-1.21.4").getAbsolutePath(),
                "🚀",
                "#10B981"
        );

        profiles.add(p1);
        profiles.add(p2);
        profiles.add(p3);

        activeProfile = p1;
        saveProfiles();
    }

    public void saveProfiles() {
        try (FileWriter writer = new FileWriter(profilesFile)) {
            gson.toJson(profiles, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File getRootDir() {
        return rootDir;
    }

    public List<Profile> getProfiles() {
        return profiles;
    }

    public Profile getActiveProfile() {
        return activeProfile;
    }

    public void setActiveProfile(Profile profile) {
        this.activeProfile = profile;
        if (profile != null) {
            profile.setLastUsed(System.currentTimeMillis());
            saveProfiles();
        }
    }

    public void addProfile(Profile profile) {
        profile.ensureDirectories();
        profiles.add(profile);
        setActiveProfile(profile);
    }

    public void removeProfile(Profile profile) {
        profiles.remove(profile);
        if (activeProfile == profile) {
            activeProfile = profiles.isEmpty() ? null : profiles.get(0);
        }
        saveProfiles();
    }

    public File getActiveModDirectory() {
        if (activeProfile != null && activeProfile.getModDir() != null) {
            File dir = new File(activeProfile.getModDir());
            dir.mkdirs();
            return dir;
        }
        File globalMods = new File(rootDir, "mods");
        globalMods.mkdirs();
        return globalMods;
    }
}
