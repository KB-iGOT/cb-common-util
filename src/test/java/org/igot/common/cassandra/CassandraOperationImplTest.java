package org.igot.common.cassandra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.igot.common.ApiResponse;
import org.igot.common.CommonConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.BatchStatement;

@ExtendWith(MockitoExtension.class)
class CassandraOperationImplTest {

    @Mock
    private CassandraConnectionManager connectionManager;

    @Mock
    private CassandraUtil cassandraUtil;

    @Mock
    private CqlSession session;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private BoundStatement boundStatement;

    @Mock
    private ResultSet resultSet;

    private CassandraOperationImpl cassandraOperation;

    private static final String KEYSPACE_NAME = "test_keyspace";
    private static final String TABLE_NAME = "test_table";

    @BeforeEach
    void setUp() {
        cassandraOperation = new CassandraOperationImpl(connectionManager, cassandraUtil);
    }

    @Test
    void testInsertRecord_Success() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("id", "123");
        request.put("name", "John Doe");

        String preparedQuery = "INSERT INTO test_keyspace.test_table(id,name) VALUES (?,?);";

        when(cassandraUtil.getPreparedStatement(KEYSPACE_NAME, TABLE_NAME, request))
                .thenReturn(preparedQuery);
        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(session.prepare(preparedQuery)).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(Object[].class))).thenReturn(boundStatement);
        when(session.execute(boundStatement)).thenReturn(resultSet);

        // Act
        Object result = cassandraOperation.insertRecord(KEYSPACE_NAME, TABLE_NAME, request);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof ApiResponse);
        ApiResponse response = (ApiResponse) result;
        assertEquals(CommonConstants.SUCCESS, response.get(CommonConstants.RESPONSE));

        verify(cassandraUtil).getPreparedStatement(KEYSPACE_NAME, TABLE_NAME, request);
        verify(connectionManager).getSession(KEYSPACE_NAME);
        verify(session).prepare(preparedQuery);
        verify(session).execute(boundStatement);
    }

    @Test
    void testInsertRecord_Exception() {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("id", "123");

        String preparedQuery = "INSERT INTO test_keyspace.test_table(id) VALUES (?);";

        when(cassandraUtil.getPreparedStatement(KEYSPACE_NAME, TABLE_NAME, request))
                .thenReturn(preparedQuery);
        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(session.prepare(preparedQuery)).thenThrow(new RuntimeException("Database error"));

        // Act
        Object result = cassandraOperation.insertRecord(KEYSPACE_NAME, TABLE_NAME, request);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof ApiResponse);
        ApiResponse response = (ApiResponse) result;
        assertEquals(CommonConstants.FAILED, response.get(CommonConstants.RESPONSE));
        assertTrue(response.get(CommonConstants.ERROR_MESSAGE).toString().contains("Database error"));
    }

    @Test
    void testGetRecordsByProperties_Success_WithAllParameters() {
        // Arrange
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("id", "123");

        List<String> fields = Arrays.asList("id", "name", "email");
        Integer limit = 10;

        List<Map<String, Object>> expectedResponse = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", "123");
        row1.put("name", "John");
        row1.put("email", "john@example.com");
        expectedResponse.add(row1);

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        when(cassandraUtil.createResponse(resultSet)).thenReturn(expectedResponse);

        // Act
        List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties(
                KEYSPACE_NAME, TABLE_NAME, propertyMap, fields, limit);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("123", result.get(0).get("id"));
        assertEquals("John", result.get(0).get("name"));

        verify(connectionManager).getSession(KEYSPACE_NAME);
        verify(session).execute(any(SimpleStatement.class));
        verify(cassandraUtil).createResponse(resultSet);
    }

    @Test
    void testGetRecordsByProperties_Success_WithoutLimit() {
        // Arrange
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("status", "active");

        List<Map<String, Object>> expectedResponse = new ArrayList<>();

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);
        when(cassandraUtil.createResponse(resultSet)).thenReturn(expectedResponse);

        // Act
        List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties(
                KEYSPACE_NAME, TABLE_NAME, propertyMap, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(connectionManager).getSession(KEYSPACE_NAME);
        verify(session).execute(any(SimpleStatement.class));
    }

    @Test
    void testGetRecordsByProperties_Exception() {
        // Arrange
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("id", "123");

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(session.execute(any(SimpleStatement.class)))
                .thenThrow(new RuntimeException("Query failed"));

        // Act
        List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties(
                KEYSPACE_NAME, TABLE_NAME, propertyMap, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testUpdateRecord_Success() {
        // Arrange
        Map<String, Object> updateAttributes = new HashMap<>();
        updateAttributes.put("name", "Jane Doe");
        updateAttributes.put("status", "active");

        Map<String, Object> compositeKey = new HashMap<>();
        compositeKey.put("id", "123");

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        // Act
        Map<String, Object> result = cassandraOperation.updateRecord(
                KEYSPACE_NAME, TABLE_NAME, updateAttributes, compositeKey);

        // Assert
        assertNotNull(result);
        assertEquals(CommonConstants.SUCCESS, result.get(CommonConstants.RESPONSE));

        verify(connectionManager).getSession(KEYSPACE_NAME);
        verify(session).execute(any(SimpleStatement.class));
    }

    @Test
    void testUpdateRecord_Exception() {
        // Arrange
        Map<String, Object> updateAttributes = new HashMap<>();
        updateAttributes.put("name", "Jane Doe");

        Map<String, Object> compositeKey = new HashMap<>();
        compositeKey.put("id", "123");

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(session.execute(any(SimpleStatement.class)))
                .thenThrow(new RuntimeException("Update failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cassandraOperation.updateRecord(KEYSPACE_NAME, TABLE_NAME, updateAttributes, compositeKey);
        });

        assertEquals("Update failed", exception.getMessage());
    }

    @Test
    void testInsertBulkRecord_Success() {
        // Arrange
        List<Map<String, Object>> requestList = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", String.valueOf(i));
            record.put("name", "User" + i);
            requestList.add(record);
        }

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(cassandraUtil.getPreparedStatement(eq(KEYSPACE_NAME), eq(TABLE_NAME), any()))
                .thenReturn("INSERT INTO test_keyspace.test_table(id,name) VALUES (?,?);");
        when(session.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(Object[].class))).thenReturn(boundStatement);
        when(session.execute(any(BatchStatement.class))).thenReturn(resultSet);

        // Act
        ApiResponse result = cassandraOperation.insertBulkRecord(KEYSPACE_NAME, TABLE_NAME, requestList);

        // Assert
        assertNotNull(result);
        assertEquals(CommonConstants.SUCCESS, result.get(CommonConstants.RESPONSE));

        // Verify batch execution (25 records, batch size 10, so 3 batches)
        verify(session, times(3)).execute(any(BatchStatement.class));
    }

    @Test
    void testInsertBulkRecord_ExactBatchSize() {
        // Arrange
        List<Map<String, Object>> requestList = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", String.valueOf(i));
            requestList.add(record);
        }

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(cassandraUtil.getPreparedStatement(eq(KEYSPACE_NAME), eq(TABLE_NAME), any()))
                .thenReturn("INSERT INTO test_keyspace.test_table(id) VALUES (?);");
        when(session.prepare(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.bind(any(Object[].class))).thenReturn(boundStatement);
        when(session.execute(any(BatchStatement.class))).thenReturn(resultSet);

        // Act
        ApiResponse result = cassandraOperation.insertBulkRecord(KEYSPACE_NAME, TABLE_NAME, requestList);

        // Assert
        assertNotNull(result);
        assertEquals(CommonConstants.SUCCESS, result.get(CommonConstants.RESPONSE));

        // Verify single batch execution for exactly 10 records
        verify(session, times(1)).execute(any(BatchStatement.class));
    }

    @Test
    void testInsertBulkRecord_Exception() {
        // Arrange
        List<Map<String, Object>> requestList = new ArrayList<>();
        Map<String, Object> record = new HashMap<>();
        record.put("id", "123");
        requestList.add(record);

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(cassandraUtil.getPreparedStatement(eq(KEYSPACE_NAME), eq(TABLE_NAME), any()))
                .thenThrow(new RuntimeException("Bulk insert failed"));

        // Act
        ApiResponse result = cassandraOperation.insertBulkRecord(KEYSPACE_NAME, TABLE_NAME, requestList);

        // Assert
        assertNotNull(result);
        assertEquals(CommonConstants.FAILED, result.get(CommonConstants.RESPONSE));
        assertTrue(result.get(CommonConstants.ERROR_MESSAGE).toString().contains("Bulk insert failed"));
    }

    @Test
    void testDeleteRecord_Success() {
        // Arrange
        Map<String, Object> compositeKeyMap = new HashMap<>();
        compositeKeyMap.put("id", "123");
        compositeKeyMap.put("timestamp", 1234567890L);

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        // Act & Assert (should not throw exception)
        assertDoesNotThrow(() -> {
            cassandraOperation.deleteRecord(KEYSPACE_NAME, TABLE_NAME, compositeKeyMap);
        });

        verify(connectionManager).getSession(KEYSPACE_NAME);
        verify(session).execute(any(SimpleStatement.class));
    }

    @Test
    void testDeleteRecord_SingleKey() {
        // Arrange
        Map<String, Object> compositeKeyMap = new HashMap<>();
        compositeKeyMap.put("id", "123");

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        // Act & Assert
        assertDoesNotThrow(() -> {
            cassandraOperation.deleteRecord(KEYSPACE_NAME, TABLE_NAME, compositeKeyMap);
        });

        verify(session).execute(any(SimpleStatement.class));
    }

    @Test
    void testDeleteRecord_Exception() {
        // Arrange
        Map<String, Object> compositeKeyMap = new HashMap<>();
        compositeKeyMap.put("id", "123");

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(session.execute(any(SimpleStatement.class)))
                .thenThrow(new RuntimeException("Delete failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            cassandraOperation.deleteRecord(KEYSPACE_NAME, TABLE_NAME, compositeKeyMap);
        });

        assertEquals("Delete failed", exception.getMessage());
    }

    @Test
    void testInsertRecordWithCompositeKey_Success_AllParameters() {
        // Arrange
        String primaryKeyColumn = "id";
        String primaryKeyValue = "123";
        Map<String, Object> compositeKey = new HashMap<>();
        compositeKey.put("timestamp", 1234567890L);
        Map<String, Object> otherFields = new HashMap<>();
        otherFields.put("name", "John Doe");
        otherFields.put("email", "john@example.com");

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(cassandraUtil.getPreparedStatement(eq(KEYSPACE_NAME), eq(TABLE_NAME), any()))
                .thenReturn("INSERT INTO test_keyspace.test_table(id,timestamp,name,email) VALUES (?,?,?,?);");
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        // Act
        Object result = cassandraOperation.insertRecord(
                KEYSPACE_NAME, TABLE_NAME, primaryKeyColumn, primaryKeyValue, compositeKey, otherFields);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof ApiResponse);
        ApiResponse response = (ApiResponse) result;
        assertEquals(CommonConstants.SUCCESS, response.get(CommonConstants.RESPONSE));

        verify(connectionManager).getSession(KEYSPACE_NAME);
        verify(cassandraUtil).getPreparedStatement(eq(KEYSPACE_NAME), eq(TABLE_NAME), any());
        verify(session).execute(any(SimpleStatement.class));
    }

    @Test
    void testInsertRecordWithCompositeKey_Success_NullCompositeKey() {
        // Arrange
        String primaryKeyColumn = "id";
        String primaryKeyValue = "123";
        Map<String, Object> otherFields = new HashMap<>();
        otherFields.put("name", "John Doe");

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(cassandraUtil.getPreparedStatement(eq(KEYSPACE_NAME), eq(TABLE_NAME), any()))
                .thenReturn("INSERT INTO test_keyspace.test_table(id,name) VALUES (?,?);");
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        // Act
        Object result = cassandraOperation.insertRecord(
                KEYSPACE_NAME, TABLE_NAME, primaryKeyColumn, primaryKeyValue, null, otherFields);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof ApiResponse);
        ApiResponse response = (ApiResponse) result;
        assertEquals(CommonConstants.SUCCESS, response.get(CommonConstants.RESPONSE));
    }

    @Test
    void testInsertRecordWithCompositeKey_Success_NullOtherFields() {
        // Arrange
        String primaryKeyColumn = "id";
        String primaryKeyValue = "123";
        Map<String, Object> compositeKey = new HashMap<>();
        compositeKey.put("timestamp", 1234567890L);

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(cassandraUtil.getPreparedStatement(eq(KEYSPACE_NAME), eq(TABLE_NAME), any()))
                .thenReturn("INSERT INTO test_keyspace.test_table(id,timestamp) VALUES (?,?);");
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        // Act
        Object result = cassandraOperation.insertRecord(
                KEYSPACE_NAME, TABLE_NAME, primaryKeyColumn, primaryKeyValue, compositeKey, null);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof ApiResponse);
        ApiResponse response = (ApiResponse) result;
        assertEquals(CommonConstants.SUCCESS, response.get(CommonConstants.RESPONSE));
    }

    @Test
    void testInsertRecordWithCompositeKey_Success_OnlyPrimaryKey() {
        // Arrange
        String primaryKeyColumn = "id";
        String primaryKeyValue = "123";

        when(connectionManager.getSession(KEYSPACE_NAME)).thenReturn(session);
        when(cassandraUtil.getPreparedStatement(eq(KEYSPACE_NAME), eq(TABLE_NAME), any()))
                .thenReturn("INSERT INTO test_keyspace.test_table(id) VALUES (?);");
        when(session.execute(any(SimpleStatement.class))).thenReturn(resultSet);

        // Act
        Object result = cassandraOperation.insertRecord(
                KEYSPACE_NAME, TABLE_NAME, primaryKeyColumn, primaryKeyValue, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof ApiResponse);
        ApiResponse response = (ApiResponse) result;
        assertEquals(CommonConstants.SUCCESS, response.get(CommonConstants.RESPONSE));
    }

    @Test
    void testInsertRecordWithCompositeKey_Exception() {
        // Arrange
        String primaryKeyColumn = "id";
        String primaryKeyValue = "123";

        when(cassandraUtil.getPreparedStatement(eq(KEYSPACE_NAME), eq(TABLE_NAME), any()))
                .thenThrow(new RuntimeException("Insert failed"));

        // Act
        Object result = cassandraOperation.insertRecord(
                KEYSPACE_NAME, TABLE_NAME, primaryKeyColumn, primaryKeyValue, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof ApiResponse);
        ApiResponse response = (ApiResponse) result;
        assertEquals(CommonConstants.FAILED, response.get(CommonConstants.RESPONSE));
        assertTrue(response.get(CommonConstants.ERROR_MESSAGE).toString().contains("Insert failed"));
    }
}
