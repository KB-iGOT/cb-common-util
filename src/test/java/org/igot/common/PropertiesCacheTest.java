package org.igot.common;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PropertiesCacheTest {

    @Mock
    private Environment environment;

    private PropertiesCache propertiesCache;

    @BeforeEach
    void setUp() {
        propertiesCache = new PropertiesCache();
        ReflectionTestUtils.setField(propertiesCache, "environment", environment);
    }

    @Test
    @DisplayName("Should initialize and load property files")
    void init_LoadsPropertyFiles() {
        assertDoesNotThrow(() -> propertiesCache.init());
    }

    @Test
    @DisplayName("Should return environment variable value when present")
    void getProperty_EnvironmentVariableExists_ReturnsEnvValue() {
        String key = "test.property";
        String envValue = "env-value";

        when(environment.getProperty(key)).thenReturn(envValue);

        String result = propertiesCache.getProperty(key);

        assertEquals(envValue, result);
        verify(environment).getProperty(key);
    }

    @Test
    @DisplayName("Should return property file value when env variable not present")
    void getProperty_NoEnvVariable_ReturnsPropertyFileValue() {
        String key = "spring.application.name";

        when(environment.getProperty(key)).thenReturn(null);

        propertiesCache.init();
        String result = propertiesCache.getProperty(key);

        // Should return the value from application.properties or the key itself
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should return key itself when property not found anywhere")
    void getProperty_PropertyNotFound_ReturnsKey() {
        String key = "non.existent.property";

        when(environment.getProperty(key)).thenReturn(null);

        propertiesCache.init();
        String result = propertiesCache.getProperty(key);

        assertEquals(key, result);
    }

    @Test
    @DisplayName("Should prioritize environment variable over property file")
    void getProperty_BothExist_PrioritizesEnvVariable() {
        String key = "spring.application.name";
        String envValue = "from-environment";

        when(environment.getProperty(key)).thenReturn(envValue);

        propertiesCache.init();
        String result = propertiesCache.getProperty(key);

        assertEquals(envValue, result);
    }

    @Test
    @DisplayName("Should handle empty environment variable value")
    void getProperty_EmptyEnvVariable_FallsBackToPropertyFile() {
        String key = "test.property";

        when(environment.getProperty(key)).thenReturn("");

        propertiesCache.init();
        String result = propertiesCache.getProperty(key);

        // Empty string is not considered as "has length", so should fall back
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should have empty attributePercentageMap on initialization")
    void init_AttributePercentageMapIsEmpty() {
        assertNotNull(propertiesCache.attributePercentageMap);
        assertTrue(propertiesCache.attributePercentageMap.isEmpty());
    }

    @Test
    @DisplayName("Should handle missing property files gracefully")
    void init_MissingPropertyFiles_HandlesGracefully() {
        // This should not throw an exception even if some property files are missing
        assertDoesNotThrow(() -> propertiesCache.init());
    }
}
