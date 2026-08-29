package net.matzified.iceclient.launcher.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MinecraftAuthenticator {

    // Official Minecraft Launcher Public Client ID
    public static final String CLIENT_ID = "00000000402b5328";
    public static final String LIVE_SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";

    private final HttpClient httpClient;

    public MinecraftAuthenticator() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    // =========================================================================
    // STEP 1: Microsoft OAuth2 (Device Authorization Flow via Live.com)
    // =========================================================================

    public MicrosoftDeviceCode requestDeviceCode() throws IOException, InterruptedException {
        Map<String, String> params = Map.of(
                "client_id", CLIENT_ID,
                "scope", LIVE_SCOPE,
                "response_type", "device_code"
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.live.com/oauth20_connect.srf"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encodeFormData(params)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = parseJsonObject(response.body());

        if (response.statusCode() != 200 || json == null) {
            throw new IOException("Failed to get device code: " + response.body());
        }

        return new MicrosoftDeviceCode(
                json.get("user_code").getAsString(),
                json.get("device_code").getAsString(),
                json.has("verification_uri") ? json.get("verification_uri").getAsString() : "https://www.microsoft.com/link",
                json.get("expires_in").getAsInt(),
                json.has("interval") ? json.get("interval").getAsInt() : 5
        );
    }

    public MicrosoftTokens pollMicrosoftToken(MicrosoftDeviceCode code, Consumer<String> statusListener)
            throws IOException, InterruptedException {
        long expireTime = System.currentTimeMillis() + (code.expiresIn() * 1000L);
        int intervalMs = Math.max(code.interval(), 5) * 1000;

        while (System.currentTimeMillis() < expireTime) {
            Thread.sleep(intervalMs);

            Map<String, String> params = Map.of(
                    "client_id", CLIENT_ID,
                    "grant_type", "urn:ietf:params:oauth:grant-type:device_code",
                    "device_code", code.deviceCode()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://login.live.com/oauth20_token.srf"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(encodeFormData(params)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject json = parseJsonObject(response.body());

            if (json != null) {
                if (response.statusCode() == 200 && json.has("access_token")) {
                    return new MicrosoftTokens(
                            json.get("access_token").getAsString(),
                            json.has("refresh_token") ? json.get("refresh_token").getAsString() : null
                    );
                }

                String error = json.has("error") ? json.get("error").getAsString() : "";
                if ("authorization_pending".equalsIgnoreCase(error)) {
                    if (statusListener != null) {
                        statusListener.accept("Waiting for you to authorize at microsoft.com/link...");
                    }
                    continue;
                } else if ("slow_down".equalsIgnoreCase(error)) {
                    intervalMs += 5000;
                    continue;
                } else if (!error.isEmpty()) {
                    String desc = json.has("error_description") ? json.get("error_description").getAsString() : error;
                    throw new IOException("OAuth2 polling error: " + desc);
                }
            }

            if (response.statusCode() != 200) {
                if (statusListener != null) {
                    statusListener.accept("Waiting for approval on Microsoft (" + response.statusCode() + ")...");
                }
            }
        }

        throw new IOException("Device code authorization timed out.");
    }

    public MicrosoftTokens refreshMicrosoftToken(String refreshToken) throws IOException, InterruptedException {
        Map<String, String> params = Map.of(
                "client_id", CLIENT_ID,
                "grant_type", "refresh_token",
                "refresh_token", refreshToken,
                "scope", LIVE_SCOPE
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.live.com/oauth20_token.srf"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encodeFormData(params)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = parseJsonObject(response.body());

        if (response.statusCode() != 200 || json == null || !json.has("access_token")) {
            throw new IOException("Failed to refresh Microsoft token: " + response.body());
        }

        return new MicrosoftTokens(
                json.get("access_token").getAsString(),
                json.has("refresh_token") ? json.get("refresh_token").getAsString() : refreshToken
        );
    }

    // =========================================================================
    // STEP 2: Xbox Live Authentication (user.auth.xboxlive.com)
    // =========================================================================

    public record XboxLiveToken(String token, String uhs) {}

    public XboxLiveToken authenticateXboxLive(String msAccessToken) throws IOException, InterruptedException {
        // Try RPS ticket with d= prefix
        JsonObject properties = new JsonObject();
        properties.addProperty("AuthMethod", "RPS");
        properties.addProperty("SiteName", "user.auth.xboxlive.com");
        properties.addProperty("RpsTicket", "d=" + msAccessToken);

        JsonObject payload = new JsonObject();
        payload.add("Properties", properties);
        payload.addProperty("RelyingParty", "http://auth.xboxlive.com");
        payload.addProperty("TokenType", "JWT");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = parseJsonObject(response.body());

        if (response.statusCode() != 200 || json == null || !json.has("Token")) {
            // Fallback try with t= prefix
            properties.addProperty("RpsTicket", "t=" + msAccessToken);
            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            json = parseJsonObject(response.body());

            if (response.statusCode() != 200 || json == null || !json.has("Token")) {
                // Fallback try raw access token
                properties.addProperty("RpsTicket", msAccessToken);
                request = HttpRequest.newBuilder()
                        .uri(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                json = parseJsonObject(response.body());

                if (response.statusCode() != 200 || json == null || !json.has("Token")) {
                    throw new IOException("Xbox Live authentication failed: " + response.body());
                }
            }
        }

        String xblToken = json.get("Token").getAsString();
        String uhs = json.getAsJsonObject("DisplayClaims")
                .getAsJsonArray("xui")
                .get(0).getAsJsonObject()
                .get("uhs").getAsString();

        return new XboxLiveToken(xblToken, uhs);
    }

    // =========================================================================
    // STEP 3: XSTS Authorization (xsts.auth.xboxlive.com)
    // =========================================================================

    public record XstsToken(String token, String uhs) {}

    public XstsToken authorizeXSTS(XboxLiveToken xboxToken) throws IOException, InterruptedException, XSTSErrorException {
        JsonObject properties = new JsonObject();
        properties.addProperty("SandboxId", "RETAIL");
        JsonArray userTokens = new JsonArray();
        userTokens.add(xboxToken.token());
        properties.add("UserTokens", userTokens);

        JsonObject payload = new JsonObject();
        payload.add("Properties", properties);
        payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        payload.addProperty("TokenType", "JWT");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = parseJsonObject(response.body());

        if (response.statusCode() != 200 || json == null || !json.has("Token")) {
            if (json != null && json.has("XErr")) {
                long xerr = json.get("XErr").getAsLong();
                throw new XSTSErrorException(xerr);
            }
            throw new IOException("XSTS authorization failed: " + response.body());
        }

        String xstsToken = json.get("Token").getAsString();
        String uhs = json.getAsJsonObject("DisplayClaims")
                .getAsJsonArray("xui")
                .get(0).getAsJsonObject()
                .get("uhs").getAsString();

        return new XstsToken(xstsToken, uhs);
    }

    // =========================================================================
    // STEP 4: Minecraft Services Authentication (api.minecraftservices.com)
    // =========================================================================

    public String authenticateMinecraftServices(XstsToken xsts) throws IOException, InterruptedException {
        JsonObject payload = new JsonObject();
        payload.addProperty("identityToken", "XBL3.0 x=" + xsts.uhs() + ";" + xsts.token());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = parseJsonObject(response.body());

        if (response.statusCode() != 200 || json == null || !json.has("access_token")) {
            throw new IOException("Minecraft authentication failed: " + response.body());
        }

        return json.get("access_token").getAsString();
    }

    // =========================================================================
    // STEP 5: Minecraft Profile & Ownership Fetch
    // =========================================================================

    public MinecraftSession fetchMinecraftProfile(String mcAccessToken, String refreshToken)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("Authorization", "Bearer " + mcAccessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject json = parseJsonObject(response.body());

        if (response.statusCode() != 200 || json == null) {
            if (response.statusCode() == 404) {
                throw new IOException("Account does not own Minecraft Java Edition or has not set a username yet.");
            }
            throw new IOException("Failed to fetch Minecraft profile: " + response.body());
        }

        String rawUuid = json.get("id").getAsString();
        String username = json.get("name").getAsString();
        UUID uuid = parseUndashedUuid(rawUuid);

        return new MinecraftSession(username, uuid, mcAccessToken, refreshToken);
    }

    // =========================================================================
    // Pipeline Orchestrator
    // =========================================================================

    public MinecraftSession authenticateWithMicrosoftToken(String msAccessToken, String refreshToken)
            throws IOException, InterruptedException, XSTSErrorException {
        // Step 2: Xbox Live
        XboxLiveToken xbl = authenticateXboxLive(msAccessToken);

        // Step 3: XSTS
        XstsToken xsts = authorizeXSTS(xbl);

        // Step 4: Minecraft Services
        String mcAccessToken = authenticateMinecraftServices(xsts);

        // Step 5: Profile Fetch
        return fetchMinecraftProfile(mcAccessToken, refreshToken);
    }

    public record MicrosoftTokens(String accessToken, String refreshToken) {}

    public static JsonObject parseJsonObject(String body) {
        if (body == null) return null;
        String trimmed = body.trim();
        if (trimmed.isEmpty()) return null;
        try {
            JsonElement elem = JsonParser.parseString(trimmed);
            if (elem.isJsonObject()) {
                return elem.getAsJsonObject();
            } else if (elem.isJsonArray()) {
                JsonArray arr = elem.getAsJsonArray();
                if (!arr.isEmpty() && arr.get(0).isJsonObject()) {
                    return arr.get(0).getAsJsonObject();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String encodeFormData(Map<String, String> data) {
        return data.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                          URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static UUID parseUndashedUuid(String raw) {
        if (raw.contains("-")) {
            return UUID.fromString(raw);
        }
        String formatted = raw.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                "$1-$2-$3-$4-$5"
        );
        return UUID.fromString(formatted);
    }
}
