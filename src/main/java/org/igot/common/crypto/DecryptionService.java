package org.igot.common.crypto;

import java.util.List;
import java.util.Map;

/**
 * Service interface for data decryption operations.
 *
 * <p>This service provides methods to decrypt encrypted data in various formats including
 * Strings, Maps, and Lists of Maps. It uses AES (Advanced Encryption Standard) algorithm
 * with a configurable encryption key and performs multiple iterations for enhanced security.
 *
 * <p>The decryption is controlled by the {@code SUNBIRD_ENCRYPTION} property. When set to "ON",
 * decryption operations are performed; otherwise, data is returned as-is.
 *
 * <p>Key Features:
 * <ul>
 *   <li>Supports decryption of primitive types, Strings, and nested Maps</li>
 *   <li>Handles single values, key-value pairs, and collections</li>
 *   <li>Configurable exception handling behavior</li>
 *   <li>Uses iterative decryption for enhanced security</li>
 * </ul>
 *
 * @version 1.0
 * @see DefaultDecryptionServiceImpl
 */
public interface DecryptionService {
    /** The encryption algorithm used for decryption operations (AES - Advanced Encryption Standard). */
    String ALGORITHM = "AES";

    /** The number of decryption iterations to perform for enhanced security. */
    int ITERATIONS = 3;

    /** The default key value used for generating the encryption/decryption key. */
    byte[] keyValue =
            new byte[] {'T', 'h', 'i', 's', 'A', 's', 'I', 'S', 'e', 'r', 'c', 'e', 'K', 't', 'e', 'y'};

    /**
     * Decrypts a map of key-value pairs where values can be primitives, Strings, or nested Maps.
     *
     * <p>This method iterates through all entries in the provided map and decrypts the values.
     * Only primitive types and String values are decrypted; Map and List values are skipped.
     * Nested maps within values will also have their primitive and String values decrypted.
     *
     * <p>Decryption is only performed when the {@code SUNBIRD_ENCRYPTION} property is set to "ON".
     * If encryption is disabled or the input is null, the data is returned unchanged.
     *
     * @param data the map containing key-value pairs to decrypt, where values can be primitives,
     *             Strings, or nested Maps. Can be null.
     * @return the same map with decrypted values, or the original map if decryption is disabled
     *         or input is null
     */
    Map<String, Object> decryptData(Map<String, Object> data);

    /**
     * Decrypts a list of maps, where each map contains key-value pairs to be decrypted.
     *
     * <p>This method iterates through each map in the list and decrypts its values using the
     * {@link #decryptData(Map)} method. Values inside each map can be primitives, Strings,
     * or nested maps containing primitive or String values.
     *
     * <p>Decryption is only performed when the {@code SUNBIRD_ENCRYPTION} property is set to "ON".
     * If encryption is disabled, the input is null, or the list is empty, the data is returned unchanged.
     *
     * @param data the list of maps to decrypt, where each map contains key-value pairs. Can be null or empty.
     * @return the same list with all maps having decrypted values, or the original list if decryption
     *         is disabled or input is null/empty
     */
    List<Map<String, Object>> decryptData(List<Map<String, Object>> data);

    /**
     * Decrypts the given String data.
     *
     * <p>This method performs decryption on the input string using the configured encryption key
     * and multiple iterations (as defined by {@link #ITERATIONS}). Decryption is only performed
     * when the {@code SUNBIRD_ENCRYPTION} property is set to "ON".
     *
     * <p>This is a convenience method that calls {@link #decryptData(String, boolean)} with
     * {@code throwExceptionOnFailure} set to {@code false}, meaning decryption failures are
     * logged but not thrown, and the original data is returned.
     *
     * @param data the input string to decrypt. Can be null or empty.
     * @return the decrypted string if decryption is enabled and successful; otherwise,
     *         the original input data
     */
    String decryptData(String data);

    /**
     * Decrypts the given String data with configurable exception handling.
     *
     * <p>This method performs decryption on the input string using the configured encryption key
     * and multiple iterations (as defined by {@link #ITERATIONS}). Decryption is only performed
     * when the {@code SUNBIRD_ENCRYPTION} property is set to "ON".
     *
     * <p>The {@code throwExceptionOnFailure} parameter controls the error handling behavior:
     * <ul>
     *   <li>If {@code true}: throws a {@link CustomException} when decryption fails</li>
     *   <li>If {@code false}: logs the error and returns the original input data</li>
     * </ul>
     *
     * <p>Note: Decryption failures can occur with masked email addresses, phone numbers,
     * or invalid encrypted data.
     *
     * @param data the input string to decrypt. Can be null or empty.
     * @param throwExceptionOnFailure if {@code true}, throws an exception on decryption failure;
     *                                if {@code false}, returns the original data on failure
     * @return the decrypted string if decryption is enabled and successful; otherwise,
     *         the original input data (when throwExceptionOnFailure is false)
     * @throws CustomException if decryption fails and throwExceptionOnFailure is {@code true}
     */
    String decryptData(String data, boolean throwExceptionOnFailure);
}
