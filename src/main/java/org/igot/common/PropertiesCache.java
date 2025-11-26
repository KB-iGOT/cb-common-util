package org.igot.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Cache component for loading and managing application properties from multiple sources.
 * <p>
 * This class provides a centralized mechanism to load configuration properties from
 * various property files and the Spring Environment. It supports multiple property files
 * and provides a fallback hierarchy when retrieving property values.
 * </p>
 * <p>
 * The property resolution follows this order:
 * <ol>
 *   <li>Spring Environment properties (highest priority)</li>
 *   <li>Properties loaded from configured property files</li>
 *   <li>The key itself as a fallback (if not found in either source)</li>
 * </ol>
 * </p>
 *
 * @author Karthikeyan R (karthik-tarento)
 */
@Slf4j
@Component
public class PropertiesCache {
    /**
     * Spring Environment for accessing system and application properties.
     */
    @Autowired
    private Environment environment;

    /**
     * Thread-safe map for storing attribute percentage values.
     * This map can be used to store and retrieve percentage-based configuration values.
     */
    public final Map<String, Float> attributePercentageMap = new ConcurrentHashMap<>();

    /**
     * Array of property file names to be loaded during initialization.
     * These files are loaded from the classpath in the order specified.
     */
    private final String[] fileName = {
            "cassandra.config.properties",
            "cassandratablecolumn.properties",
            "application.properties",
            "customerror.properties"
    };

    /**
     * Properties object that holds all loaded configuration properties.
     */
    private final Properties configProp = new Properties();

    /**
     * Initializes the properties cache by loading all configured property files.
     * <p>
     * This method is automatically invoked after dependency injection is complete.
     * It loads each property file from the classpath and merges them into the
     * internal properties object. If a file is not found or fails to load,
     * appropriate warnings or errors are logged without interrupting the initialization.
     * </p>
     */
    @PostConstruct
    public void init() {
        for (String file : fileName) {
            try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(file)) {
                if (in != null) {
                    configProp.load(in);
                } else {
                    log.warn("Property file not found: {}", file);
                }
            } catch (IOException e) {
                log.error("Failed to load property file: {}", file, e);
            }
        }
    }

    /**
     * Retrieves a property value for the specified key with fallback support.
     * <p>
     * The method follows a hierarchical lookup strategy:
     * <ol>
     *   <li>First checks the Spring Environment for the property</li>
     *   <li>If not found or empty, checks the loaded properties files</li>
     *   <li>If still not found, returns the key itself as a fallback</li>
     * </ol>
     * </p>
     *
     * @param key the property key to look up
     * @return the property value from Environment, loaded properties, or the key itself if not found
     */
    public String getProperty(String key) {
        String value = environment.getProperty(key);
        if (StringUtils.hasLength(value))
            return value;
        return configProp.getProperty(key) != null ? configProp.getProperty(key) : key;
    }
}
