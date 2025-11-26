package org.igot.common.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Base64;

import org.igot.common.CommonConstants;
import org.igot.common.PropertiesCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeyManagerTest {

    @Mock
    private PropertiesCache propertiesCache;

    private KeyManager keyManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        keyManager = new KeyManager(propertiesCache);
    }

    @Test
    @DisplayName("Should load public keys from directory during init")
    void init_WithValidKeys_LoadsKeysSuccessfully() throws Exception {
        // Create test public key file
        KeyPair keyPair = generateKeyPair();
        String pemKey = convertToPEM(keyPair.getPublic());
        Path keyFile = tempDir.resolve("test-key-id");
        Files.writeString(keyFile, pemKey);

        when(propertiesCache.getProperty(CommonConstants.ACCESS_TOKEN_PUBLICKEY_BASEPATH))
                .thenReturn(tempDir.toString());

        keyManager.init();

        KeyData result = keyManager.getPublicKey("test-key-id");
        assertNotNull(result);
        assertEquals("test-key-id", result.getKeyId());
        assertNotNull(result.getPublicKey());
    }

    @Test
    @DisplayName("Should handle multiple public keys")
    void init_WithMultipleKeys_LoadsAllKeys() throws Exception {
        // Create multiple test public key files
        for (int i = 1; i <= 3; i++) {
            KeyPair keyPair = generateKeyPair();
            String pemKey = convertToPEM(keyPair.getPublic());
            Path keyFile = tempDir.resolve("key-" + i);
            Files.writeString(keyFile, pemKey);
        }

        when(propertiesCache.getProperty(CommonConstants.ACCESS_TOKEN_PUBLICKEY_BASEPATH))
                .thenReturn(tempDir.toString());

        keyManager.init();

        assertNotNull(keyManager.getPublicKey("key-1"));
        assertNotNull(keyManager.getPublicKey("key-2"));
        assertNotNull(keyManager.getPublicKey("key-3"));
    }

    @Test
    @DisplayName("Should return null for non-existent key")
    void getPublicKey_NonExistentKey_ReturnsNull() {
        KeyData result = keyManager.getPublicKey("non-existent-key");

        assertNull(result);
    }

    @Test
    @DisplayName("Should handle empty directory gracefully")
    void init_EmptyDirectory_NoKeysLoaded() {
        when(propertiesCache.getProperty(CommonConstants.ACCESS_TOKEN_PUBLICKEY_BASEPATH))
                .thenReturn(tempDir.toString());

        assertDoesNotThrow(() -> keyManager.init());
        assertNull(keyManager.getPublicKey("any-key"));
    }

    @Test
    @DisplayName("Should handle invalid key path gracefully")
    void init_InvalidPath_HandlesGracefully() {
        when(propertiesCache.getProperty(CommonConstants.ACCESS_TOKEN_PUBLICKEY_BASEPATH))
                .thenReturn("/non/existent/path");

        assertDoesNotThrow(() -> keyManager.init());
    }

    @Test
    @DisplayName("Should skip invalid PEM files")
    void init_WithInvalidPEMFile_SkipsInvalidFile() throws IOException {
        // Create an invalid PEM file
        Path invalidFile = tempDir.resolve("invalid-key");
        Files.writeString(invalidFile, "This is not a valid PEM file");

        when(propertiesCache.getProperty(CommonConstants.ACCESS_TOKEN_PUBLICKEY_BASEPATH))
                .thenReturn(tempDir.toString());

        assertDoesNotThrow(() -> keyManager.init());
        assertNull(keyManager.getPublicKey("invalid-key"));
    }

    @Test
    @DisplayName("Should load public key from PEM string correctly")
    void loadPublicKey_ValidPEM_ReturnsPublicKey() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String pemKey = convertToPEM(keyPair.getPublic());

        PublicKey result = KeyManager.loadPublicKey(pemKey);

        assertNotNull(result);
        assertEquals("RSA", result.getAlgorithm());
        assertEquals(keyPair.getPublic(), result);
    }

    @Test
    @DisplayName("Should handle PEM with extra whitespace")
    void loadPublicKey_PEMWithWhitespace_ParsesCorrectly() throws Exception {
        KeyPair keyPair = generateKeyPair();
        String pemKey = convertToPEM(keyPair.getPublic());
        // Add extra whitespace
        pemKey = "\n\n" + pemKey + "\n\n";

        PublicKey result = KeyManager.loadPublicKey(pemKey);

        assertNotNull(result);
        assertEquals(keyPair.getPublic(), result);
    }

    @Test
    @DisplayName("Should throw exception for invalid base64 content")
    void loadPublicKey_InvalidBase64_ThrowsException() {
        String invalidPEM = "-----BEGIN PUBLIC KEY-----\nNot valid base64!\n-----END PUBLIC KEY-----";

        assertThrows(Exception.class, () -> KeyManager.loadPublicKey(invalidPEM));
    }

    // Helper methods

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }

    private String convertToPEM(PublicKey publicKey) {
        String base64Key = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN PUBLIC KEY-----\n");
        // Add line breaks every 64 characters
        for (int i = 0; i < base64Key.length(); i += 64) {
            pem.append(base64Key, i, Math.min(i + 64, base64Key.length()));
            pem.append("\n");
        }
        pem.append("-----END PUBLIC KEY-----");
        return pem.toString();
    }
}
