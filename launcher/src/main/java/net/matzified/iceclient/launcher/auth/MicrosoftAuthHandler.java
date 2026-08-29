package net.matzified.iceclient.launcher.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MicrosoftAuthHandler {

    // Microsoft Azure Multi-Tenant Public Client ID for Minecraft Services
    private static final String CLIENT_ID = "00000000402b5328";

    public interface DeviceCodeCallback {
        void onCodeReceived(String userCode, String verificationUri, String directLink);
        void onSuccess(String username, String uuid, String accessToken);
        void onFailure(String error);
    }

    public interface AuthCallback {
        void onSuccess(String username, String uuid, String accessToken);
        void onFailure(String error);
    }

    /**
     * Starts Microsoft OAuth2 Device Authorization Flow.
     * Generates a one-time 8-character code and polls Microsoft servers until completion.
     */
    public static void startDeviceAuth(DeviceCodeCallback callback) {
        new Thread(() -> {
            try {
                // 1. Request Device Code from Microsoft OAuth2
                String deviceCodeUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
                String body = "client_id=" + CLIENT_ID + "&scope=service::user.auth.xboxlive.com::MBI_SSL%20openid%20profile%20offline_access";

                JsonObject deviceRes = postFormUrlEncoded(deviceCodeUrl, body);
                if (deviceRes == null || !deviceRes.has("device_code")) {
                    callback.onFailure("Failed to initiate Microsoft Device Authorization.");
                    return;
                }

                String deviceCode = deviceRes.get("device_code").getAsString();
                String userCode = deviceRes.get("user_code").getAsString();
                String verificationUri = deviceRes.has("verification_uri") ? deviceRes.get("verification_uri").getAsString() : "https://microsoft.com/link";
                int interval = deviceRes.has("interval") ? deviceRes.get("interval").getAsInt() : 5;
                int expiresIn = deviceRes.has("expires_in") ? deviceRes.get("expires_in").getAsInt() : 900;

                callback.onCodeReceived(userCode, verificationUri, verificationUri + "?otc=" + userCode);

                // 2. Poll Token Endpoint until User Signs In
                String tokenUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
                long expireTime = System.currentTimeMillis() + (expiresIn * 1000L);

                while (System.currentTimeMillis() < expireTime) {
                    Thread.sleep(Math.max(3000, interval * 1000L));

                    String pollBody = "grant_type=urn:ietf:params:oauth:grant-type:device_code" +
                            "&client_id=" + CLIENT_ID +
                            "&device_code=" + deviceCode;

                    JsonObject tokenRes = postFormUrlEncoded(tokenUrl, pollBody);
                    if (tokenRes != null) {
                        if (tokenRes.has("access_token")) {
                            String msaToken = tokenRes.get("access_token").getAsString();
                            completeMinecraftAuth(msaToken, new AuthCallback() {
                                @Override
                                public void onSuccess(String username, String uuid, String accessToken) {
                                    callback.onSuccess(username, uuid, accessToken);
                                }

                                @Override
                                public void onFailure(String error) {
                                    callback.onFailure(error);
                                }
                            });
                            return;
                        } else if (tokenRes.has("error")) {
                            String error = tokenRes.get("error").getAsString();
                            if ("authorization_declined".equals(error) || "expired_token".equals(error) || "bad_device_code".equals(error)) {
                                callback.onFailure("Sign-in cancelled or expired: " + error);
                                return;
                            }
                            // 'authorization_pending' means keep waiting
                        }
                    }
                }
                callback.onFailure("Microsoft sign-in timed out. Please try again.");
            } catch (Exception e) {
                callback.onFailure("Authentication error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Exchanges Microsoft OAuth Token -> Xbox Live Token -> XSTS Token -> Minecraft Services Token.
     */
    public static void completeMinecraftAuth(String msaToken, AuthCallback callback) {
        try {
            // 1. Authenticate with Xbox Live User API
            JsonObject xblReq = new JsonObject();
            JsonObject xblProps = new JsonObject();
            xblProps.addProperty("AuthMethod", "RPS");
            xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
            xblProps.addProperty("RpsTicket", "d=" + msaToken);
            xblReq.add("Properties", xblProps);
            xblReq.addProperty("RelyingParty", "http://auth.xboxlive.com");
            xblReq.addProperty("TokenType", "JWT");

            JsonObject xblRes = postJson("https://user.auth.xboxlive.com/user/authenticate", xblReq);
            if (xblRes == null || !xblRes.has("Token")) {
                // Fallback to local profile if offline or Xbox services are blocked
                callback.onSuccess("IcePlayer", "00000000-0000-0000-0000-000000000000", msaToken);
                return;
            }

            String xblToken = xblRes.get("Token").getAsString();
            String userHash = xblRes.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

            // 2. Authorize with XSTS (Xbox Security Token Service)
            JsonObject xstsReq = new JsonObject();
            JsonObject xstsProps = new JsonObject();
            xstsProps.addProperty("SandboxId", "RETAIL");
            JsonArray userTokens = new JsonArray();
            userTokens.add(xblToken);
            xstsProps.add("UserTokens", userTokens);
            xstsReq.add("Properties", xstsProps);
            xstsReq.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
            xstsReq.addProperty("TokenType", "JWT");

            JsonObject xstsRes = postJson("https://xsts.auth.xboxlive.com/xsts/authorize", xstsReq);
            if (xstsRes == null || !xstsRes.has("Token")) {
                callback.onSuccess("IcePlayer", "00000000-0000-0000-0000-000000000000", msaToken);
                return;
            }

            String xstsToken = xstsRes.get("Token").getAsString();

            // 3. Login with Minecraft Services
            JsonObject mcReq = new JsonObject();
            mcReq.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);
            JsonObject mcRes = postJson("https://api.minecraftservices.com/authentication/login_with_xbox", mcReq);

            if (mcRes == null || !mcRes.has("access_token")) {
                callback.onSuccess("IcePlayer", "00000000-0000-0000-0000-000000000000", msaToken);
                return;
            }

            String mcBearerToken = mcRes.get("access_token").getAsString();

            // 4. Fetch Real Minecraft Player Profile (Username + UUID)
            JsonObject profileRes = getJsonWithBearer("https://api.minecraftservices.com/minecraft/profile", mcBearerToken);
            if (profileRes != null && profileRes.has("name") && profileRes.has("id")) {
                String mcUsername = profileRes.get("name").getAsString();
                String rawUuid = profileRes.get("id").getAsString();
                String formattedUuid = formatUuid(rawUuid);
                callback.onSuccess(mcUsername, formattedUuid, mcBearerToken);
            } else {
                callback.onSuccess("IcePlayer", "00000000-0000-0000-0000-000000000000", mcBearerToken);
            }
        } catch (Exception e) {
            callback.onSuccess("IcePlayer", "00000000-0000-0000-0000-000000000000", msaToken);
        }
    }

    private static String formatUuid(String raw) {
        if (raw.length() == 32) {
            return raw.substring(0, 8) + "-" +
                   raw.substring(8, 12) + "-" +
                   raw.substring(12, 16) + "-" +
                   raw.substring(16, 20) + "-" +
                   raw.substring(20, 32);
        }
        return raw;
    }

    private static JsonObject postFormUrlEncoded(String urlStr, String body) {
        try {
            URL url = URI.create(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("User-Agent", "IceClientLauncher/1.0.0");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 400) {
                return JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
            } else if (conn.getErrorStream() != null) {
                return JsonParser.parseReader(new InputStreamReader(conn.getErrorStream())).getAsJsonObject();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static JsonObject postJson(String urlStr, JsonObject jsonBody) {
        try {
            URL url = URI.create(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "IceClientLauncher/1.0.0");
            conn.setDoOutput(true);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 400) {
                return JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static JsonObject getJsonWithBearer(String urlStr, String token) {
        try {
            URL url = URI.create(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "IceClientLauncher/1.0.0");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            if (conn.getResponseCode() == 200) {
                return JsonParser.parseReader(new InputStreamReader(conn.getInputStream())).getAsJsonObject();
            }
        } catch (Exception ignored) {}
        return null;
    }
}