package org.igot.common.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Base64UtilTest {

    @Test
    @DisplayName("Should decode Base64 string correctly")
    void decode_String_DecodesCorrectly() {
        String original = "Hello, World!";
        String encoded = Base64.getEncoder().encodeToString(original.getBytes(StandardCharsets.UTF_8));

        byte[] result = Base64Util.decode(encoded, 0);

        assertEquals(original, new String(result, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should decode Base64 byte array correctly")
    void decode_ByteArray_DecodesCorrectly() {
        String original = "Test Data";
        byte[] encoded = Base64.getEncoder().encode(original.getBytes(StandardCharsets.UTF_8));

        byte[] result = Base64Util.decode(encoded, 0);

        assertEquals(original, new String(result, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should encode to Base64 string correctly")
    void encodeToString_EncodesCorrectly() {
        String original = "Hello, World!";
        byte[] data = original.getBytes(StandardCharsets.UTF_8);

        String result = Base64Util.encodeToString(data, 0);

        String expected = Base64.getEncoder().encodeToString(data);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Should handle empty string")
    void decode_EmptyString_ReturnsEmptyArray() {
        // Empty string with padding added becomes "====" which decodes to empty
        String encoded = "";

        byte[] result = Base64Util.decode(encoded, 0);

        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("Should decode URL-safe Base64 without padding")
    void decode_UrlSafeWithoutPadding_DecodesCorrectly() {
        // URL-safe encoded string (as used in JWT tokens)
        String original = "Hello, World!";
        String urlSafeEncoded = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(original.getBytes(StandardCharsets.UTF_8));

        byte[] result = Base64Util.decode(urlSafeEncoded, 0);

        assertEquals(original, new String(result, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should decode URL-safe Base64 with special characters")
    void decode_UrlSafeSpecialChars_DecodesCorrectly() {
        // This string will produce - and _ in URL-safe encoding
        byte[] data = {(byte) 0xfb, (byte) 0xff, (byte) 0xfe};
        String urlSafeEncoded = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(data);

        byte[] result = Base64Util.decode(urlSafeEncoded, 0);

        assertArrayEquals(data, result);
    }

    @Test
    @DisplayName("Should handle empty byte array encoding")
    void encodeToString_EmptyArray_ReturnsEmptyString() {
        byte[] data = new byte[0];

        String result = Base64Util.encodeToString(data, 0);

        assertEquals("", result);
    }

    @Test
    @DisplayName("Should encode and decode round trip")
    void encodeAndDecode_RoundTrip_PreservesData() {
        String original = "This is a test message with special chars: !@#$%^&*()";
        byte[] data = original.getBytes(StandardCharsets.UTF_8);

        String encoded = Base64Util.encodeToString(data, 0);
        byte[] decoded = Base64Util.decode(encoded, 0);

        assertEquals(original, new String(decoded, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should handle binary data correctly")
    void decode_BinaryData_HandlesCorrectly() {
        byte[] binaryData = new byte[256];
        for (int i = 0; i < 256; i++) {
            binaryData[i] = (byte) i;
        }

        String encoded = Base64Util.encodeToString(binaryData, 0);
        byte[] decoded = Base64Util.decode(encoded, 0);

        assertArrayEquals(binaryData, decoded);
    }

    @Test
    @DisplayName("Should handle large data")
    void decode_LargeData_HandlesCorrectly() {
        byte[] largeData = new byte[10000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        String encoded = Base64Util.encodeToString(largeData, 0);
        byte[] decoded = Base64Util.decode(encoded, 0);

        assertArrayEquals(largeData, decoded);
    }

    @Test
    @DisplayName("Should decode standard Base64 with padding")
    void decode_WithPadding_DecodesCorrectly() {
        // "ab" encodes to "YWI=" with padding
        String encoded = "YWI=";

        byte[] result = Base64Util.decode(encoded, 0);

        assertEquals("ab", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should decode Base64 with double padding")
    void decode_DoublePadding_DecodesCorrectly() {
        // "a" encodes to "YQ==" with double padding
        String encoded = "YQ==";

        byte[] result = Base64Util.decode(encoded, 0);

        assertEquals("a", new String(result, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Should throw exception for invalid Base64")
    void decode_InvalidBase64_ThrowsException() {
        String invalidBase64 = "Not valid!@#$";

        assertThrows(IllegalArgumentException.class, () -> Base64Util.decode(invalidBase64, 0));
    }

    @Test
    @DisplayName("Flags parameter should be ignored (backward compatibility)")
    void decode_WithDifferentFlags_SameResult() {
        String original = "Test";
        String encoded = Base64.getEncoder().encodeToString(original.getBytes(StandardCharsets.UTF_8));

        byte[] result1 = Base64Util.decode(encoded, 0);
        byte[] result2 = Base64Util.decode(encoded, 11);
        byte[] result3 = Base64Util.decode(encoded, 255);

        assertArrayEquals(result1, result2);
        assertArrayEquals(result2, result3);
    }

    @Test
    @DisplayName("Should handle Unicode characters")
    void encodeAndDecode_Unicode_PreservesData() {
        String unicode = "Hello 世界 🌍 مرحبا";
        byte[] data = unicode.getBytes(StandardCharsets.UTF_8);

        String encoded = Base64Util.encodeToString(data, 0);
        byte[] decoded = Base64Util.decode(encoded, 0);

        assertEquals(unicode, new String(decoded, StandardCharsets.UTF_8));
    }
}
