package com.auction.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Tiện ích băm và xác minh mật khẩu.
 *
 * <p>Định dạng lưu trữ: {@code BASE64(salt):BASE64(SHA-256(salt + password))}.
 * Mỗi mật khẩu có một salt ngẫu nhiên riêng (16 bytes), ngăn chặn rainbow-table attack.
 *
 * <p>Lưu ý: SHA-256 với salt đủ mạnh cho mục đích học thuật.
 * Trong môi trường production, nên dùng BCrypt / Argon2 (có key-stretching).
 */
public final class PasswordUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String SEPARATOR = ":";
    private static final int SALT_BYTES = 16;

    private PasswordUtils() {}

    /**
     * Băm mật khẩu với salt ngẫu nhiên.
     *
     * @param rawPassword mật khẩu thô
     * @return chuỗi {@code "salt:hash"} để lưu vào database
     */
    public static String hash(String rawPassword) {
        if (rawPassword == null) throw new IllegalArgumentException("Password must not be null");

        byte[] saltBytes = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(saltBytes);
        String salt = Base64.getEncoder().encodeToString(saltBytes);
        String hash = sha256(salt + rawPassword);
        return salt + SEPARATOR + hash;
    }

    /**
     * Xác minh mật khẩu thô với chuỗi đã băm.
     *
     * @param rawPassword mật khẩu người dùng nhập
     * @param stored      chuỗi được lưu trong database ({@code "salt:hash"})
     * @return {@code true} nếu khớp
     */
    public static boolean verify(String rawPassword, String stored) {
        if (rawPassword == null || stored == null) return false;
        int idx = stored.indexOf(SEPARATOR);
        if (idx < 0) return false; // định dạng không hợp lệ

        String salt         = stored.substring(0, idx);
        String expectedHash = stored.substring(idx + 1);
        return constantTimeEquals(expectedHash, sha256(salt + rawPassword));
    }

    // ----------------------------------------------------------------
    //  Internal helpers
    // ----------------------------------------------------------------

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the Java spec – cannot happen
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * So sánh chuỗi trong thời gian hằng để tránh timing attack.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
