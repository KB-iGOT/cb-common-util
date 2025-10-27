package org.igot.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "org.igot.jwt.validator")
public class CommonProperties {
    private String ssoUrl;
    private String ssoRealm;
    private String accessTokenPublicKeyBasepath;
    
    // Getters and Setters
    public String getSsoUrl() {
        return ssoUrl;
    }
    
    public void setSsoUrl(String ssoUrl) {
        this.ssoUrl = ssoUrl;
    }
    
    public String getSsoRealm() {
        return ssoRealm;
    }
    
    public void setSsoRealm(String ssoRealm) {
        this.ssoRealm = ssoRealm;
    }
    
    public String getAccessTokenPublicKeyBasepath() {
        return accessTokenPublicKeyBasepath;
    }
    
    public void setAccessTokenPublicKeyBasepath(String accessTokenPublicKeyBasepath) {
        this.accessTokenPublicKeyBasepath = accessTokenPublicKeyBasepath;
    }
}
