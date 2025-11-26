package org.igot.common.cassandra;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.igot.common.CommonConstants;
import org.igot.common.CustomException;
import org.igot.common.PropertiesCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel;
import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.metadata.Metadata;
import com.datastax.oss.driver.internal.core.time.AtomicTimestampGenerator;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CassandraConnectionManagerImpl implements CassandraConnectionManager {
    private static final Map<String, CqlSession> cassandraSessionMap = new ConcurrentHashMap<>(2);
    private static CqlSession session;

    @Autowired
    PropertiesCache propertiesCache;

    /**
     * Method invoked after bean creation for initialization
     */
    public CassandraConnectionManagerImpl() {
        // Initialize the connection and register shutdown hook
        registerShutdownHook();
        createCassandraConnection();
    }

    /**
     * Retrieves a session for the specified keyspace.
     * If a session for the keyspace already exists, returns it; otherwise, creates a new session.
     *
     * @param keyspaceName The keyspace for which to retrieve the session.
     * @return The session object for the specified keyspace.
     * @throws Exception 
     */
    @Override
    public CqlSession getSession(String keyspaceName) {
        // Check if session for keyspace already exists
        CqlSession currentSession = cassandraSessionMap.get(keyspaceName);
        if (currentSession != null&& !currentSession.isClosed()) {
            return currentSession;
        } else {
            // Create new session scoped to keyspace using the USE command
            CqlSession newSession = createCassandraConnectionWithKeySpaces(keyspaceName);
            cassandraSessionMap.put(keyspaceName, newSession);
            return newSession;
        }
    }

    /**
     * Creates a Cassandra connection based on properties
     */
    private CqlSession createCassandraConnectionWithKeySpaces(String keySpaceName) {
        try {
            // Load the properties required for connection
            
            String cassandraHost = propertiesCache.getProperty(CommonConstants.CASSANDRA_CONFIG_HOST);
            if (!StringUtils.hasLength(cassandraHost)) {
                throw new CustomException(
                        CommonConstants.ERROR,
                        "Cassandra host is not configured",
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
            List<String> hosts = Arrays.asList(cassandraHost.split(","));
            List<InetSocketAddress> contactPoints = hosts.stream()
                    .map(host -> new InetSocketAddress(host.trim(), 9042)) // Assuming default port 9042
                    .collect(Collectors.toList());
            List<String> contactPointsString = hosts.stream()
                    .map(host -> host.trim() + ":9042") // Ensure proper host:port format
                    .collect(Collectors.toList());
            DriverConfigLoader loader = DriverConfigLoader.programmaticBuilder()
                    .withStringList(DefaultDriverOption.CONTACT_POINTS, contactPointsString)
                    .withString(DefaultDriverOption.REQUEST_CONSISTENCY, getConsistencyLevel().name())
                    .withString(DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER, "datacenter1")
                    .withInt(DefaultDriverOption.CONNECTION_POOL_LOCAL_SIZE,
                            Integer.parseInt(propertiesCache.getProperty(CommonConstants.CORE_CONNECTIONS_PER_HOST_FOR_LOCAL)))
                    .withInt(DefaultDriverOption.CONNECTION_POOL_REMOTE_SIZE,
                            Integer.parseInt(propertiesCache.getProperty(CommonConstants.CORE_CONNECTIONS_PER_HOST_FOR_REMOTE)))
                    .withInt(DefaultDriverOption.HEARTBEAT_INTERVAL,
                            Integer.parseInt(propertiesCache.getProperty(CommonConstants.HEARTBEAT_INTERVAL)))
                    .withInt(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, 10000)
                    .withInt(DefaultDriverOption.REQUEST_TIMEOUT, 10000)
                    .withString(DefaultDriverOption.PROTOCOL_VERSION, ProtocolVersion.V4.toString())
                    .withClass(DefaultDriverOption.RETRY_POLICY_CLASS, com.datastax.oss.driver.internal.core.retry.DefaultRetryPolicy.class)
                    .withClass(DefaultDriverOption.TIMESTAMP_GENERATOR_CLASS, AtomicTimestampGenerator.class)
                    .build();
            CqlSession sessionWithKeyspaces;
            if (StringUtils.hasText(keySpaceName)) {
                sessionWithKeyspaces = CqlSession.builder()
                        .addContactPoints(contactPoints)
                        .withLocalDatacenter("datacenter1")
                        .withKeyspace(keySpaceName)
                        .withConfigLoader(loader)
                        .build();
            } else {
                sessionWithKeyspaces = CqlSession.builder()
                        .addContactPoints(contactPoints)
                        .withLocalDatacenter("datacenter1")
                        .withConfigLoader(loader)
                        .build();
            }
            log.info("Connected to the keyspaces: " + keySpaceName);
            // Get metadata and log cluster information
            final Metadata metadata = sessionWithKeyspaces.getMetadata();
            log.info(String.format("Connected to cluster: %s", metadata.getClusterName()));
            // Log nodes in the cluster
            for (Node host : metadata.getNodes().values()) {
                log.info(String.format("Datacenter: %s; Host: %s; Rack: %s", host.getDatacenter(), host.getEndPoint(), host.getRack()));
            }
            return sessionWithKeyspaces;
        } catch (Exception e) {
            log.error("Error while creating Cassandra connection", e);
            throw new CustomException(
                    CommonConstants.ERROR,
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void createCassandraConnection() {
        try {
            session = createCassandraConnectionWithKeySpaces(null);
        } catch (Exception e) {
            log.error("Error while creating Cassandra connection", e);
            throw new CustomException(
                    CommonConstants.ERROR,
                    e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves consistency level from properties
     *
     * @return -consistency level from properties
     */
    private ConsistencyLevel getConsistencyLevel() {
        String consistency = propertiesCache.getProperty(CommonConstants.SUNBIRD_CASSANDRA_CONSISTENCY_LEVEL);
        log.info("CassandraConnectionManagerImpl:getConsistencyLevel: level = " + consistency);
        if (!StringUtils.hasLength(consistency)) return null;

        try {
            return DefaultConsistencyLevel.valueOf(consistency.toUpperCase());
        } catch (IllegalArgumentException exception) {
            log.info("CassandraConnectionManagerImpl:getConsistencyLevel: Exception occurred with error message = "
                    + exception.getMessage());
        }
        return null;
    }



    /**
     * Registers a shutdown hook to clean-up resources
     */
    public static void registerShutdownHook() {
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new ResourceCleanUp());
        log.info("Cassandra ShutDownHook registered.");
    }

    /**
     * Cleans up Cassandra resources during shutdown
     */
    static class ResourceCleanUp extends Thread {
        @Override
        public void run() {
            try {
                log.info("Started resource cleanup for Cassandra.");
                for (Map.Entry<String, CqlSession> entry : cassandraSessionMap.entrySet()) {
                    entry.getValue().close();
                }
                if (session != null) {
                    session.close();
                }
                log.info("Completed resource cleanup for Cassandra.");
            } catch (Exception ex) {
                log.error("Error during resource cleanup", ex);
            }
        }
    }
}
