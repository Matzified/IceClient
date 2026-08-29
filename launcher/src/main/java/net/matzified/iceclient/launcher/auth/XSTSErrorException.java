package net.matzified.iceclient.launcher.auth;

public class XSTSErrorException extends Exception {

    private final long errorCode;

    public XSTSErrorException(long errorCode) {
        super(resolveErrorMessage(errorCode));
        this.errorCode = errorCode;
    }

    public long getErrorCode() {
        return errorCode;
    }

    private static String resolveErrorMessage(long code) {
        return switch ((int) code) {
            case (int) 2148916233L -> "Account does not have an Xbox Live profile. Please sign up at xbox.com.";
            case (int) 2148916235L -> "Xbox Live is not available in the account's country or region.";
            case (int) 2148916236L, (int) 2148916237L -> "Adult verification is required (e.g., South Korea / region requirement).";
            case (int) 2148916238L -> "Child Account: The account is under 18 and must be added to a Microsoft Family by an adult organizer.";
            default -> "Xbox Live XSTS authorization failed with error code: " + code;
        };
    }
}
