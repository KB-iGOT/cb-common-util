package org.igot.common.auth;

/**
 * Utility class for Base64 encoding and decoding operations.
 * <p>
 * This class provides methods for handling both standard and URL-safe Base64 encoding,
 * commonly used in JWT (JSON Web Token) processing and cryptographic operations.
 * The implementation handles URL-safe Base64 conversion and automatic padding.
 * </p>
 *
 * @see java.util.Base64
 * 
 * @author Karthikeyan R (karthik-tarento)
 */
public class Base64Util {
    /**
     * Flag for standard Base64 encoding/decoding.
     */
    public static final int DEFAULT = 0;

    /**
     * Flag for URL-safe Base64 encoding/decoding (used in JWT tokens).
     */
    public static final int URL_SAFE = 8;

    /**
     * Decodes a Base64 encoded string into a byte array.
     * <p>
     * This method handles both standard and URL-safe Base64 encoding. When processing
     * URL-safe Base64 (common in JWT tokens), it automatically:
     * <ul>
     *   <li>Converts URL-safe characters ('-' and '_') to standard Base64 characters ('+' and '/')</li>
     *   <li>Adds padding ('=') if missing</li>
     * </ul>
     * </p>
     *
     * @param str the Base64 encoded string to decode
     * @param flags encoding flags ({@link #DEFAULT} or {@link #URL_SAFE})
     * @return the decoded byte array
     * @throws IllegalArgumentException if the input is not valid Base64
     */
    public static byte[] decode(String str, int flags) {
        // Handle URL-safe Base64 (used in JWT tokens)
        // Replace URL-safe characters with standard Base64 characters
        String standardBase64 = str.replace('-', '+').replace('_', '/');
        // Add padding if missing
        int paddingNeeded = (4 - standardBase64.length() % 4) % 4;
        standardBase64 = standardBase64 + "=".repeat(paddingNeeded);
        return java.util.Base64.getDecoder().decode(standardBase64);
    }

    /**
     * Decodes a Base64 encoded byte array into a decoded byte array.
     * <p>
     * This is a convenience method that converts the input byte array to a string
     * and delegates to {@link #decode(String, int)}.
     * </p>
     *
     * @param input the Base64 encoded byte array to decode
     * @param flags encoding flags ({@link #DEFAULT} or {@link #URL_SAFE})
     * @return the decoded byte array
     * @throws IllegalArgumentException if the input is not valid Base64
     * @see #decode(String, int)
     */
    public static byte[] decode(byte[] input, int flags) {
        return decode(new String(input), flags);
    }

    /**
     * Encodes a byte array into a Base64 encoded string.
     * <p>
     * This method encodes the input byte array using standard Base64 encoding.
     * The flags parameter is currently not used in the encoding logic but is
     * kept for API consistency.
     * </p>
     *
     * @param input the byte array to encode
     * @param flags encoding flags ({@link #DEFAULT} or {@link #URL_SAFE})
     * @return the Base64 encoded string
     */
    public static String encodeToString(byte[] input, int flags) {
        return java.util.Base64.getEncoder().encodeToString(input);
    }
}
