package net.matzified.iceclient.launcher.auth;

public record MicrosoftDeviceCode(
        String userCode,
        String deviceCode,
        String verificationUri,
        int expiresIn,
        int interval
) {}
