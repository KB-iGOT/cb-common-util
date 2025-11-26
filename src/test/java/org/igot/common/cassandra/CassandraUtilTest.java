package org.igot.common.cassandra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.igot.common.PropertiesCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

@ExtendWith(MockitoExtension.class)
class CassandraUtilTest {

    @Mock
    private PropertiesCache propertiesCache;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ColumnDefinitions columnDefinitions;

    @Mock
    private Row row;

    @Mock
    private ColumnDefinition columnDefinition;

    private CassandraUtil cassandraUtil;

    @BeforeEach
    void setUp() {
        cassandraUtil = new CassandraUtil(propertiesCache);
    }

    @Test
    void testGetPreparedStatement_WithSingleColumn() {
        // Arrange
        String keyspaceName = "test_keyspace";
        String tableName = "test_table";
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", "123");

        // Act
        String result = cassandraUtil.getPreparedStatement(keyspaceName, tableName, map);

        // Assert
        assertNotNull(result);
        assertEquals("INSERT INTO test_keyspace.test_table(id) VALUES (?);", result);
    }

    @Test
    void testGetPreparedStatement_WithMultipleColumns() {
        // Arrange
        String keyspaceName = "test_keyspace";
        String tableName = "users";
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", "123");
        map.put("name", "John Doe");
        map.put("email", "john@example.com");

        // Act
        String result = cassandraUtil.getPreparedStatement(keyspaceName, tableName, map);

        // Assert
        assertNotNull(result);
        assertEquals("INSERT INTO test_keyspace.users(id,name,email) VALUES (?,?,?);", result);
    }

    @Test
    void testGetPreparedStatement_WithEmptyMap() {
        // Arrange
        String keyspaceName = "test_keyspace";
        String tableName = "test_table";
        Map<String, Object> map = new HashMap<>();

        // Act
        String result = cassandraUtil.getPreparedStatement(keyspaceName, tableName, map);

        // Assert
        assertNotNull(result);
        assertEquals("INSERT INTO test_keyspace.test_table() VALUES ();", result);
    }

    @Test
    void testFetchColumnsMapping_WithPropertyMapping() {
        // Arrange
        com.datastax.oss.driver.api.core.CqlIdentifier columnName1 =
            com.datastax.oss.driver.api.core.CqlIdentifier.fromInternal("user_id");
        com.datastax.oss.driver.api.core.CqlIdentifier columnName2 =
            com.datastax.oss.driver.api.core.CqlIdentifier.fromInternal("user_name");

        ColumnDefinition col1 = mock(ColumnDefinition.class);
        ColumnDefinition col2 = mock(ColumnDefinition.class);

        when(col1.getName()).thenReturn(columnName1);
        when(col2.getName()).thenReturn(columnName2);

        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.size()).thenReturn(2);

        List<ColumnDefinition> colDefList = Arrays.asList(col1, col2);
        Iterator<ColumnDefinition> colDefIterator = colDefList.iterator();
        doReturn(colDefIterator).when(columnDefinitions).iterator();

        when(propertiesCache.getProperty("user_id")).thenReturn("userId");
        when(propertiesCache.getProperty("user_name")).thenReturn("userName");

        // Act
        Map<String, String> result = cassandraUtil.fetchColumnsMapping(resultSet);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("user_id", result.get("userId"));
        assertEquals("user_name", result.get("userName"));
    }

    @Test
    void testFetchColumnsMapping_WithoutPropertyMapping() {
        // Arrange
        com.datastax.oss.driver.api.core.CqlIdentifier columnName1 =
            com.datastax.oss.driver.api.core.CqlIdentifier.fromInternal("id");
        com.datastax.oss.driver.api.core.CqlIdentifier columnName2 =
            com.datastax.oss.driver.api.core.CqlIdentifier.fromInternal("name");

        ColumnDefinition col1 = mock(ColumnDefinition.class);
        ColumnDefinition col2 = mock(ColumnDefinition.class);

        when(col1.getName()).thenReturn(columnName1);
        when(col2.getName()).thenReturn(columnName2);

        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.size()).thenReturn(2);

        List<ColumnDefinition> colDefList = Arrays.asList(col1, col2);
        Iterator<ColumnDefinition> colDefIterator = colDefList.iterator();
        doReturn(colDefIterator).when(columnDefinitions).iterator();

        when(propertiesCache.getProperty("id")).thenReturn("id");
        when(propertiesCache.getProperty("name")).thenReturn("name");

        // Act
        Map<String, String> result = cassandraUtil.fetchColumnsMapping(resultSet);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("id", result.get("id"));
        assertEquals("name", result.get("name"));
    }

    @Test
    void testFetchColumnsMapping_WithNullProperty() {
        // Arrange
        com.datastax.oss.driver.api.core.CqlIdentifier columnName =
            com.datastax.oss.driver.api.core.CqlIdentifier.fromInternal("test_column");

        ColumnDefinition col = mock(ColumnDefinition.class);
        when(col.getName()).thenReturn(columnName);

        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.size()).thenReturn(1);

        List<ColumnDefinition> colDefList = Arrays.asList(col);
        Iterator<ColumnDefinition> colDefIterator = colDefList.iterator();
        doReturn(colDefIterator).when(columnDefinitions).iterator();

        when(propertiesCache.getProperty("test_column")).thenReturn(null);

        // Act
        Map<String, String> result = cassandraUtil.fetchColumnsMapping(resultSet);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test_column", result.get("test_column"));
    }

    @Test
    void testFetchColumnsMapping_WithWhitespaceProperty() {
        // Arrange
        com.datastax.oss.driver.api.core.CqlIdentifier columnName =
            com.datastax.oss.driver.api.core.CqlIdentifier.fromInternal("test_column");

        ColumnDefinition col = mock(ColumnDefinition.class);
        when(col.getName()).thenReturn(columnName);

        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.size()).thenReturn(1);

        List<ColumnDefinition> colDefList = Arrays.asList(col);
        Iterator<ColumnDefinition> colDefIterator = colDefList.iterator();
        doReturn(colDefIterator).when(columnDefinitions).iterator();

        when(propertiesCache.getProperty("test_column")).thenReturn("  mappedColumn  ");

        // Act
        Map<String, String> result = cassandraUtil.fetchColumnsMapping(resultSet);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test_column", result.get("mappedColumn"));
    }

    @Test
    void testCreateResponse_List_WithMultipleRows() {
        // Arrange
        com.datastax.oss.driver.api.core.CqlIdentifier col1Name =
            com.datastax.oss.driver.api.core.CqlIdentifier.fromInternal("id");
        com.datastax.oss.driver.api.core.CqlIdentifier col2Name =
            com.datastax.oss.driver.api.core.CqlIdentifier.fromInternal("name");

        ColumnDefinition col1 = mock(ColumnDefinition.class);
        ColumnDefinition col2 = mock(ColumnDefinition.class);

        when(col1.getName()).thenReturn(col1Name);
        when(col2.getName()).thenReturn(col2Name);

        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.size()).thenReturn(2);

        List<ColumnDefinition> colDefList = Arrays.asList(col1, col2);
        Iterator<ColumnDefinition> colDefIterator = colDefList.iterator();
        doReturn(colDefIterator).when(columnDefinitions).iterator();

        Row row1 = mock(Row.class);
        Row row2 = mock(Row.class);

        List<Row> rowList = Arrays.asList(row1, row2);
        Iterator<Row> rowIterator = rowList.iterator();
        doReturn(rowIterator).when(resultSet).iterator();

        when(propertiesCache.getProperty("id")).thenReturn("id");
        when(propertiesCache.getProperty("name")).thenReturn("name");

        when(row1.getObject("id")).thenReturn("123");
        when(row1.getObject("name")).thenReturn("Alice");
        when(row2.getObject("id")).thenReturn("456");
        when(row2.getObject("name")).thenReturn("Bob");

        // Act
        List<Map<String, Object>> result = cassandraUtil.createResponse(resultSet);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("123", result.get(0).get("id"));
        assertEquals("Alice", result.get(0).get("name"));
        assertEquals("456", result.get(1).get("id"));
        assertEquals("Bob", result.get(1).get("name"));
    }

    @Test
    void testCreateResponse_List_WithEmptyResultSet() {
        // Arrange
        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.size()).thenReturn(0);

        List<ColumnDefinition> emptyColDefList = Collections.emptyList();
        Iterator<ColumnDefinition> emptyColDefIterator = emptyColDefList.iterator();
        doReturn(emptyColDefIterator).when(columnDefinitions).iterator();

        List<Row> emptyRowList = Collections.emptyList();
        Iterator<Row> emptyRowIterator = emptyRowList.iterator();
        doReturn(emptyRowIterator).when(resultSet).iterator();

        // Act
        List<Map<String, Object>> result = cassandraUtil.createResponse(resultSet);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCreateResponse_Map_WithStringKeys() {
        // Arrange
        com.datastax.oss.driver.api.core.CqlIdentifier col1Name =
            com.datastax.oss.driver.api.core.CqlIdentifier.fromInternal("userId");
        com.datastax.oss.driver.api.core.CqlIdentifier col2Name =
            com.datastax.oss.driver.api.core.CqlIdentifier.fromInternal("userName");

        ColumnDefinition col1 = mock(ColumnDefinition.class);
        ColumnDefinition col2 = mock(ColumnDefinition.class);

        when(col1.getName()).thenReturn(col1Name);
        when(col2.getName()).thenReturn(col2Name);

        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.size()).thenReturn(2);

        List<ColumnDefinition> colDefList = Arrays.asList(col1, col2);
        Iterator<ColumnDefinition> colDefIterator = colDefList.iterator();
        doReturn(colDefIterator).when(columnDefinitions).iterator();

        Row row1 = mock(Row.class);
        Row row2 = mock(Row.class);

        List<Row> rowList = Arrays.asList(row1, row2);
        Iterator<Row> rowIterator = rowList.iterator();
        doReturn(rowIterator).when(resultSet).iterator();

        when(propertiesCache.getProperty("userId")).thenReturn("userId");
        when(propertiesCache.getProperty("userName")).thenReturn("userName");

        when(row1.getObject("userId")).thenReturn("user1");
        when(row1.getObject("userName")).thenReturn("Alice");
        when(row2.getObject("userId")).thenReturn("user2");
        when(row2.getObject("userName")).thenReturn("Bob");

        // Act
        Map<String, Object> result = cassandraUtil.createResponse(resultSet, "userId");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("user1"));
        assertTrue(result.containsKey("user2"));

        Map<String, Object> user1Map = (Map<String, Object>) result.get("user1");
        assertEquals("user1", user1Map.get("userId"));
        assertEquals("Alice", user1Map.get("userName"));

        Map<String, Object> user2Map = (Map<String, Object>) result.get("user2");
        assertEquals("user2", user2Map.get("userId"));
        assertEquals("Bob", user2Map.get("userName"));
    }

    @Test
    void testCreateResponse_Map_WithNonStringKeys() {
        // Arrange
        com.datastax.oss.driver.api.core.CqlIdentifier col1Name =
            com.datastax.oss.driver.api.core.CqlIdentifier.fromInternal("id");
        com.datastax.oss.driver.api.core.CqlIdentifier col2Name =
            com.datastax.oss.driver.api.core.CqlIdentifier.fromInternal("value");

        ColumnDefinition col1 = mock(ColumnDefinition.class);
        ColumnDefinition col2 = mock(ColumnDefinition.class);

        when(col1.getName()).thenReturn(col1Name);
        when(col2.getName()).thenReturn(col2Name);

        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.size()).thenReturn(2);

        List<ColumnDefinition> colDefList = Arrays.asList(col1, col2);
        Iterator<ColumnDefinition> colDefIterator = colDefList.iterator();
        doReturn(colDefIterator).when(columnDefinitions).iterator();

        Row row1 = mock(Row.class);

        List<Row> rowList = Arrays.asList(row1);
        Iterator<Row> rowIterator = rowList.iterator();
        doReturn(rowIterator).when(resultSet).iterator();

        when(propertiesCache.getProperty("id")).thenReturn("id");
        when(propertiesCache.getProperty("value")).thenReturn("value");

        // Key is an Integer, not a String
        when(row1.getObject("id")).thenReturn(123);
        when(row1.getObject("value")).thenReturn("test value");

        // Act
        Map<String, Object> result = cassandraUtil.createResponse(resultSet, "id");

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size()); // Should be empty because key is not a String
    }

    @Test
    void testCreateResponse_Map_WithEmptyResultSet() {
        // Arrange
        when(resultSet.getColumnDefinitions()).thenReturn(columnDefinitions);
        when(columnDefinitions.size()).thenReturn(0);

        List<ColumnDefinition> emptyColDefList = Collections.emptyList();
        Iterator<ColumnDefinition> emptyColDefIterator = emptyColDefList.iterator();
        doReturn(emptyColDefIterator).when(columnDefinitions).iterator();

        List<Row> emptyRowList = Collections.emptyList();
        Iterator<Row> emptyRowIterator = emptyRowList.iterator();
        doReturn(emptyRowIterator).when(resultSet).iterator();

        // Act
        Map<String, Object> result = cassandraUtil.createResponse(resultSet, "userId");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
