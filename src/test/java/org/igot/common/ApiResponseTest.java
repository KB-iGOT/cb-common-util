package org.igot.common;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiResponseTest {

    @Test
    @DisplayName("Should create ApiResponse with default values")
    void constructor_Default_SetsDefaultValues() {
        ApiResponse response = new ApiResponse();

        assertEquals("v1", response.getVer());
        assertNotNull(response.getTs());
        assertNotNull(response.getParams());
        assertNotNull(response.getParams().getResMsgId());
        assertNotNull(response.getParams().getMsgId());
        assertNotNull(response.getResponse());
        assertTrue(response.getResponse().isEmpty());
    }

    @Test
    @DisplayName("Should create ApiResponse with id")
    void constructor_WithId_SetsIdAndDefaults() {
        String apiId = "api.test.endpoint";
        ApiResponse response = new ApiResponse(apiId);

        assertEquals(apiId, response.getId());
        assertEquals("v1", response.getVer());
        assertNotNull(response.getTs());
        assertNotNull(response.getParams());
    }

    @Test
    @DisplayName("Should have ISO-8601 timestamp format")
    void constructor_Timestamp_IsISO8601Format() {
        ApiResponse response = new ApiResponse();

        String timestamp = response.getTs();
        // Should be parseable as ISO-8601 Instant
        assertDoesNotThrow(() -> Instant.parse(timestamp));
    }

    @Test
    @DisplayName("Should generate unique UUIDs for params")
    void constructor_Params_HasUniqueUUID() {
        ApiResponse response1 = new ApiResponse();
        ApiResponse response2 = new ApiResponse();

        assertNotEquals(
                response1.getParams().getResMsgId(),
                response2.getParams().getResMsgId()
        );
    }

    @Test
    @DisplayName("Should set and get response code")
    void setResponseCode_SetsCorrectly() {
        ApiResponse response = new ApiResponse();
        response.setResponseCode(HttpStatus.OK);

        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    @DisplayName("Should set and get response data")
    void setResponse_SetsDataCorrectly() {
        ApiResponse response = new ApiResponse();
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");
        data.put("count", 42);

        response.setResponse(data);

        assertEquals("value", response.getResponse().get("key"));
        assertEquals(42, response.getResponse().get("count"));
    }

    @Test
    @DisplayName("Should allow adding data to response map")
    void getResponse_AllowsAddingData() {
        ApiResponse response = new ApiResponse();
        response.getResponse().put("result", "success");
        response.getResponse().put("data", new HashMap<>());

        assertEquals("success", response.getResponse().get("result"));
        assertNotNull(response.getResponse().get("data"));
    }

    @Test
    @DisplayName("Should set error status in params")
    void params_SetErrorStatus() {
        ApiResponse response = new ApiResponse();
        response.getParams().setStatus(CommonConstants.FAILED);
        response.getParams().setErrMsg("Test error message");
        response.getParams().setErr("ERR_CODE");

        assertEquals(CommonConstants.FAILED, response.getParams().getStatus());
        assertEquals("Test error message", response.getParams().getErrMsg());
        assertEquals("ERR_CODE", response.getParams().getErr());
    }

    @Test
    @DisplayName("Should handle HttpStatus UNAUTHORIZED")
    void setResponseCode_Unauthorized_SetsCorrectly() {
        ApiResponse response = new ApiResponse();
        response.setResponseCode(HttpStatus.UNAUTHORIZED);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getResponseCode());
        assertEquals(401, response.getResponseCode().value());
    }

    @Test
    @DisplayName("Should handle HttpStatus INTERNAL_SERVER_ERROR")
    void setResponseCode_InternalServerError_SetsCorrectly() {
        ApiResponse response = new ApiResponse();
        response.setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(500, response.getResponseCode().value());
    }

    @Test
    @DisplayName("Should set version correctly")
    void setVer_SetsVersionCorrectly() {
        ApiResponse response = new ApiResponse();
        response.setVer("v2");

        assertEquals("v2", response.getVer());
    }

    @Test
    @DisplayName("Should set id correctly")
    void setId_SetsIdCorrectly() {
        ApiResponse response = new ApiResponse();
        response.setId("new.api.id");

        assertEquals("new.api.id", response.getId());
    }

    @Test
    @DisplayName("Params should have matching resmsgid and msgid")
    void constructor_ParamsIds_Match() {
        ApiResponse response = new ApiResponse();

        assertEquals(
                response.getParams().getResMsgId(),
                response.getParams().getMsgId()
        );
    }

    @Test
    @DisplayName("Should get value from response map using get()")
    void get_ReturnsValueFromResponseMap() {
        ApiResponse response = new ApiResponse();
        response.getResponse().put("testKey", "testValue");
        response.getResponse().put("numKey", 123);

        assertEquals("testValue", response.get("testKey"));
        assertEquals(123, response.get("numKey"));
    }

    @Test
    @DisplayName("Should return null for non-existent key using get()")
    void get_NonExistentKey_ReturnsNull() {
        ApiResponse response = new ApiResponse();

        assertNull(response.get("nonExistentKey"));
    }

    @Test
    @DisplayName("Should put key-value pair into response map")
    void put_AddsKeyValueToResponseMap() {
        ApiResponse response = new ApiResponse();
        response.put("name", "John Doe");
        response.put("age", 30);

        assertEquals("John Doe", response.get("name"));
        assertEquals(30, response.get("age"));
    }

    @Test
    @DisplayName("Should overwrite existing value with put()")
    void put_ExistingKey_OverwritesValue() {
        ApiResponse response = new ApiResponse();
        response.put("status", "pending");
        response.put("status", "completed");

        assertEquals("completed", response.get("status"));
    }

    @Test
    @DisplayName("Should put all entries from map using putAll()")
    void putAll_AddsAllEntriesFromMap() {
        ApiResponse response = new ApiResponse();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("key1", "value1");
        dataMap.put("key2", "value2");
        dataMap.put("key3", 100);

        response.putAll(dataMap);

        assertEquals("value1", response.get("key1"));
        assertEquals("value2", response.get("key2"));
        assertEquals(100, response.get("key3"));
    }

    @Test
    @DisplayName("Should handle empty map in putAll()")
    void putAll_EmptyMap_DoesNotThrow() {
        ApiResponse response = new ApiResponse();
        Map<String, Object> emptyMap = new HashMap<>();

        assertDoesNotThrow(() -> response.putAll(emptyMap));
        assertTrue(response.getResponse().isEmpty());
    }

    @Test
    @DisplayName("Should return true when key exists using containsKey()")
    void containsKey_ExistingKey_ReturnsTrue() {
        ApiResponse response = new ApiResponse();
        response.put("testKey", "testValue");

        assertTrue(response.containsKey("testKey"));
    }

    @Test
    @DisplayName("Should return false when key does not exist using containsKey()")
    void containsKey_NonExistentKey_ReturnsFalse() {
        ApiResponse response = new ApiResponse();

        assertFalse(response.containsKey("nonExistentKey"));
    }

    @Test
    @DisplayName("Should get entire response map using getResult()")
    void getResult_ReturnsEntireResponseMap() {
        ApiResponse response = new ApiResponse();
        response.put("key1", "value1");
        response.put("key2", "value2");

        Map<String, Object> result = response.getResult();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    @DisplayName("Should set entire response map using setResult()")
    void setResult_ReplacesEntireResponseMap() {
        ApiResponse response = new ApiResponse();
        response.put("oldKey", "oldValue");

        Map<String, Object> newMap = new HashMap<>();
        newMap.put("newKey", "newValue");
        newMap.put("anotherKey", 456);

        response.setResult(newMap);

        assertFalse(response.containsKey("oldKey"));
        assertTrue(response.containsKey("newKey"));
        assertEquals("newValue", response.get("newKey"));
        assertEquals(456, response.get("anotherKey"));
    }

    @Test
    @DisplayName("Should create default response with all required fields")
    void createDefaultResponse_SetsAllRequiredFields() {
        String apiId = "api.test.endpoint";
        ApiResponse response = ApiResponse.createDefaultResponse(apiId);

        assertEquals(apiId, response.getId());
        assertEquals(CommonConstants.API_VERSION_1, response.getVer());
        assertNotNull(response.getParams());
        assertNotNull(response.getParams().getResMsgId());
        assertEquals(CommonConstants.SUCCESS, response.getParams().getStatus());
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertNotNull(response.getTs());
    }

    @Test
    @DisplayName("Should create default response with valid timestamp")
    void createDefaultResponse_HasValidTimestamp() {
        ApiResponse response = ApiResponse.createDefaultResponse("api.test");

        String timestamp = response.getTs();
        assertDoesNotThrow(() -> Instant.parse(timestamp));
    }

    @Test
    @DisplayName("Should create default responses with unique UUIDs")
    void createDefaultResponse_GeneratesUniqueUUIDs() {
        ApiResponse response1 = ApiResponse.createDefaultResponse("api.test1");
        ApiResponse response2 = ApiResponse.createDefaultResponse("api.test2");

        assertNotEquals(
                response1.getParams().getResMsgId(),
                response2.getParams().getResMsgId()
        );
    }

    @Test
    @DisplayName("Should update error details with message and status code")
    void updateErrorDetails_SetsErrorMessageAndStatus() {
        ApiResponse response = new ApiResponse();
        String errorMessage = "Invalid request parameters";
        HttpStatus errorStatus = HttpStatus.BAD_REQUEST;

        response.updateErrorDetails(errorMessage, errorStatus);

        assertEquals(CommonConstants.FAILED, response.getParams().getStatus());
        assertEquals(errorMessage, response.getParams().getErrMsg());
        assertEquals(errorStatus, response.getResponseCode());
    }

    @Test
    @DisplayName("Should update error details with UNAUTHORIZED status")
    void updateErrorDetails_UnauthorizedStatus_SetsCorrectly() {
        ApiResponse response = new ApiResponse();
        response.updateErrorDetails("Unauthorized access", HttpStatus.UNAUTHORIZED);

        assertEquals(CommonConstants.FAILED, response.getParams().getStatus());
        assertEquals("Unauthorized access", response.getParams().getErrMsg());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getResponseCode());
        assertEquals(401, response.getResponseCode().value());
    }

    @Test
    @DisplayName("Should update error details with INTERNAL_SERVER_ERROR status")
    void updateErrorDetails_InternalServerError_SetsCorrectly() {
        ApiResponse response = new ApiResponse();
        response.updateErrorDetails("Server error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

        assertEquals(CommonConstants.FAILED, response.getParams().getStatus());
        assertEquals("Server error occurred", response.getParams().getErrMsg());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals(500, response.getResponseCode().value());
    }

    @Test
    @DisplayName("Should handle null error message in updateErrorDetails()")
    void updateErrorDetails_NullErrorMessage_DoesNotThrow() {
        ApiResponse response = new ApiResponse();

        assertDoesNotThrow(() -> response.updateErrorDetails(null, HttpStatus.BAD_REQUEST));
        assertEquals(CommonConstants.FAILED, response.getParams().getStatus());
        assertNull(response.getParams().getErrMsg());
    }
}
