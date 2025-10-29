package org.igot.common.config;

import org.igot.common.util.AccessTokenValidator;
import org.igot.common.util.KeyManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CommonProperties.class)
public class CommonUtilAutoConfiguration {
    private final CommonProperties commonProperties;

    public CommonUtilAutoConfiguration(CommonProperties commonProperties) {
        this.commonProperties = commonProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public KeyManager keyManager() {
        return new KeyManager(commonProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessTokenValidator accessTokenValidator(KeyManager keyManager) {
        return new AccessTokenValidator(keyManager, commonProperties);
    }
}
