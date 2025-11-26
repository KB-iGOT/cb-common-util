package org.igot.common.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CryptoUtilTest {

    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();
    }

    @Test
    @DisplayName("Should verify valid RSA signature")
    void verifyRSASign_ValidSignature_ReturnsTrue() throws Exception {
        String payload = "test payload data";
        byte[] signature = createSignature(payload);

        boolean result = CryptoUtil.verifyRSASign(
                payload,
                signature,
                keyPair.getPublic(),
                "SHA256withRSA"
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("Should reject invalid RSA signature")
    void verifyRSASign_InvalidSignature_ReturnsFalse() throws Exception {
        String payload = "test payload data";
        byte[] signature = createSignature(payload);

        // Corrupt the signature
        signature[0] = (byte) (signature[0] ^ 0xFF);

        boolean result = CryptoUtil.verifyRSASign(
                payload,
                signature,
                keyPair.getPublic(),
                "SHA256withRSA"
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("Should reject signature with different payload")
    void verifyRSASign_DifferentPayload_ReturnsFalse() throws Exception {
        String originalPayload = "original payload";
        String differentPayload = "different payload";
        byte[] signature = createSignature(originalPayload);

        boolean result = CryptoUtil.verifyRSASign(
                differentPayload,
                signature,
                keyPair.getPublic(),
                "SHA256withRSA"
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("Should reject signature with wrong public key")
    void verifyRSASign_WrongPublicKey_ReturnsFalse() throws Exception {
        String payload = "test payload data";
        byte[] signature = createSignature(payload);

        // Generate different key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair differentKeyPair = keyGen.generateKeyPair();

        boolean result = CryptoUtil.verifyRSASign(
                payload,
                signature,
                differentKeyPair.getPublic(),
                "SHA256withRSA"
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for invalid algorithm")
    void verifyRSASign_InvalidAlgorithm_ReturnsFalse() throws Exception {
        String payload = "test payload data";
        byte[] signature = createSignature(payload);

        boolean result = CryptoUtil.verifyRSASign(
                payload,
                signature,
                keyPair.getPublic(),
                "INVALID_ALGORITHM"
        );

        assertFalse(result);
    }

    @Test
    @DisplayName("Should handle empty payload")
    void verifyRSASign_EmptyPayload_WorksCorrectly() throws Exception {
        String payload = "";
        byte[] signature = createSignature(payload);

        boolean result = CryptoUtil.verifyRSASign(
                payload,
                signature,
                keyPair.getPublic(),
                "SHA256withRSA"
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle long payload")
    void verifyRSASign_LongPayload_WorksCorrectly() throws Exception {
        String payload = "A".repeat(10000);
        byte[] signature = createSignature(payload);

        boolean result = CryptoUtil.verifyRSASign(
                payload,
                signature,
                keyPair.getPublic(),
                "SHA256withRSA"
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle special characters in payload")
    void verifyRSASign_SpecialCharacters_WorksCorrectly() throws Exception {
        String payload = "test!@#$%^&*()_+-=[]{}|;':\",./<>?";
        byte[] signature = createSignature(payload);

        boolean result = CryptoUtil.verifyRSASign(
                payload,
                signature,
                keyPair.getPublic(),
                "SHA256withRSA"
        );

        assertTrue(result);
    }

    @Test
    @DisplayName("Should return false for null public key")
    void verifyRSASign_NullPublicKey_ReturnsFalse() throws Exception {
        String payload = "test payload data";
        byte[] signature = createSignature(payload);

        boolean result = CryptoUtil.verifyRSASign(
                payload,
                signature,
                null,
                "SHA256withRSA"
        );

        assertFalse(result);
    }

    // Helper method

    private byte[] createSignature(String payload) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(payload.getBytes("US-ASCII"));
        return signature.sign();
    }
}
