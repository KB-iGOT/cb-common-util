package org.igot.common.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.igot.common.ApiResponse;
import org.igot.common.CommonConstants;
import org.igot.common.PropertiesCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AccessTokenValidatorTest {

    @Mock
    private KeyManager keyManager;

    @Mock
    private PropertiesCache propertiesCache;

    private ObjectMapper objectMapper;
    private AccessTokenValidator validator;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        validator = new AccessTokenValidator(keyManager, objectMapper, propertiesCache);

        // Generate RSA key pair for testing
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED for invalid token format")
    void verifyUserToken_InvalidTokenFormat_ReturnsUnauthorized() {
        String invalidToken = "invalid.token";

        String result = validator.verifyUserToken(invalidToken);

        assertEquals(CommonConstants._UNAUTHORIZED, result);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when public key not found")
    void verifyUserToken_PublicKeyNotFound_ReturnsUnauthorized() throws Exception {
        String token = createTestToken("nonexistent-key", createPayload(), keyPair);

        when(keyManager.getPublicKey("nonexistent-key")).thenReturn(null);

        String result = validator.verifyUserToken(token);

        assertEquals(CommonConstants._UNAUTHORIZED, result);
        verify(keyManager).getPublicKey("nonexistent-key");
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when token signature is invalid")
    void verifyUserToken_InvalidSignature_ReturnsUnauthorized() throws Exception {
        Map<String, Object> payload = createPayload();
        String token = createTestToken("test-key", payload, keyPair);

        // Create different key pair for verification (signature mismatch)
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair differentKeyPair = keyGen.generateKeyPair();

        KeyData keyData = new KeyData("test-key", differentKeyPair.getPublic());
        when(keyManager.getPublicKey("test-key")).thenReturn(keyData);

        String result = validator.verifyUserToken(token);

        assertEquals(CommonConstants._UNAUTHORIZED, result);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when token is expired")
    void verifyUserToken_ExpiredToken_ReturnsUnauthorized() throws Exception {
        Map<String, Object> payload = createPayload();
        payload.put("exp", (int) (System.currentTimeMillis() / 1000 - 3600)); // Expired 1 hour ago

        String token = createTestToken("test-key", payload, keyPair);
        KeyData keyData = new KeyData("test-key", keyPair.getPublic());

        when(keyManager.getPublicKey("test-key")).thenReturn(keyData);

        String result = validator.verifyUserToken(token);

        assertEquals(CommonConstants._UNAUTHORIZED, result);
    }

    @Test
    @DisplayName("Should return UNAUTHORIZED when issuer check fails")
    void verifyUserToken_InvalidIssuer_ReturnsUnauthorized() throws Exception {
        Map<String, Object> payload = createPayload();
        payload.put("iss", "https://wrong-issuer.com/realms/test");

        String token = createTestToken("test-key", payload, keyPair);
        KeyData keyData = new KeyData("test-key", keyPair.getPublic());

        when(keyManager.getPublicKey("test-key")).thenReturn(keyData);
        when(propertiesCache.getProperty(CommonConstants.SSO_URL)).thenReturn("https://sso.example.com/");
        when(propertiesCache.getProperty(CommonConstants.SSO_REALM)).thenReturn("myrealm");

        String result = validator.verifyUserToken(token);

        assertEquals(CommonConstants._UNAUTHORIZED, result);
    }

    @Test
    @DisplayName("Should extract user ID from valid token")
    void verifyUserToken_ValidToken_ReturnsUserId() throws Exception {
        Map<String, Object> payload = createPayload();
        String token = createTestToken("test-key", payload, keyPair);
        KeyData keyData = new KeyData("test-key", keyPair.getPublic());

        when(keyManager.getPublicKey("test-key")).thenReturn(keyData);
        when(propertiesCache.getProperty(CommonConstants.SSO_URL)).thenReturn("https://sso.example.com/");
        when(propertiesCache.getProperty(CommonConstants.SSO_REALM)).thenReturn("myrealm");

        String result = validator.verifyUserToken(token);

        assertEquals("user123", result);
    }

    @Test
    @DisplayName("Should return null for fetchUserIdFromAccessToken with null token")
    void fetchUserIdFromAccessToken_NullToken_ReturnsNull() {
        String result = validator.fetchUserIdFromAccessToken(null);

        assertNull(result);
    }

    @Test
    @DisplayName("Should return null for fetchUserIdFromAccessToken when token is unauthorized")
    void fetchUserIdFromAccessToken_UnauthorizedToken_ReturnsNull() {
        String invalidToken = "invalid.token.here";

        String result = validator.fetchUserIdFromAccessToken(invalidToken);

        assertNull(result);
    }

    @Test
    @DisplayName("Should set error response when token is unauthorized")
    void fetchUserIdFromAccessToken_WithResponse_UnauthorizedToken_SetsErrorResponse() {
        String invalidToken = "invalid.token.here";
        ApiResponse response = new ApiResponse("test-api");

        String result = validator.fetchUserIdFromAccessToken(invalidToken, response);

        assertNull(result);
        assertEquals(CommonConstants.FAILED, response.getParams().getStatus());
        assertEquals(CommonConstants.ACCESS_TOKEN_IS_EXPIRED, response.getParams().getErrMsg());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getResponseCode());
    }

    @Test
    @DisplayName("Should extract token payload successfully")
    void extractTokenPayload_ValidToken_ReturnsPayload() throws Exception {
        Map<String, Object> payload = createPayload();
        String token = createTestToken("test-key", payload, keyPair);
        KeyData keyData = new KeyData("test-key", keyPair.getPublic());

        when(keyManager.getPublicKey("test-key")).thenReturn(keyData);
        when(propertiesCache.getProperty(CommonConstants.SSO_URL)).thenReturn("https://sso.example.com/");
        when(propertiesCache.getProperty(CommonConstants.SSO_REALM)).thenReturn("myrealm");

        Map<String, Object> result = validator.extractTokenPayload(token);

        assertFalse(result.isEmpty());
        assertEquals("f:user:user123", result.get("sub"));
    }

    @Test
    @DisplayName("Should return empty map for invalid token in extractTokenPayload")
    void extractTokenPayload_InvalidToken_ReturnsEmptyMap() {
        String invalidToken = "invalid.token.here";

        Map<String, Object> result = validator.extractTokenPayload(invalidToken);

        assertTrue(result.isEmpty());
    }

    // Helper methods

    private Map<String, Object> createPayload() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("sub", "f:user:user123");
        payload.put("iss", "https://sso.example.com/realms/myrealm");
        payload.put("exp", (int) (System.currentTimeMillis() / 1000 + 3600)); // Valid for 1 hour
        payload.put("iat", (int) (System.currentTimeMillis() / 1000));
        return payload;
    }

    private String createTestToken(String keyId, Map<String, Object> payload, KeyPair keyPair) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Create header
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "RS256");
        header.put("typ", "JWT");
        header.put("kid", keyId);

        // Encode header and payload
        String headerEncoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mapper.writeValueAsBytes(header));
        String payloadEncoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mapper.writeValueAsBytes(payload));

        // Create signature
        String dataToSign = headerEncoded + "." + payloadEncoded;
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(dataToSign.getBytes("US-ASCII"));
        byte[] signatureBytes = signature.sign();

        String signatureEncoded = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signatureBytes);

        return headerEncoded + "." + payloadEncoded + "." + signatureEncoded;
    }
}
