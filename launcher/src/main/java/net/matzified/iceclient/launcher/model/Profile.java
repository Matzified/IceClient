package net.matzified.iceclient.launcher.model;

import java.io.File;

public class Profile {
    private String id;
    private String name;
    private String description;
    private String mcVersion;
    private String loaderVersion;
    private int ramGb;
    private String jvmArgs;
    private String gameDir;
    private String modDir;
    private String resourcePacksDir;
    private String shaderPacksDir;
    private String dataPacksDir;
    private String icon;
    private String bannerColor;
    private long lastUsed;

    public Profile() {}

    public Profile(String id, String name, String description, String mcVersion, String loaderVersion, int ramGb, String jvmArgs, String gameDir, String icon, String bannerColor) {
        this.id = id;
        this.name = name;
        this.description = description != null ? description : "Custom Ice Client Profile";
        this.mcVersion = mcVersion;
        this.loaderVersion = loaderVersion;
        this.ramGb = ramGb;
        this.jvmArgs = jvmArgs;
        this.gameDir = gameDir;
        this.modDir = new File(gameDir, "mods").getAbsolutePath();
        this.resourcePacksDir = new File(gameDir, "resourcepacks").getAbsolutePath();
        this.shaderPacksDir = new File(gameDir, "shaderpacks").getAbsolutePath();
        this.dataPacksDir = new File(gameDir, "datapacks").getAbsolutePath();
        this.icon = icon != null ? icon : "🧊";
        this.bannerColor = bannerColor != null ? bannerColor : "#0284C7";
        this.lastUsed = System.currentTimeMillis();

        ensureDirectories();
    }

    public void ensureDirectories() {
        if (gameDir != null) {
            new File(gameDir).mkdirs();
            new File(getModDir()).mkdirs();
            new File(getResourcePacksDir()).mkdirs();
            new File(getShaderPacksDir()).mkdirs();
            new File(getDataPacksDir()).mkdirs();
            new File(gameDir, "config").mkdirs();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMcVersion() { return mcVersion; }
    public void setMcVersion(String mcVersion) { this.mcVersion = mcVersion; }

    public String getLoaderVersion() { return loaderVersion; }
    public void setLoaderVersion(String loaderVersion) { this.loaderVersion = loaderVersion; }

    public int getRamGb() { return ramGb; }
    public void setRamGb(int ramGb) { this.ramGb = ramGb; }

    public String getJvmArgs() { return jvmArgs; }
    public void setJvmArgs(String jvmArgs) { this.jvmArgs = jvmArgs; }

    public String getGameDir() { return gameDir; }
    public void setGameDir(String gameDir) { this.gameDir = gameDir; ensureDirectories(); }

    public String getModDir() {
        return modDir != null ? modDir : new File(gameDir, "mods").getAbsolutePath();
    }

    public File getModsDir() {
        return new File(getModDir());
    }

    public String getResourcePacksDir() {
        return resourcePacksDir != null ? resourcePacksDir : new File(gameDir, "resourcepacks").getAbsolutePath();
    }

    public String getShaderPacksDir() {
        return shaderPacksDir != null ? shaderPacksDir : new File(gameDir, "shaderpacks").getAbsolutePath();
    }

    public String getDataPacksDir() {
        return dataPacksDir != null ? dataPacksDir : new File(gameDir, "datapacks").getAbsolutePath();
    }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getBannerColor() { return bannerColor; }
    public void setBannerColor(String bannerColor) { this.bannerColor = bannerColor; }

    public long getLastUsed() { return lastUsed; }
    public void setLastUsed(long lastUsed) { this.lastUsed = lastUsed; }

    @Override
    public String toString() {
        return (icon != null ? icon + " " : "") + name + " (" + mcVersion + ")";
    }
}
