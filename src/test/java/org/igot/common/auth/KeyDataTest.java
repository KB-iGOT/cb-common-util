package org.igot.common.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KeyDataTest {

    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();
    }

    @Test
    @DisplayName("Should create KeyData with all args constructor")
    void constructor_AllArgs_SetsFieldsCorrectly() {
        String keyId = "test-key-id";
        PublicKey publicKey = keyPair.getPublic();

        KeyData keyData = new KeyData(keyId, publicKey);

        assertEquals(keyId, keyData.getKeyId());
        assertEquals(publicKey, keyData.getPublicKey());
    }

    @Test
    @DisplayName("Should get keyId correctly")
    void getKeyId_ReturnsCorrectValue() {
        KeyData keyData = new KeyData("my-key", keyPair.getPublic());

        assertEquals("my-key", keyData.getKeyId());
    }

    @Test
    @DisplayName("Should get publicKey correctly")
    void getPublicKey_ReturnsCorrectValue() {
        PublicKey publicKey = keyPair.getPublic();
        KeyData keyData = new KeyData("key-id", publicKey);

        assertEquals(publicKey, keyData.getPublicKey());
        assertSame(publicKey, keyData.getPublicKey());
    }

    @Test
    @DisplayName("Should set keyId correctly")
    void setKeyId_SetsCorrectly() {
        KeyData keyData = new KeyData("initial-id", keyPair.getPublic());
        keyData.setKeyId("new-id");

        assertEquals("new-id", keyData.getKeyId());
    }

    @Test
    @DisplayName("Should set publicKey correctly")
    void setPublicKey_SetsCorrectly() throws Exception {
        KeyData keyData = new KeyData("key-id", keyPair.getPublic());

        // Generate a new key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair newKeyPair = keyGen.generateKeyPair();

        keyData.setPublicKey(newKeyPair.getPublic());

        assertEquals(newKeyPair.getPublic(), keyData.getPublicKey());
        assertNotEquals(keyPair.getPublic(), keyData.getPublicKey());
    }

    @Test
    @DisplayName("Should handle null keyId")
    void constructor_NullKeyId_HandlesCorrectly() {
        KeyData keyData = new KeyData(null, keyPair.getPublic());

        assertNull(keyData.getKeyId());
        assertNotNull(keyData.getPublicKey());
    }

    @Test
    @DisplayName("Should handle null publicKey")
    void constructor_NullPublicKey_HandlesCorrectly() {
        KeyData keyData = new KeyData("key-id", null);

        assertEquals("key-id", keyData.getKeyId());
        assertNull(keyData.getPublicKey());
    }

    @Test
    @DisplayName("Should handle both null values")
    void constructor_BothNull_HandlesCorrectly() {
        KeyData keyData = new KeyData(null, null);

        assertNull(keyData.getKeyId());
        assertNull(keyData.getPublicKey());
    }

    @Test
    @DisplayName("Should handle empty keyId")
    void constructor_EmptyKeyId_HandlesCorrectly() {
        KeyData keyData = new KeyData("", keyPair.getPublic());

        assertEquals("", keyData.getKeyId());
    }

    @Test
    @DisplayName("Should handle long keyId")
    void constructor_LongKeyId_HandlesCorrectly() {
        String longKeyId = "key-" + "x".repeat(1000);
        KeyData keyData = new KeyData(longKeyId, keyPair.getPublic());

        assertEquals(longKeyId, keyData.getKeyId());
    }

    @Test
    @DisplayName("Should handle special characters in keyId")
    void constructor_SpecialCharsInKeyId_HandlesCorrectly() {
        String specialKeyId = "key_!@#$%^&*()_+-=[]{}|;':\",./<>?";
        KeyData keyData = new KeyData(specialKeyId, keyPair.getPublic());

        assertEquals(specialKeyId, keyData.getKeyId());
    }

    @Test
    @DisplayName("Should verify RSA algorithm of public key")
    void getPublicKey_HasRSAAlgorithm() {
        KeyData keyData = new KeyData("rsa-key", keyPair.getPublic());

        assertEquals("RSA", keyData.getPublicKey().getAlgorithm());
    }

    @Test
    @DisplayName("Should preserve public key format")
    void getPublicKey_PreservesFormat() {
        KeyData keyData = new KeyData("key-id", keyPair.getPublic());

        assertEquals("X.509", keyData.getPublicKey().getFormat());
    }

    @Test
    @DisplayName("Should allow setting to null after initialization")
    void setFields_ToNull_SetsCorrectly() {
        KeyData keyData = new KeyData("key-id", keyPair.getPublic());

        keyData.setKeyId(null);
        keyData.setPublicKey(null);

        assertNull(keyData.getKeyId());
        assertNull(keyData.getPublicKey());
    }

    @Test
    @DisplayName("Multiple KeyData instances should be independent")
    void multipleInstances_AreIndependent() {
        KeyData keyData1 = new KeyData("key-1", keyPair.getPublic());
        KeyData keyData2 = new KeyData("key-2", keyPair.getPublic());

        keyData1.setKeyId("modified-key");

        assertEquals("modified-key", keyData1.getKeyId());
        assertEquals("key-2", keyData2.getKeyId());
    }
}
