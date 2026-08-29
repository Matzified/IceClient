package net.matzified.iceclient.launcher.auth;

import java.util.UUID;

/**
 * Represents an authenticated Minecraft Java Edition player session.
 * Injected directly into Minecraft launch arguments:
 * --username <username> --uuid <formattedUuid> --accessToken <accessToken> --userType msa
 */
public record MinecraftSession(
        String username,
        UUID uuid,
        String accessToken,
        String refreshToken
) {
    public String getTrimmedUuid() {
        return uuid.toString().replace("-", "");
    }

    public String getFormattedUuid() {
        return uuid.toString();
    }
}
