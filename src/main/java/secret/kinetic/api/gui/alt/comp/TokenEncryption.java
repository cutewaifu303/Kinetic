package secret.kinetic.api.gui.alt.comp;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TokenEncryption {

    private static final byte[] KEY = new byte[]{89, 117, 114, 105, 83, 101, 120, 76, 101, 103, 101, 110, 100, 33, 73, 76, 111, 118, 101, 76, 101, 115, 98, 105, 97, 110, 115, 33};

    public static String encrypt(String rawToken) {
        if (rawToken == null) return null;
        byte[] tokenBytes = rawToken.getBytes(StandardCharsets.UTF_8);
        byte[] encryptedBytes = new byte[tokenBytes.length];

        for (int i = 0; i < tokenBytes.length; i++) {
            encryptedBytes[i] = (byte) (tokenBytes[i] ^ KEY[i % KEY.length]);
        }

        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decrypt(String encryptedTokenBase64) {
        if (encryptedTokenBase64 == null) return null;
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedTokenBase64);
            byte[] decryptedBytes = new byte[decodedBytes.length];

            for (int i = 0; i < decodedBytes.length; i++) {
                decryptedBytes[i] = (byte) (decodedBytes[i] ^ KEY[i % KEY.length]);
            }

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
