package net.matzified.iceclient.launcher.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class MicrosoftAuthTester {
    public static void main(String[] args) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        try {
            // Step 1: Request Device Code
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://login.live.com/oauth20_connect.srf"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("client_id=00000000402b5328&scope=service::user.auth.xboxlive.com::MBI_SSL&response_type=device_code"))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("Connect response: " + res.body());

            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(res.body()).getAsJsonObject();
            String deviceCode = json.get("device_code").getAsString();
            System.out.println("Device code: " + deviceCode);

            // Step 2: Poll token endpoint immediately (when not yet approved)
            HttpRequest pollReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://login.live.com/oauth20_token.srf"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("client_id=00000000402b5328&grant_type=urn:ietf:params:oauth:grant-type:device_code&device_code=" + java.net.URLEncoder.encode(deviceCode, java.nio.charset.StandardCharsets.UTF_8)))
                    .build();
            HttpResponse<String> pollRes = client.send(pollReq, HttpResponse.BodyHandlers.ofString());
            System.out.println("\nPoll status code: " + pollRes.statusCode());
            System.out.println("Poll response headers: " + pollRes.headers().map());
            System.out.println("Poll response body: [" + pollRes.body() + "]");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
