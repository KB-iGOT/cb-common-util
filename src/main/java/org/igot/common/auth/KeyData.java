package org.igot.common.auth;

import java.security.PublicKey;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Data holder for RSA public key information.
 * <p>
 * This class encapsulates a public key along with its identifier, typically used
 * for access token validation. The key ID corresponds to the filename of the
 * public key file loaded from the filesystem.
 * </p>
 *
 * @see KeyManager
 * 
 * @author Karthikeyan R (karthik-tarento)
 */
@Getter
@Setter
@AllArgsConstructor
public class KeyData {
    /**
     * The unique identifier for this key, typically the filename of the key file.
     */
    private String keyId;

    /**
     * The RSA public key used for token signature verification.
     */
    private PublicKey publicKey;
}
