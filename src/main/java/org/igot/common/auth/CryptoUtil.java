package org.igot.common.auth;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

/**
 * Utility class providing cryptographic operations for digital signature verification.
 * <p>
 * This class offers methods for verifying RSA digital signatures, commonly used in
 * JWT (JSON Web Token) validation and other security-related operations.
 * </p>
 * 
 * @author Karthikeyan R (karthik-tarento)
 */
public class CryptoUtil {
    /**
     * Character encoding used for converting payload strings to bytes.
     * US-ASCII is used for compatibility with standard JWT implementations.
     */
    private static final Charset US_ASCII = Charset.forName("US-ASCII");

    /**
     * Verifies an RSA digital signature against a payload using the specified public key and algorithm.
     * <p>
     * This method is typically used to verify JWT signatures or other cryptographically signed data.
     * The payload is converted to bytes using US-ASCII encoding before verification.
     * </p>
     *
     * @param payLoad the string payload that was signed (e.g., JWT header and claims)
     * @param signature the signature bytes to verify
     * @param key the RSA public key used for verification
     * @param algorithm the signature algorithm to use (e.g., "SHA256withRSA", "SHA512withRSA")
     * @return {@code true} if the signature is valid for the given payload, {@code false} otherwise
     *         (including cases where the algorithm is unsupported, key is invalid, or verification fails)
     */
    public static boolean verifyRSASign(String payLoad, byte[] signature, PublicKey key, String algorithm) {
        Signature sign;
        try {
            sign = Signature.getInstance(algorithm);
            sign.initVerify(key);
            sign.update(payLoad.getBytes(US_ASCII));
            return sign.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            return false;
        }
    }
}
