package net.matzified.iceclient.launcher.auth;

public class Account {
    private String id;
    private String username;
    private String uuid;
    private String accessToken;
    private String skinUrl;
    private boolean active;

    public Account() {}

    public Account(String id, String username, String uuid, String accessToken, String skinUrl, boolean active) {
        this.id = id;
        this.username = username;
        this.uuid = uuid;
        this.accessToken = accessToken;
        this.skinUrl = skinUrl;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getSkinUrl() { return skinUrl; }
    public void setSkinUrl(String skinUrl) { this.skinUrl = skinUrl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return username + (active ? " (Active)" : "");
    }
}
