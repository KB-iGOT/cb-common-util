package org.igot.common.auth;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.igot.common.CommonConstants;
import org.igot.common.PropertiesCache;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages RSA public keys used for access token validation.
 * <p>
 * This component loads and caches public keys from the filesystem during application startup.
 * Keys are stored in a map indexed by their filename (keyId) for quick retrieval during
 * token validation operations.
 * </p>
 * <p>
 * The base path for public keys is configured via the {@link CommonConstants#ACCESS_TOKEN_PUBLICKEY_BASEPATH}
 * property. All files in this directory (including subdirectories) are loaded as public keys.
 * </p>
 *
 * @see KeyData
 * @see PropertiesCache
 * 
 * @author Karthikeyan R (karthik-tarento)
 */
@Slf4j
@Component
public class KeyManager {

    private final PropertiesCache propertiesCache;

    /**
     * Cache of public keys indexed by key ID (filename).
     */
    private final Map<String, KeyData> keyMap = new HashMap<>();

    /**
     * Constructs a new KeyManager with the specified properties cache.
     *
     * @param propertiesCache the properties cache containing configuration values
     */
    public KeyManager(PropertiesCache propertiesCache) {
        this.propertiesCache = propertiesCache;
    }

    /**
     * Initializes the key manager by loading all public keys from the configured base path.
     * <p>
     * This method is automatically invoked after dependency injection is complete.
     * It walks through the directory tree starting from the base path, reads all regular
     * files as public keys, and stores them in the internal cache.
     * </p>
     * <p>
     * If any individual key file fails to load, an error is logged but the initialization
     * continues for remaining keys. If the base path itself is invalid or inaccessible,
     * an error is logged and the key map remains empty.
     * </p>
     */
    @PostConstruct
    public void init() {
        String basePath = propertiesCache.getProperty(CommonConstants.ACCESS_TOKEN_PUBLICKEY_BASEPATH);
        try (Stream<Path> walk = Files.walk(Paths.get(basePath))) {
            // Collect all regular files from the base path and its subdirectories
            List<String> result = walk.filter(Files::isRegularFile).map(x -> x.toString()).collect(Collectors.toList());
            result.forEach(
                    file -> {
                        try {
                            Path path = Paths.get(file);
                            String content = Files.readString(path, StandardCharsets.UTF_8);
                            // Create KeyData with filename as keyId
                            KeyData keyData = new KeyData(
                                    path.getFileName().toString(),
                                    loadPublicKey(content));
                            keyMap.put(path.getFileName().toString(), keyData);
                        } catch (Exception e) {
                            log.error("KeyManager:init: exception in reading public keys ", e);
                        }
                    });
        } catch (Exception e) {
            log.error("KeyManager:init: exception in loading publickeys ", e);
        }
    }

    /**
     * Retrieves the public key data associated with the specified key ID.
     * <p>
     * The key ID typically corresponds to the filename of the public key file
     * that was loaded during initialization.
     * </p>
     *
     * @param keyId the key identifier (filename) to look up
     * @return the {@link KeyData} object containing the public key, or {@code null} if not found
     */
    public KeyData getPublicKey(String keyId) {
        KeyData keyData = keyMap.get(keyId);
        if (keyData == null) {
            log.warn("Public key not found for keyId: {}", keyId);
        }
        return keyData;
    }

    /**
     * Loads a public key from PEM-formatted string content.
     * <p>
     * This method parses a PEM-encoded RSA public key by:
     * <ol>
     *   <li>Removing the BEGIN/END PUBLIC KEY headers and footers</li>
     *   <li>Removing line breaks and whitespace</li>
     *   <li>Base64 decoding the key content</li>
     *   <li>Creating an RSA PublicKey using X.509 encoding specification</li>
     * </ol>
     * </p>
     *
     * @param key the PEM-formatted public key string (including headers)
     * @return the decoded {@link PublicKey} object
     * @throws Exception if the key format is invalid or cannot be parsed
     */
    public static PublicKey loadPublicKey(String key) throws Exception {
        // Remove PEM headers/footers and whitespace
        String publicKey = key
                .replaceAll("(-+BEGIN PUBLIC KEY-+)", "")
                .replaceAll("(-+END PUBLIC KEY-+)", "")
                .replaceAll("[\\r\\n]+", "");

        // Decode the base64 content
        byte[] keyBytes = Base64Util.decode(publicKey, 0);

        // Generate the PublicKey from X509 specification
        X509EncodedKeySpec x509publicKey = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(x509publicKey);
    }
}