package org.igot.common.cassandra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.igot.common.ApiResponse;
import org.igot.common.CommonConstants;
import org.springframework.stereotype.Component;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BatchStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatementBuilder;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.DefaultBatchType;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.delete.Delete;
import com.datastax.oss.driver.api.querybuilder.relation.Relation;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import com.datastax.oss.driver.api.querybuilder.term.Term;
import com.datastax.oss.driver.api.querybuilder.update.Assignment;
import com.datastax.oss.driver.api.querybuilder.update.UpdateStart;
import com.datastax.oss.driver.api.querybuilder.update.UpdateWithAssignments;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of CassandraOperation interface providing concrete database operations.
 * This class handles all CRUD operations for Cassandra database including single record operations,
 * bulk operations, and query building.
 */
@Component
@Slf4j
public class CassandraOperationImpl implements CassandraOperation {
    private final CassandraConnectionManager connectionManager;
    private final CassandraUtil cassandraUtil;

    /**
     * Constructs a new CassandraOperationImpl instance.
     *
     * @param connectionManager the connection manager for obtaining Cassandra sessions
     * @param cassandraUtil utility class for Cassandra operations
     */
    public CassandraOperationImpl(CassandraConnectionManager connectionManager, CassandraUtil cassandraUtil) {
        this.connectionManager = connectionManager;
        this.cassandraUtil = cassandraUtil;
    }

    /**
     * Builds a SELECT query with optional WHERE clauses based on property filters.
     * Supports both equality and IN operator for list values.
     *
     * @param keyspaceName the name of the keyspace
     * @param tableName the name of the table
     * @param propertyMap map of column names to values for filtering (supports List for IN clause)
     * @param fields specific columns to retrieve, or null for all columns
     * @return a Select query builder instance
     */
    private Select processQuery(String keyspaceName, String tableName,
            Map<String, Object> propertyMap, List<String> fields) {
        Select select;
        if (CollectionUtils.isNotEmpty(fields)) {
            select = QueryBuilder.selectFrom(keyspaceName, tableName).columns(fields);
        } else {
            select = QueryBuilder.selectFrom(keyspaceName, tableName).all();
        }

        if (MapUtils.isEmpty(propertyMap)) {
            return select;
        }

        for (Map.Entry<String, Object> entry : propertyMap.entrySet()) {
            String columnName = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof List) {
                List<?> valueList = (List<?>) value;
                if (CollectionUtils.isNotEmpty(valueList)) {
                    List<Term> terms = valueList.stream()
                            .map(QueryBuilder::literal)
                            .collect(Collectors.toList());
                    select = select.whereColumn(columnName).in(terms);
                }
            } else {
                select = select.whereColumn(columnName)
                        .isEqualTo(QueryBuilder.literal(value));
            }
        }
        return select;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object insertRecord(String keyspaceName, String tableName, Map<String, Object> request) {
        ApiResponse response = new ApiResponse();
        try {
            String query = cassandraUtil.getPreparedStatement(keyspaceName, tableName, request);
            CqlSession session = connectionManager.getSession(keyspaceName);
            PreparedStatement statement = session.prepare(query);
            BoundStatement boundStatement = statement.bind(request.values().toArray());
            session.execute(boundStatement);
            response.put(CommonConstants.RESPONSE, CommonConstants.SUCCESS);
        } catch (Exception e) {
            String errMsg = String.format("Exception occurred while inserting record to %s %s", tableName,
                    e.getMessage());
            log.error("Error inserting record into {}: {}", tableName, e.getMessage(), e);
            response.put(CommonConstants.RESPONSE, CommonConstants.FAILED);
            response.put(CommonConstants.ERROR_MESSAGE, errMsg);
        }
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map<String, Object>> getRecordsByProperties(String keyspaceName, String tableName,
            Map<String, Object> propertyMap, List<String> fields, Integer limit) {
        List<Map<String, Object>> response = new ArrayList<>();
        try {
            Select selectQuery = processQuery(keyspaceName, tableName, propertyMap, fields);
            if (limit != null) {
                selectQuery = selectQuery.limit(limit);
            }
            String queryString = selectQuery.toString();
            SimpleStatement statement = SimpleStatement.newInstance(queryString);
            ResultSet results = connectionManager.getSession(keyspaceName).execute(statement);
            response = cassandraUtil.createResponse(results);
        } catch (Exception e) {
            log.error("Error fetching records from {}: {}", tableName, e.getMessage(), e);
        }
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> updateRecord(String keyspaceName, String tableName, Map<String, Object> updateAttributes,
            Map<String, Object> compositeKey) {
        Map<String, Object> response = new HashMap<>();
        try {
            CqlSession session = connectionManager.getSession(keyspaceName);
            UpdateStart updateStart = QueryBuilder.update(keyspaceName, tableName);
            UpdateWithAssignments updateWithAssignments = updateStart.set(updateAttributes.entrySet().stream()
                    .map(entry -> Assignment.setColumn(entry.getKey(), QueryBuilder.literal(entry.getValue())))
                    .toArray(Assignment[]::new));
            com.datastax.oss.driver.api.querybuilder.update.Update update = updateWithAssignments.where(compositeKey
                    .entrySet().stream()
                    .map(entry -> Relation.column(entry.getKey()).isEqualTo(QueryBuilder.literal(entry.getValue())))
                    .toArray(Relation[]::new));
            SimpleStatement statement = update.build();
            session.execute(statement);
            response.put(CommonConstants.RESPONSE, CommonConstants.SUCCESS);
        } catch (Exception e) {
            String errMsg = String.format("Exception occurred while updating record to %s: %s", tableName,
                    e.getMessage());
            log.error(errMsg, e);
            response.put(CommonConstants.RESPONSE, CommonConstants.FAILED);
            response.put(CommonConstants.ERROR_MESSAGE, errMsg);
            throw e;
        }
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResponse insertBulkRecord(String keyspaceName, String tableName, List<Map<String, Object>> requestList) {
        ApiResponse response = new ApiResponse();
        try {
            final int batchSize = 10;
            List<Map<String, Object>> tempBatch = new ArrayList<>(batchSize);
            CqlSession session = connectionManager.getSession(keyspaceName);

            for (int i = 0; i < requestList.size(); i++) {
                tempBatch.add(requestList.get(i));

                if (tempBatch.size() == batchSize || i == requestList.size() - 1) {
                    BatchStatementBuilder batchBuilder = BatchStatement.builder(DefaultBatchType.LOGGED);
                    for (Map<String, Object> requestMap : tempBatch) {
                        String query = cassandraUtil.getPreparedStatement(keyspaceName, tableName, requestMap);
                        PreparedStatement preparedStatement = session.prepare(query);
                        BoundStatement boundStatement = preparedStatement.bind(requestMap.values().toArray());
                        batchBuilder.addStatement(boundStatement);
                    }
                    session.execute(batchBuilder.build());
                    tempBatch.clear();
                }
            }
            response.put(CommonConstants.RESPONSE, CommonConstants.SUCCESS);
        } catch (Exception e) {
            String errMsg = String.format("Exception occurred while inserting bulk record to %s: %s", tableName,
                    e.getMessage());
            log.error(errMsg, e);
            response.put(CommonConstants.RESPONSE, CommonConstants.FAILED);
            response.put(CommonConstants.ERROR_MESSAGE, errMsg);
        }
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteRecord(String keyspaceName, String tableName, Map<String, Object> compositeKeyMap) {
        try {
            CqlSession session = connectionManager.getSession(keyspaceName);
            Delete delete = null;

            for (Map.Entry<String, Object> entry : compositeKeyMap.entrySet()) {
                if (delete == null) {
                    delete = QueryBuilder.deleteFrom(keyspaceName, tableName)
                            .whereColumn(entry.getKey()).isEqualTo(QueryBuilder.literal(entry.getValue()));
                } else {
                    delete = delete.whereColumn(entry.getKey()).isEqualTo(QueryBuilder.literal(entry.getValue()));
                }
            }

            if (delete != null) {
                session.execute(delete.build());
            }
        } catch (Exception e) {
            log.error("CassandraOperationImpl: deleteRecord by composite key. {} {} {}",
                    CommonConstants.EXCEPTION_MSG_DELETE, tableName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object insertRecord(String keyspaceName, String tableName, String primaryKeyColumn, String primaryKeyValue,
            Map<String, Object> compositeKey, Map<String, Object> otherFields) {
        ApiResponse response = new ApiResponse();
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put(primaryKeyColumn, primaryKeyValue);
            if (MapUtils.isNotEmpty(compositeKey)) {
                request.putAll(compositeKey);
            }
            if (MapUtils.isNotEmpty(otherFields)) {
                request.putAll(otherFields);
            }
            String query = cassandraUtil.getPreparedStatement(keyspaceName, tableName, request);
            CqlSession session = connectionManager.getSession(keyspaceName);
            SimpleStatement simpleStatement = SimpleStatement.builder(query)
                    .addPositionalValues(request.values())
                    .build();
            session.execute(simpleStatement);
            response.put(CommonConstants.RESPONSE, CommonConstants.SUCCESS);
        } catch (Exception e) {
            String errMsg = String.format(
                    "Exception occurred while inserting record into %s. Error: %s",
                    tableName, e.getMessage());
            log.error("Error inserting record into {}: {}", tableName, e.getMessage(), e);
            response.put(CommonConstants.RESPONSE, CommonConstants.FAILED);
            response.put(CommonConstants.ERROR_MESSAGE, errMsg);
        }
        return response;
    }
}
