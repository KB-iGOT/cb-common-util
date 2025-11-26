package org.igot.common.cassandra;

import java.util.List;
import java.util.Map;

import org.igot.common.ApiResponse;

/**
 * Interface for Cassandra database operations.
 * Provides methods for CRUD operations on Cassandra tables.
 */
public interface CassandraOperation {
    /**
     * Inserts a record into Cassandra.
     *
     * @param keyspaceName the name of the keyspace containing the table
     * @param tableName the name of the table into which to insert the record
     * @param request a map representing the record to insert, where keys are column names
     * @return an ApiResponse object containing the result of the insertion operation
     */
    Object insertRecord(String keyspaceName, String tableName, Map<String, Object> request);

    /**
     * Retrieves records from Cassandra based on specified properties and filters.
     *
     * @param keyspaceName the name of the keyspace containing the table
     * @param tableName the name of the table from which to retrieve records
     * @param propertyMap a map of column names and values to filter by (WHERE clause)
     * @param fields list of specific columns to retrieve, or null to retrieve all columns
     * @param limit maximum number of records to retrieve, or null for no limit
     * @return a list of maps, where each map represents a record with column names as keys
     */
    List<Map<String, Object>> getRecordsByProperties(String keyspaceName, String tableName,
            Map<String, Object> propertyMap, List<String> fields, Integer limit);

    /**
     * Updates a record in Cassandra identified by a composite key.
     *
     * @param keyspaceName the name of the keyspace containing the table
     * @param tableName the name of the table in which to update the record
     * @param updateAttributes a map of column names and new values to update
     * @param compositeKey a map representing the composite primary key identifying the record
     * @return a map containing the result of the update operation with response status
     * @throws RuntimeException if the update operation fails
     */
    Map<String, Object> updateRecord(String keyspaceName, String tableName,
            Map<String, Object> updateAttributes,
            Map<String, Object> compositeKey);

    /**
     * Inserts multiple records into Cassandra in batches.
     * Records are processed in batches for optimal performance.
     *
     * @param keyspaceName the name of the keyspace containing the table
     * @param tableName the name of the table into which to insert records
     * @param request a list of maps, where each map represents a record to insert
     * @return an ApiResponse object containing the result of the bulk insertion operation
     */
    ApiResponse insertBulkRecord(String keyspaceName, String tableName, List<Map<String, Object>> request);

    /**
     * Deletes a record from Cassandra identified by a composite key.
     *
     * @param keyspaceName the name of the keyspace containing the table
     * @param tableName the name of the table from which to delete the record
     * @param keyMap a map representing the composite primary key identifying the record to delete
     * @throws RuntimeException if the delete operation fails
     */
    void deleteRecord(String keyspaceName, String tableName, Map<String, Object> keyMap);

    /**
     * Inserts a record into Cassandra with a composite primary key.
     * This method builds a complete record by combining the primary key, composite key parts,
     * and other fields before insertion.
     *
     * @param keyspaceName the name of the keyspace containing the table
     * @param tableName the name of the table into which to insert the record
     * @param primaryKeyColumn the name of the primary key column
     * @param primaryKeyValue the value of the primary key
     * @param compositeKey a map representing additional composite key fields and their values (can be null)
     * @param otherFields a map representing other non-key fields and their values to be inserted (can be null)
     * @return an ApiResponse object containing the result of the insertion operation
     */
    Object insertRecord(
            String keyspaceName,
            String tableName,
            String primaryKeyColumn,
            String primaryKeyValue,
            Map<String, Object> compositeKey,
            Map<String, Object> otherFields);
}
