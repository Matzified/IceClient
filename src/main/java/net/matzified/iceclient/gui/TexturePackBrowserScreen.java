package net.matzified.iceclient.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 🧊 In-Game Resource Pack / Texture Pack Browser (inspired by Resourcify).
 * Allows players to search, download, and install resource packs directly inside Minecraft.
 */
public class TexturePackBrowserScreen extends Screen {

    private final Screen parent;
    private TextFieldWidget searchField;
    private final List<PackEntry> packs = new ArrayList<>();
    private boolean loading = false;
    private String statusMessage = "Search for texture packs (e.g. Bare Bones, Faithful, Fullbright)...";
    private int scrollOffset = 0;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public static class PackEntry {
        public String id;
        public String title;
        public String description;
        public String author;
        public int downloads;
        public String iconUrl;
        public String status = "Install";
        public boolean downloading = false;
    }

    public TexturePackBrowserScreen(Screen parent) {
        super(Text.literal("Texture Pack Browser"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        int cx = width / 2;

        // Search Field
        searchField = new TextFieldWidget(textRenderer, cx - 200, 42, 320, 24, Text.literal("Search packs..."));
        searchField.setMaxLength(64);
        searchField.setChangedListener(text -> searchPacks(text));
        addSelectableChild(searchField);

        // Search Button
        addDrawableChild(ButtonWidget.builder(Text.literal("🔍 Search"), btn -> searchPacks(searchField.getText()))
                .dimensions(cx + 126, 42, 74, 24).build());

        // Open Resource Pack Folder Button
        addDrawableChild(ButtonWidget.builder(Text.literal("📂 Open Folder"), btn -> {
            try {
                File dir = new File(client.runDirectory, "resourcepacks");
                dir.mkdirs();
                java.awt.Desktop.getDesktop().open(dir);
            } catch (Exception ignored) {}
        }).dimensions(cx - 200, height - 34, 110, 24).build());

        // Back / Close Button
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), btn -> {
            if (client != null) client.setScreen(parent);
        }).dimensions(cx + 90, height - 34, 110, 24).build());

        // Initial popular search
        searchPacks("pvp");
    }

    private void searchPacks(String query) {
        loading = true;
        statusMessage = "Fetching resource packs from Modrinth...";
        packs.clear();

        new Thread(() -> {
            try {
                String q = query != null ? query.trim() : "";
                String encodedQ = URLEncoder.encode(q, StandardCharsets.UTF_8);
                String url = "https://api.modrinth.com/v2/search?query=" + encodedQ + "&facets=[[\"project_type:resourcepack\"]]&limit=15";

                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", "IceClient/1.0").GET().build();
                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200) {
                    JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
                    JsonArray hits = root.getAsJsonArray("hits");

                    List<PackEntry> loaded = new ArrayList<>();
                    for (JsonElement el : hits) {
                        JsonObject obj = el.getAsJsonObject();
                        PackEntry p = new PackEntry();
                        p.id = obj.get("project_id").getAsString();
                        p.title = obj.get("title").getAsString();
                        p.description = obj.has("description") ? obj.get("description").getAsString() : "";
                        p.author = obj.has("author") ? obj.get("author").getAsString() : "Modrinth";
                        p.downloads = obj.has("downloads") ? obj.get("downloads").getAsInt() : 0;
                        p.iconUrl = obj.has("icon_url") && !obj.get("icon_url").isJsonNull() ? obj.get("icon_url").getAsString() : null;
                        loaded.add(p);
                    }

                    MinecraftClient.getInstance().execute(() -> {
                        packs.addAll(loaded);
                        loading = false;
                        statusMessage = loaded.isEmpty() ? "No texture packs found." : "Showing " + loaded.size() + " texture packs";
                    });
                }
            } catch (Exception ex) {
                MinecraftClient.getInstance().execute(() -> {
                    loading = false;
                    statusMessage = "Error loading packs: " + ex.getMessage();
                });
            }
        }).start();
    }

    private void downloadPack(PackEntry pack) {
        pack.downloading = true;
        pack.status = "Downloading...";

        new Thread(() -> {
            try {
                String verUrl = "https://api.modrinth.com/v2/project/" + pack.id + "/version";
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(verUrl)).header("User-Agent", "IceClient/1.0").GET().build();
                HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200) {
                    JsonArray versions = JsonParser.parseString(resp.body()).getAsJsonArray();
                    if (!versions.isEmpty()) {
                        JsonArray files = versions.get(0).getAsJsonObject().getAsJsonArray("files");
                        if (!files.isEmpty()) {
                            JsonObject f = files.get(0).getAsJsonObject();
                            String downloadUrl = f.get("url").getAsString();
                            String filename = f.get("filename").getAsString();

                            File rpDir = new File(client.runDirectory, "resourcepacks");
                            rpDir.mkdirs();
                            File destFile = new File(rpDir, filename);

                            HttpRequest dlReq = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).header("User-Agent", "IceClient/1.0").GET().build();
                            HttpResponse<InputStream> dlResp = HTTP.send(dlReq, HttpResponse.BodyHandlers.ofInputStream());

                            try (InputStream in = dlResp.body(); FileOutputStream out = new FileOutputStream(destFile)) {
                                byte[] buf = new byte[8192];
                                int r;
                                while ((r = in.read(buf)) != -1) {
                                    out.write(buf, 0, r);
                                }
                            }

                            MinecraftClient.getInstance().execute(() -> {
                                pack.downloading = false;
                                pack.status = "Installed ✓";
                                if (client.getResourcePackManager() != null) {
                                    client.getResourcePackManager().scanPacks();
                                }
                            });
                            return;
                        }
                    }
                }
                MinecraftClient.getInstance().execute(() -> {
                    pack.downloading = false;
                    pack.status = "Failed";
                });
            } catch (Exception e) {
                MinecraftClient.getInstance().execute(() -> {
                    pack.downloading = false;
                    pack.status = "Error";
                });
            }
        }).start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark translucent background
        context.fill(0, 0, width, height, 0xDD0D111A);

        int cx = width / 2;

        // Top Header
        context.drawTextWithShadow(textRenderer, "🎨 IN-GAME TEXTURE PACK BROWSER", cx - 120, 16, 0xFF38BDF8);
        searchField.render(context, mouseX, mouseY, delta);

        // Status / Loading text
        context.drawTextWithShadow(textRenderer, statusMessage, cx - 200, 72, 0xFF94A3B8);

        // Pack Cards View
        int cardY = 88;
        int cardW = 400;
        int cardH = 50;
        int startX = cx - (cardW / 2);

        for (int i = 0; i < packs.size(); i++) {
            if (cardY + cardH > height - 42) break;

            PackEntry p = packs.get(i);
            boolean isHover = mouseX >= startX && mouseX <= startX + cardW && mouseY >= cardY && mouseY <= cardY + cardH;

            // Card background
            context.fill(startX, cardY, startX + cardW, cardY + cardH, isHover ? 0xEE1E293B : 0xEE141C2C);
            context.fill(startX, cardY, startX + cardW, cardY + 1, isHover ? 0xFF38BDF8 : 0x4438BDF8);
            context.fill(startX, cardY + cardH - 1, startX + cardW, cardY + cardH, 0x4438BDF8);
            context.fill(startX, cardY, startX + 1, cardY + cardH, 0x4438BDF8);
            context.fill(startX + cardW - 1, cardY, startX + cardW, cardY + cardH, 0x4438BDF8);

            // Title & Author
            String title = p.title != null && p.title.length() > 28 ? p.title.substring(0, 25) + "..." : p.title;
            context.drawTextWithShadow(textRenderer, "🎨 " + title, startX + 10, cardY + 8, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "by " + p.author + " • " + p.downloads + " downloads", startX + 10, cardY + 22, 0xFF94A3B8);

            String desc = p.description != null && p.description.length() > 40 ? p.description.substring(0, 37) + "..." : p.description;
            context.drawTextWithShadow(textRenderer, desc, startX + 10, cardY + 34, 0xFF64748B);

            // Install Button
            int btnW = 84;
            int btnH = 24;
            int btnX = startX + cardW - btnW - 10;
            int btnY = cardY + 13;

            boolean isBtnHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            int btnColor = p.status.contains("✓") ? 0xFF10B981 : (p.downloading ? 0xFF0284C7 : (isBtnHover ? 0xFF0EA5E9 : 0xFF0369A1));
            context.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnColor);
            context.drawTextWithShadow(textRenderer, p.status, btnX + 8, btnY + 8, 0xFFFFFFFF);

            cardY += cardH + 6;
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int cx = width / 2;
            int cardY = 88;
            int cardW = 400;
            int cardH = 50;
            int startX = cx - (cardW / 2);

            for (int i = 0; i < packs.size(); i++) {
                if (cardY + cardH > height - 42) break;
                PackEntry p = packs.get(i);
                int btnW = 84;
                int btnH = 24;
                int btnX = startX + cardW - btnW - 10;
                int btnY = cardY + 13;

                if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                    if (!p.downloading && !p.status.contains("✓")) {
                        downloadPack(p);
                        return true;
                    }
                }
                cardY += cardH + 6;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (client != null) client.setScreen(parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
