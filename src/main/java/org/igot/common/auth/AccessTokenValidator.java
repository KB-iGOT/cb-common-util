package org.igot.common.auth;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.igot.common.ApiResponse;
import org.igot.common.CommonConstants;
import org.igot.common.PropertiesCache;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Validator for JWT access tokens used in authentication and authorization.
 * <p>
 * This component validates JWT (JSON Web Token) access tokens by:
 * <ul>
 *   <li>Verifying the RSA signature using public keys managed by {@link KeyManager}</li>
 *   <li>Validating the token's expiration time</li>
 *   <li>Checking the issuer (iss) claim against the configured SSO realm</li>
 *   <li>Extracting user information from the token payload</li>
 * </ul>
 * </p>
 * <p>
 * The validator uses the standard JWT format: header.payload.signature, where each
 * component is Base64-encoded. The token's signature is verified using the public key
 * identified by the "kid" (key ID) claim in the JWT header.
 * </p>
 *
 * @see KeyManager
 * @see CryptoUtil
 * 
 * @author Karthikeyan R (karthik-tarento)
 */
@Slf4j
@Component
public class AccessTokenValidator {

    private final ObjectMapper mapper;
    private final PropertiesCache cache;
    private final KeyManager keyManager;

    /**
     * Constructs an AccessTokenValidator with required dependencies.
     *
     * @param keyManager the KeyManager instance for retrieving public keys used in signature verification
     * @param mapper the ObjectMapper for parsing JSON content from JWT tokens
     * @param cache the PropertiesCache containing SSO configuration (realm URL, etc.)
     */
    public AccessTokenValidator(KeyManager keyManager, ObjectMapper mapper, PropertiesCache cache) {
        this.keyManager = keyManager;
        this.mapper = mapper;
        this.cache = cache;
    }

    /**
     * Validates a JWT access token by verifying its signature, expiration, and structure.
     * <p>
     * This method performs the following validation steps:
     * <ol>
     *   <li>Splits the token into header, payload, and signature components</li>
     *   <li>Extracts the key ID (kid) from the JWT header</li>
     *   <li>Retrieves the corresponding public key from the KeyManager</li>
     *   <li>Verifies the RSA signature using SHA256withRSA algorithm</li>
     *   <li>Checks if the token has expired using the "exp" claim</li>
     * </ol>
     * Any validation failures are logged and result in an empty map being returned.
     * </p>
     *
     * @param token the JWT access token string in format "header.payload.signature"
     * @return a map containing the decoded token payload (claims) if valid; otherwise, an empty map
     * @throws Exception if token structure is invalid or validation fails
     */
    private Map<String, Object> validateToken(String token) throws Exception {
        try {
            String[] tokenElements = token.split("\\.");
            String header = tokenElements[0];
            String body = tokenElements[1];
            String signature = tokenElements[2];
            String payLoad = header + CommonConstants.DOT_SEPARATOR + body;
            Map<Object, Object> headerData = mapper.readValue(new String(decodeFromBase64(header)), Map.class);
            String keyId = headerData.get("kid").toString();
            KeyData keyData = keyManager.getPublicKey(keyId);
            if (keyData == null) {
                throw new Exception("Public key not found for keyId: " + keyId);
            }
            boolean isValid = CryptoUtil.verifyRSASign(
                    payLoad,
                    decodeFromBase64(signature),
                    keyData.getPublicKey(),
                    CommonConstants.SHA_256_WITH_RSA);
            if (isValid) {
                Map<String, Object> tokenBody = mapper.readValue(new String(decodeFromBase64(body)), Map.class);
                boolean isExp = isExpired((Integer) tokenBody.get("exp"));
                if (isExp) {
                    throw new Exception("Expired auth token is received.");
                }
                return tokenBody;
            } else {
                throw new Exception("Invalid auth token is received.");
            }
        } catch (Exception e) {
            log.warn("Failed to validate the user token. Exception: ", e);
        }
        return Collections.emptyMap();
    }

    /**
     * Verifies a user's access token and extracts the user ID from the "sub" (subject) claim.
     * <p>
     * This method validates the token and checks the issuer claim. If valid, it extracts
     * the user ID from the "sub" claim. The user ID is expected to be in a format like
     * "prefix:userId", and only the part after the last colon is returned.
     * </p>
     *
     * @param token the JWT access token to be verified
     * @return the extracted user ID if the token is valid; otherwise, "UNAUTHORIZED"
     */
    public String verifyUserToken(String token) {
        String userId = CommonConstants._UNAUTHORIZED;
        try {
            Map<String, Object> payload = validateToken(token);
            if (MapUtils.isNotEmpty(payload) && checkIss((String) payload.get("iss"))) {
                userId = (String) payload.get(CommonConstants.SUB);
                if (StringUtils.hasLength(userId)) {
                    int pos = userId.lastIndexOf(":");
                    userId = userId.substring(pos + 1);
                }
            }
        } catch (Exception ex) {
            log.error("Exception in verifyUserAccessToken: verify ", ex);
        }
        return userId;
    }

    /**
     * Validates that the token's issuer (iss) claim matches the expected SSO realm URL.
     * <p>
     * The expected issuer URL is constructed from the SSO_URL and SSO_REALM properties
     * in the format: {SSO_URL}realms/{SSO_REALM}
     * </p>
     *
     * @param iss the issuer claim value from the JWT token
     * @return {@code true} if the issuer matches the expected realm URL (case-insensitive),
     *         {@code false} otherwise or if realm URL is not configured
     */
    private boolean checkIss(String iss) {
        String realmUrl = cache.getProperty(CommonConstants.SSO_URL) + "realms/"
                + cache.getProperty(CommonConstants.SSO_REALM);
        if (!StringUtils.hasLength(realmUrl))
            return false;
        return (realmUrl.equalsIgnoreCase(iss));
    }

    /**
     * Determines if a token has expired by comparing the expiration time with current time.
     * <p>
     * The expiration time is compared against the current system time in UTC.
     * If the token is expired, a warning is logged with both the current time and
     * the token's expiration time.
     * </p>
     *
     * @param expiration the token's expiration time from the "exp" claim (seconds since Unix epoch)
     * @return {@code true} if the current time is greater than the expiration time, {@code false} otherwise
     */
    private boolean isExpired(Integer expiration) {
        long currentTime = Instant.now().getEpochSecond();
        boolean retValue = (currentTime > expiration);
        if (retValue) {
            log.warn("Received expired auth token request. Current time: {}, Token expire time: {}",
                    currentTime, expiration);
        }
        return retValue;
    }

    /**
     * Decodes a Base64-encoded string (typically from a JWT component).
     * <p>
     * This method uses {@link Base64Util} to handle URL-safe Base64 decoding,
     * which is the standard encoding format for JWT tokens.
     * </p>
     *
     * @param data the Base64-encoded string to decode (from JWT header, payload, or signature)
     * @return the decoded byte array
     */
    private byte[] decodeFromBase64(String data) {
        return Base64Util.decode(data, 11);
    }

    /**
     * Extracts the user ID from a JWT access token.
     * <p>
     * This is a convenience method that validates the token and extracts the user ID.
     * If the token is null, invalid, or results in "UNAUTHORIZED", this method returns null.
     * Exceptions during token processing are logged and null is returned.
     * </p>
     *
     * @param accessToken the JWT access token from which to extract the user ID
     * @return the extracted user ID if the token is valid, or {@code null} if extraction fails
     */
    public String fetchUserIdFromAccessToken(String accessToken) {
        String clientAccessTokenId = null;
        if (accessToken != null) {
            try {
                clientAccessTokenId = verifyUserToken(accessToken);
                if (CommonConstants._UNAUTHORIZED.equalsIgnoreCase(clientAccessTokenId)) {
                    clientAccessTokenId = null;
                }
            } catch (Exception ex) {
                String errMsg = "Exception occurred while fetching the userid from the access token. Exception: "
                        + ex.getMessage();
                log.error(errMsg, ex);
                clientAccessTokenId = null;
            }
        }
        return clientAccessTokenId;
    }

    /**
     * Extracts the user ID from a JWT access token with error reporting to the API response.
     * <p>
     * This method validates the token and extracts the user ID. Unlike the single-parameter
     * variant, this method updates the provided {@link ApiResponse} object with appropriate
     * error information if validation fails:
     * <ul>
     *   <li>If the token is expired: sets response code to UNAUTHORIZED with an "expired" message</li>
     *   <li>If validation fails due to exceptions: sets response code to INTERNAL_SERVER_ERROR</li>
     * </ul>
     * </p>
     *
     * @param accessToken the JWT access token from which to extract the user ID
     * @param response the ApiResponse object to populate with error details if validation fails
     * @return the extracted user ID if the token is valid, or {@code null} if extraction fails
     */
    public String fetchUserIdFromAccessToken(String accessToken, ApiResponse response) {
        String clientAccessTokenId = null;
        if (accessToken != null) {
            try {
                clientAccessTokenId = verifyUserToken(accessToken);
                if (CommonConstants._UNAUTHORIZED.equalsIgnoreCase(clientAccessTokenId)) {
                    response.getParams().setStatus(CommonConstants.FAILED);
                    response.getParams().setErrMsg(CommonConstants.ACCESS_TOKEN_IS_EXPIRED);
                    response.setResponseCode(HttpStatus.UNAUTHORIZED);
                    clientAccessTokenId = null;
                }
            } catch (Exception ex) {
                String errMsg = "Exception occurred while fetching the userid from the access token. Exception: "
                        + ex.getMessage();
                log.error(errMsg, ex);
                response.getParams().setStatus(CommonConstants.FAILED);
                response.getParams().setErrMsg(CommonConstants.ACCESS_TOKEN_VALIDATION_FAILED);
                response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);
                clientAccessTokenId = null;
            }
        }
        return clientAccessTokenId;
    }

    /**
     * Extracts and returns the complete payload (claims) from a JWT access token.
     * <p>
     * This method validates the token (including signature verification, expiration check,
     * and issuer validation) and returns all claims from the token payload. This is useful
     * when you need access to multiple claims beyond just the user ID.
     * </p>
     * <p>
     * The returned map contains standard JWT claims such as:
     * <ul>
     *   <li>"sub" - subject (user identifier)</li>
     *   <li>"iss" - issuer</li>
     *   <li>"exp" - expiration time</li>
     *   <li>"iat" - issued at time</li>
     *   <li>Any custom claims included in the token</li>
     * </ul>
     * </p>
     *
     * @param token the JWT access token to validate and parse
     * @return a map containing all claims from the token payload if valid; otherwise, an empty map
     */
    public Map<String, Object> extractTokenPayload(String token) {
        Map<String, Object> tokenPayload = new HashMap<>();
        try {
            Map<String, Object> payload = validateToken(token);
            if (MapUtils.isNotEmpty(payload) && checkIss((String) payload.get("iss"))) {
                tokenPayload = payload;
            }
        } catch (Exception ex) {
            log.error("Exception in extractTokenPayload: ", ex);
        }
        return tokenPayload;
    }

    public UserDetails fetchUserDetailsFromToken(String accessToken) {
        // Initialize clientAccessTokenId to null
        UserDetails userDetails = new UserDetails();
        // Check if the accessToken is not null
        if (StringUtils.hasLength(accessToken)) {
            String userId = CommonConstants._UNAUTHORIZED;
            try {
                Map<String, Object> payload = validateToken(accessToken);
                if (MapUtils.isNotEmpty(payload) && checkIss((String) payload.get(CommonConstants.ISS))) {
                    String sub = (String) payload.get(CommonConstants.SUB);
                    if (StringUtils.hasLength(sub)) {
                        int pos = sub.lastIndexOf(":");
                        userId = sub.substring(pos + 1);
                    }
                    userDetails.setUserId(userId);

                    Object nameObj = payload.get(CommonConstants.NAME);
                    if (nameObj != null && StringUtils.hasLength(nameObj.toString())) {
                        userDetails.setName(nameObj.toString());
                    }

                    Object orgObj = payload.get(CommonConstants.ORG);
                    if (orgObj != null && StringUtils.hasLength(orgObj.toString())) {
                        userDetails.setOrg(orgObj.toString());
                    }

                    Object groupObj = payload.get(CommonConstants.GROUP);
                    if (groupObj != null && StringUtils.hasLength(groupObj.toString())) {
                        userDetails.setGroup(groupObj.toString());
                    }

                    Object designationObj = payload.get(CommonConstants.DESIGNATION);
                    if (designationObj != null && StringUtils.hasLength(designationObj.toString())) {
                        userDetails.setDesignation(designationObj.toString());
                    }

                    Object userRolesObj = payload.get(CommonConstants.USER_ROLES);
                    if (userRolesObj instanceof List) {
                        List<?> list = (List<?>) userRolesObj;
                        if (!list.isEmpty()) {
                            List<String> roles = new ArrayList<>();
                            for (Object item : list) {
                                if (item != null && StringUtils.hasLength(item.toString())) {
                                    roles.add(item.toString());
                                }
                            }
                            if (!roles.isEmpty()) {
                                userDetails.setUserRoles(roles);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.error("Exception in fetchUserDetailsFromToken: error: {}", ex.getMessage(), ex);
            }
        }
        return userDetails;
    }
}
