package org.igot.common.cassandra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.igot.common.CommonConstants;
import org.igot.common.PropertiesCache;
import org.springframework.stereotype.Component;

import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

/**
 * Utility class for Cassandra database operations.
 * Provides helper methods for generating CQL statements and processing query results.
 */
@Component
public class CassandraUtil {
    private final PropertiesCache propertiesCache;

    /**
     * Constructs a new CassandraUtil instance.
     *
     * @param propertiesCache the properties cache for column name mapping
     */
    public CassandraUtil(PropertiesCache propertiesCache) {
        this.propertiesCache = propertiesCache;
    }

    /**
     * Generates a parameterized INSERT statement for Cassandra.
     * Creates a CQL prepared statement with placeholders (?) for values.
     *
     * @param keyspaceName the name of the Cassandra keyspace
     * @param tableName the name of the table
     * @param map a map containing column names as keys and their values
     * @return a parameterized INSERT statement string
     *         (e.g., "INSERT INTO keyspace.table (col1,col2) VALUES (?,?);")
     */
    public String getPreparedStatement(
            String keyspaceName, String tableName, Map<String, Object> map) {
        Set<String> keySet = map.keySet();
        int columnCount = keySet.size();
        StringBuilder query = new StringBuilder(128);
        query.append(CommonConstants.INSERT_INTO)
             .append(keyspaceName)
             .append(CommonConstants.DOT)
             .append(tableName)
             .append(CommonConstants.OPEN_BRACE)
             .append(String.join(CommonConstants.COMMA, keySet))
             .append(CommonConstants.VALUES_WITH_BRACE);
        for (int i = 0; i < columnCount; i++) {
            if (i > 0) {
                query.append(CommonConstants.COMMA);
            }
            query.append(CommonConstants.QUE_MARK);
        }
        query.append(CommonConstants.CLOSING_BRACE);
        return query.toString();
    }

    /**
     * Converts a Cassandra ResultSet into a list of maps.
     * Each row in the result set is transformed into a map with mapped column names as keys.
     *
     * @param results the Cassandra query result set
     * @return a list of maps, where each map represents a row with column names mapped
     *         according to properties configuration
     */
    public List<Map<String, Object>> createResponse(ResultSet results) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        Map<String, String> columnsMapping = fetchColumnsMapping(results);
        int columnSize = columnsMapping.size();
        for (Row row : results) {
            Map<String, Object> rowMap = new HashMap<>(columnSize);
            columnsMapping.forEach((key, value) -> rowMap.put(key, row.getObject(value)));
            responseList.add(rowMap);
        }
        return responseList;
    }

    /**
     * Converts a Cassandra ResultSet into a map of maps, keyed by a specific column value.
     * Each row is transformed into a map and indexed by the value of the specified key column.
     * Only rows where the key column value is a String are included in the result.
     *
     * @param results the Cassandra query result set
     * @param key the column name to use as the key for the resulting map
     * @return a map where keys are String values from the specified column and values are
     *         maps representing the entire row with column names mapped according to
     *         properties configuration
     */
    public Map<String, Object> createResponse(ResultSet results, String key) {
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, String> columnsMapping = fetchColumnsMapping(results);
        int columnSize = columnsMapping.size();
        for (Row row : results) {
            Map<String, Object> rowMap = new HashMap<>(columnSize);
            columnsMapping.forEach((columnKey, columnValue) -> rowMap.put(columnKey, row.getObject(columnValue)));

            Object keyValue = rowMap.get(key);
            if (keyValue instanceof String) {
                responseMap.put((String) keyValue, rowMap);
            }
        }
        return responseMap;
    }

    /**
     * Creates a mapping between property names and Cassandra column names.
     * Retrieves the mapped property name for each column from the properties cache.
     * If no mapping is found, uses the original column name as the property name.
     *
     * @param results the Cassandra query result set containing column definitions
     * @return a map where keys are property names (from properties cache) and values are
     *         the actual Cassandra column names
     */
    public Map<String, String> fetchColumnsMapping(ResultSet results) {
        ColumnDefinitions columnDefinitions = results.getColumnDefinitions();
        int columnCount = columnDefinitions.size();
        Map<String, String> columnsMapping = new HashMap<>(columnCount);
        for (ColumnDefinition column : columnDefinitions) {
            String columnName = column.getName().asInternal();
            String property = propertiesCache.getProperty(columnName);
            if (property != null) {
                property = property.trim();
            }
            if (property == null || property.isEmpty()) {
                property = columnName;
            }
            columnsMapping.put(property, columnName);
        }
        return columnsMapping;
    }
}
