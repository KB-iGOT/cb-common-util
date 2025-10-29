package org.igot.common.model;

import java.security.PublicKey;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class KeyData {
    private String keyId;
    private PublicKey publicKey;

    public KeyData(String keyId, PublicKey publicKey) {
        this.keyId = keyId;
        this.publicKey = publicKey;
    }
}
