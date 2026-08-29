package net.matzified.iceclient.launcher.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MicrosoftPKCEAuthenticator {

    public static final String CLIENT_ID = "00000000402b5328";
    public static final String SCOPE = "XboxLive.signin offline_access";

    private final HttpClient httpClient;

    public MicrosoftPKCEAuthenticator() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Starts the full PKCE Authorization flow with local loopback server interception.
     */
    public CompletableFuture<MinecraftSession> startLoginFlowAsync(Consumer<String> statusCallback) {
        CompletableFuture<MinecraftSession> future = new CompletableFuture<>();
        PKCE pkce = new PKCE();

        HttpServer server;
        int port;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            port = server.getAddress().getPort();
        } catch (IOException e) {
            future.completeExceptionally(new RuntimeException("Failed to bind local loopback server", e));
            return future;
        }

        String redirectUri = "http://127.0.0.1:" + port;
        AtomicBoolean isCaptured = new AtomicBoolean(false);

        Map<String, String> query = Map.of(
                "client_id", CLIENT_ID,
                "response_type", "code",
                "redirect_uri", redirectUri,
                "scope", SCOPE,
                "code_challenge", pkce.getCodeChallenge(),
                "code_challenge_method", "S256",
                "prompt", "select_account"
        );
        String authUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?" + encodeQuery(query);

        server.createContext("/", exchange -> {
            String requestQuery = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQuery(requestQuery);

            if (params.containsKey("code") && isCaptured.compareAndSet(false, true)) {
                String code = params.get("code");

                String responseHtml = "<!DOCTYPE html><html><head><title>Ice Client Login</title>" +
                        "<style>body{background:#0d0f14;color:#f8fafc;font-family:Segoe UI,sans-serif;text-align:center;padding-top:15%;}" +
                        ".card{background:#181c26;display:inline-block;padding:32px 48px;border-radius:16px;border:1px solid #38bdf8;box-shadow:0 10px 30px rgba(56,189,248,0.2);}" +
                        "h1{color:#38bdf8;margin-bottom:8px;}p{color:#94a3b8;font-size:15px;}</style></head>" +
                        "<body><div class='card'><h1>✓ Authentication Successful</h1><p>You can now return to Ice Client.</p></div></body></html>";
                byte[] bytes = responseHtml.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }

                server.stop(1);

                if (statusCallback != null) statusCallback.accept("Code received! Exchanging tokens...");

                // Execute 5-step token exchange
                exchangeTokensAsync(code, pkce.getCodeVerifier(), redirectUri, statusCallback)
                        .thenAccept(future::complete)
                        .exceptionally(ex -> {
                            future.completeExceptionally(ex);
                            return null;
                        });

            } else if (params.containsKey("error") && isCaptured.compareAndSet(false, true)) {
                String error = params.get("error_description");
                server.stop(1);
                future.completeExceptionally(new RuntimeException("OAuth Error: " + (error != null ? error : params.get("error"))));
            }
        });

        server.start();

        if (statusCallback != null) statusCallback.accept("Opening Microsoft sign-in window...");

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(authUrl));
            }
        } catch (Exception e) {
            server.stop(0);
            future.completeExceptionally(e);
        }

        return future;
    }

    public CompletableFuture<MinecraftSession> exchangeTokensAsync(String code, String codeVerifier, String redirectUri, Consumer<String> status) {
        if (status != null) status.accept("Authenticating with Microsoft Identity...");

        return exchangeMicrosoftTokenAsync(code, codeVerifier, redirectUri)
                .thenCompose(tokens -> {
                    if (status != null) status.accept("Authenticating with Xbox Live...");
                    return authenticateXboxLiveAsync(tokens.accessToken())
                            .thenCompose(xbl -> {
                                if (status != null) status.accept("Authorizing with XSTS Services...");
                                return authorizeXSTSAsync(xbl);
                            })
                            .thenCompose(xsts -> {
                                if (status != null) status.accept("Authenticating with Minecraft Services...");
                                return authenticateMinecraftServicesAsync(xsts);
                            })
                            .thenCompose(mcToken -> {
                                if (status != null) status.accept("Fetching Minecraft Profile & Gamertag...");
                                return fetchMinecraftProfileAsync(mcToken, tokens.refreshToken());
                            });
                });
    }

    // =========================================================================
    // STEP A: Microsoft Token Exchange (Authorization Code + PKCE Verifier)
    // =========================================================================

    public record MicrosoftTokens(String accessToken, String refreshToken) {}

    public CompletableFuture<MicrosoftTokens> exchangeMicrosoftTokenAsync(String code, String codeVerifier, String redirectUri) {
        Map<String, String> body = Map.of(
                "client_id", CLIENT_ID,
                "grant_type", "authorization_code",
                "code", code,
                "redirect_uri", redirectUri,
                "code_verifier", codeVerifier,
                "scope", SCOPE
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://login.microsoftonline.com/consumers/oauth2/v2.0/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encodeFormData(body)))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Microsoft Token Exchange Failed: " + response.body());
                    }
                    return new MicrosoftTokens(
                            json.get("access_token").getAsString(),
                            json.has("refresh_token") ? json.get("refresh_token").getAsString() : null
                    );
                });
    }

    // =========================================================================
    // STEP B: Xbox Live Authentication (user.auth.xboxlive.com)
    // =========================================================================

    public record XboxLiveToken(String token, String uhs) {}

    public CompletableFuture<XboxLiveToken> authenticateXboxLiveAsync(String msAccessToken) {
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

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Xbox Live Authentication Failed: " + response.body());
                    }
                    String token = json.get("Token").getAsString();
                    String uhs = json.getAsJsonObject("DisplayClaims")
                            .getAsJsonArray("xui")
                            .get(0).getAsJsonObject()
                            .get("uhs").getAsString();
                    return new XboxLiveToken(token, uhs);
                });
    }

    // =========================================================================
    // STEP C: XSTS Token Authorization (xsts.auth.xboxlive.com)
    // =========================================================================

    public record XstsToken(String token, String uhs) {}

    public CompletableFuture<XstsToken> authorizeXSTSAsync(XboxLiveToken xboxToken) {
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

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (response.statusCode() != 200) {
                        if (json.has("XErr")) {
                            long xerr = json.get("XErr").getAsLong();
                            throw new RuntimeException("XSTS Error (" + xerr + "): " + resolveXstsError(xerr));
                        }
                        throw new RuntimeException("XSTS Authorization Failed: " + response.body());
                    }
                    String token = json.get("Token").getAsString();
                    String uhs = json.getAsJsonObject("DisplayClaims")
                            .getAsJsonArray("xui")
                            .get(0).getAsJsonObject()
                            .get("uhs").getAsString();
                    return new XstsToken(token, uhs);
                });
    }

    // =========================================================================
    // STEP D: Minecraft Services Authentication (api.minecraftservices.com)
    // =========================================================================

    public CompletableFuture<String> authenticateMinecraftServicesAsync(XstsToken xsts) {
        JsonObject payload = new JsonObject();
        payload.addProperty("identityToken", "XBL3.0 x=" + xsts.uhs() + ";" + xsts.token());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Minecraft Services Login Failed: " + response.body());
                    }
                    return json.get("access_token").getAsString();
                });
    }

    // =========================================================================
    // STEP E: Minecraft Profile Fetch (api.minecraftservices.com)
    // =========================================================================

    public CompletableFuture<MinecraftSession> fetchMinecraftProfileAsync(String mcAccessToken, String refreshToken) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("Authorization", "Bearer " + mcAccessToken)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (response.statusCode() != 200) {
                        if (response.statusCode() == 404) {
                            throw new RuntimeException("This Microsoft account does not own Minecraft: Java Edition.");
                        }
                        throw new RuntimeException("Failed to fetch Minecraft Profile: " + response.body());
                    }

                    String rawUuid = json.get("id").getAsString();
                    String username = json.get("name").getAsString();
                    UUID uuid = parseUndashedUuid(rawUuid);

                    return new MinecraftSession(username, uuid, mcAccessToken, refreshToken);
                });
    }

    private static String encodeFormData(Map<String, String> data) {
        return data.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                          URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static String encodeQuery(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
              .append("=")
              .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, val);
            }
        }
        return params;
    }

    private static UUID parseUndashedUuid(String raw) {
        if (raw.contains("-")) return UUID.fromString(raw);
        String formatted = raw.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                "$1-$2-$3-$4-$5"
        );
        return UUID.fromString(formatted);
    }

    private static String resolveXstsError(long code) {
        return switch ((int) code) {
            case (int) 2148916233L -> "Account does not have an Xbox Live profile. Sign up at xbox.com.";
            case (int) 2148916235L -> "Xbox Live is not available in your country/region.";
            case (int) 2148916236L, (int) 2148916237L -> "Adult verification is required for this region.";
            case (int) 2148916238L -> "Child Account: Must be added to a Family group by an adult organizer.";
            default -> "Error Code " + code;
        };
    }
}
