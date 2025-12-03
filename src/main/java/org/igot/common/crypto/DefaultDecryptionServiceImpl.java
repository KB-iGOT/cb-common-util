package org.igot.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.igot.common.CommonConstants;
import org.igot.common.CustomException;
import org.igot.common.PropertiesCache;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of the {@link DecryptionService} interface.
 *
 * <p>This class provides AES-based decryption functionality with support for various data types
 * including Strings, Maps, and Lists. It uses BASE64 encoding/decoding and performs iterative
 * decryption for enhanced security.
 *
 * <p>Key Features:
 * <ul>
 *   <li>AES encryption algorithm with configurable key</li>
 *   <li>BASE64 decoding for encrypted data</li>
 *   <li>Multiple decryption iterations (default: 3) for enhanced security</li>
 *   <li>Property-based configuration for encryption key and enabled state</li>
 *   <li>Graceful error handling with optional exception throwing</li>
 *   <li>Automatic initialization on application startup via {@code @PostConstruct}</li>
 * </ul>
 *
 * <p>Configuration Properties:
 * <ul>
 *   <li>{@code SUNBIRD_ENCRYPTION}: Controls whether decryption is enabled ("ON") or disabled</li>
 *   <li>{@code ENCRYPTION_KEY}: The salt value used for generating the decryption key</li>
 * </ul>
 *
 * <p>Thread Safety: This implementation initializes the Cipher instance once during
 * {@code @PostConstruct} and reuses it across all decryption operations. Ensure proper
 * synchronization if used in multi-threaded environments.
 *
 * @version 1.0
 * @see DecryptionService
 * @see PropertiesCache
 */
@Component
@Slf4j
public class DefaultDecryptionServiceImpl implements DecryptionService {
    /** Cache for accessing application configuration properties. */
    private final PropertiesCache propertiesCache;

    /** The encryption key retrieved from configuration properties. */
    private static String encryption_key = "";

    /** Flag indicating whether Sunbird encryption is enabled. */
    private String sunbirdEncryption = "";

    /** The Cipher instance used for all decryption operations. */
    private static Cipher c;

    /**
     * Constructs a new DefaultDecryptionServiceImpl with the specified properties cache.
     *
     * @param propertiesCache the properties cache for accessing configuration values
     */
    public DefaultDecryptionServiceImpl(PropertiesCache propertiesCache) {
        this.propertiesCache = propertiesCache;
    }

    /**
     * Initializes the decryption service after dependency injection.
     *
     * <p>This method is automatically invoked after the bean is constructed and dependencies
     * are injected. It performs the following initialization steps:
     * <ol>
     *   <li>Retrieves the SUNBIRD_ENCRYPTION property to determine if decryption is enabled</li>
     *   <li>Loads the encryption key (salt) from configuration</li>
     *   <li>Generates the AES key from the configured key value</li>
     *   <li>Initializes the Cipher instance in DECRYPT_MODE</li>
     * </ol>
     *
     * <p>Any exceptions during initialization are logged but not propagated, allowing the
     * application to start even if decryption initialization fails.
     */
    @PostConstruct
    public void init() {
        try {
            sunbirdEncryption = propertiesCache.getProperty(CommonConstants.SUNBIRD_ENCRYPTION);
            encryption_key = getSalt();
            Key key = generateKey();
            c = Cipher.getInstance(ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, key);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> decryptData(Map<String, Object> data) {
        if (CommonConstants.ON.equalsIgnoreCase(sunbirdEncryption)) {
            if (data == null) {
                return data;
            }
            Iterator<Map.Entry<String, Object>> itr = data.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, Object> entry = itr.next();
                if (!(entry.getValue() instanceof Map || entry.getValue() instanceof List)
                        && null != entry.getValue()) {
                    data.put(entry.getKey(), decrypt(entry.getValue() + "", false));
                }
            }
        }
        return data;
    }

    @Override
    public List<Map<String, Object>> decryptData(
            List<Map<String, Object>> data) {
        if (CommonConstants.ON.equalsIgnoreCase(sunbirdEncryption)) {
            if (data == null || data.isEmpty()) {
                return data;
            }

            for (Map<String, Object> map : data) {
                decryptData(map);
            }
        }
        return data;
    }

    @Override
    public String decryptData(String data) {
        return decryptData(data, false);
    }

    @Override
    public String decryptData(String data, boolean throwExceptionOnFailure) {
        if (CommonConstants.ON.equalsIgnoreCase(sunbirdEncryption)) {
            if (!StringUtils.hasText(data)) {
                return data;
            } else {
                return decrypt(data, throwExceptionOnFailure);
            }
        } else {
            return data;
        }
    }

    /**
     * Performs the actual decryption operation on the given encrypted value.
     *
     * <p>This internal method handles the core decryption logic using the following steps:
     * <ol>
     *   <li>Decodes the BASE64-encoded encrypted string</li>
     *   <li>Performs AES decryption using the initialized Cipher</li>
     *   <li>Removes the salt prefix from the decrypted value</li>
     *   <li>Repeats the process for the configured number of iterations</li>
     * </ol>
     *
     * <p>If decryption fails (e.g., due to invalid encrypted data, masked values, or corrupted input),
     * the behavior depends on the {@code throwExceptionOnFailure} parameter.
     *
     * @param value the encrypted string value to decrypt
     * @param throwExceptionOnFailure if {@code true}, throws a CustomException on failure;
     *                                if {@code false}, logs the error and returns the original value
     * @return the decrypted string, or the original value if decryption fails and
     *         throwExceptionOnFailure is {@code false}
     * @throws CustomException if decryption fails and throwExceptionOnFailure is {@code true}
     */
    public String decrypt(
            String value, boolean throwExceptionOnFailure) {
        try {
            String dValue = null;
            String valueToDecrypt = value.trim();
            for (int i = 0; i < ITERATIONS; i++) {
                byte[] decordedValue = new BASE64Decoder().decodeBuffer(valueToDecrypt);
                byte[] decValue = c.doFinal(decordedValue);
                dValue =
                        new String(decValue, StandardCharsets.UTF_8).substring(encryption_key.length());
                valueToDecrypt = dValue;
            }
            return dValue;
        } catch (Exception ex) {
            // This could happen with masked email and phone number. Not others.
            log.error("DefaultDecryptionServiceImpl:decrypt: ignorable errorMsg = ", ex);
            if (throwExceptionOnFailure) {
                log.info("Throwing exception error upon explicit ask by callers for value " + value);
                throw new CustomException(CommonConstants.SERVER_ERROR, "Decryption failed", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return value;
    }

    /**
     * Generates the AES secret key from the configured key value.
     *
     * <p>This method creates a {@link SecretKeySpec} using the key bytes defined in
     * {@link DecryptionService#keyValue} and the AES algorithm.
     *
     * @return the generated AES secret key
     */
    private Key generateKey() {
        return new SecretKeySpec(keyValue, ALGORITHM);
    }

    /**
     * Retrieves the salt (encryption key) from the configuration properties.
     *
     * <p>This method attempts to load the encryption key in the following order:
     * <ol>
     *   <li>Returns the cached encryption key if already loaded</li>
     *   <li>Retrieves the key from the ENCRYPTION_KEY property in the properties cache</li>
     *   <li>Throws a CustomException if no valid encryption key is found</li>
     * </ol>
     *
     * <p>The encryption key is required for the decryption process and must be configured
     * in the application properties.
     *
     * @return the encryption key (salt) used for decryption
     * @throws CustomException if the encryption key is not configured in the properties
     */
    public String getSalt() {
        if (StringUtils.hasText(encryption_key)) {
            return encryption_key;
        } else {
            encryption_key = propertiesCache.getProperty(CommonConstants.ENCRYPTION_KEY);
            if (!StringUtils.hasText(encryption_key)) {
                log.info("Salt value is not provided by Env");
                encryption_key = propertiesCache.getProperty(CommonConstants.ENCRYPTION_KEY);
            }
        }
        if (!StringUtils.hasText(encryption_key)) {
            log.info("throwing exception for invalid salt");
            throw new CustomException(CommonConstants.SERVER_ERROR, "Encryption key is not configured", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return encryption_key;
    }
}
