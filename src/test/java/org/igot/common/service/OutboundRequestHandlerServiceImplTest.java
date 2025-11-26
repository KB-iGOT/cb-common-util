package org.igot.common.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class OutboundRequestHandlerServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    private OutboundRequestHandlerServiceImpl service;

    private static final String TEST_URI = "http://test.com/api";

    @BeforeEach
    void setUp() {
        service = new OutboundRequestHandlerServiceImpl(restTemplate);
    }

    @Test
    void testFetchResult_Success() {
        // Arrange
        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("status", "success");
        expectedResponse.put("data", "test data");

        when(restTemplate.getForObject(TEST_URI, Map.class)).thenReturn(expectedResponse);

        // Act
        Object result = service.fetchResult(TEST_URI);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(restTemplate, times(1)).getForObject(TEST_URI, Map.class);
    }

    @Test
    void testFetchResult_HttpClientErrorException() {
        // Arrange
        String errorResponse = "{\"error\":\"Bad Request\",\"message\":\"Invalid input\"}";
        HttpClientErrorException exception = new HttpClientErrorException(
                HttpStatus.BAD_REQUEST, "Bad Request", errorResponse.getBytes(), null);

        when(restTemplate.getForObject(TEST_URI, Map.class)).thenThrow(exception);

        // Act
        Object result = service.fetchResult(TEST_URI);

        // Assert
        assertNotNull(result);
        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals("Bad Request", resultMap.get("error"));
        assertEquals("Invalid input", resultMap.get("message"));
        verify(restTemplate, times(1)).getForObject(TEST_URI, Map.class);
    }

    @Test
    void testFetchResult_GenericException() {
        // Arrange
        when(restTemplate.getForObject(TEST_URI, Map.class))
                .thenThrow(new RuntimeException("Connection timeout"));

        // Act
        Object result = service.fetchResult(TEST_URI);

        // Assert
        assertNull(result);
        verify(restTemplate, times(1)).getForObject(TEST_URI, Map.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFetchResultUsingExchange_Success() {
        // Arrange
        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("status", "success");
        ParameterizedTypeReference<Map<String, Object>> responseType =
                new ParameterizedTypeReference<Map<String, Object>>() {};
        ResponseEntity<Map<String, Object>> responseEntity =
                new ResponseEntity<>(expectedResponse, HttpStatus.OK);

        when(restTemplate.exchange(eq(TEST_URI), eq(HttpMethod.GET), isNull(),
                any(ParameterizedTypeReference.class))).thenReturn(responseEntity);

        // Act
        Map<String, Object> result = service.fetchResultUsingExchange(TEST_URI, responseType);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(restTemplate, times(1)).exchange(eq(TEST_URI), eq(HttpMethod.GET),
                isNull(), any(ParameterizedTypeReference.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFetchResultUsingExchange_HttpClientErrorException() {
        // Arrange
        String errorResponse = "{\"error\":\"Unauthorized\",\"message\":\"Invalid token\"}";
        HttpClientErrorException exception = new HttpClientErrorException(
                HttpStatus.UNAUTHORIZED, "Unauthorized", errorResponse.getBytes(), null);

        ParameterizedTypeReference<Map<String, Object>> responseType =
                new ParameterizedTypeReference<Map<String, Object>>() {};

        when(restTemplate.exchange(eq(TEST_URI), eq(HttpMethod.GET), isNull(),
                any(ParameterizedTypeReference.class))).thenThrow(exception);

        // Act
        Map<String, Object> result = service.fetchResultUsingExchange(TEST_URI, responseType);

        // Assert
        assertNull(result);
        verify(restTemplate, times(1)).exchange(eq(TEST_URI), eq(HttpMethod.GET),
                isNull(), any(ParameterizedTypeReference.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testFetchResultUsingExchange_GenericException() {
        // Arrange
        ParameterizedTypeReference<Map<String, Object>> responseType =
                new ParameterizedTypeReference<Map<String, Object>>() {};

        when(restTemplate.exchange(eq(TEST_URI), eq(HttpMethod.GET), isNull(),
                any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Network error"));

        // Act
        Map<String, Object> result = service.fetchResultUsingExchange(TEST_URI, responseType);

        // Assert
        assertNull(result);
        verify(restTemplate, times(1)).exchange(eq(TEST_URI), eq(HttpMethod.GET),
                isNull(), any(ParameterizedTypeReference.class));
    }

    @Test
    void testFetchResultUsingPatch_Success_WithHeaders() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "test");

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token123");
        headers.put("X-Custom-Header", "custom-value");

        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("status", "updated");

        when(restTemplate.patchForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(expectedResponse);

        // Act
        Map<String, Object> result = service.fetchResultUsingPatch(TEST_URI, requestBody, headers);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(restTemplate, times(1)).patchForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchResultUsingPatch_Success_WithoutHeaders() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "test");

        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("status", "updated");

        when(restTemplate.patchForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(expectedResponse);

        // Act
        Map<String, Object> result = service.fetchResultUsingPatch(TEST_URI, requestBody, null);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(restTemplate, times(1)).patchForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchResultUsingPatch_HttpClientErrorException() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "test");

        String errorResponse = "{\"error\":\"Validation Error\",\"message\":\"Name is required\"}";
        HttpClientErrorException exception = new HttpClientErrorException(
                HttpStatus.BAD_REQUEST, "Bad Request", errorResponse.getBytes(), null);

        when(restTemplate.patchForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(exception);

        // Act
        Map<String, Object> result = service.fetchResultUsingPatch(TEST_URI, requestBody, null);

        // Assert
        assertNotNull(result);
        assertEquals("Validation Error", result.get("error"));
        assertEquals("Name is required", result.get("message"));
        verify(restTemplate, times(1)).patchForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchResultUsingPatch_NullResponse() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "test");

        when(restTemplate.patchForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(null);

        // Act
        Map<String, Object> result = service.fetchResultUsingPatch(TEST_URI, requestBody, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(restTemplate, times(1)).patchForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchResultUsingPost_Success_WithHeaders() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("username", "testuser");
        requestBody.put("email", "test@example.com");

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token123");
        headers.put("X-Request-ID", "req-123");

        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("id", "123");
        expectedResponse.put("status", "created");

        when(restTemplate.postForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(expectedResponse);

        // Act
        Map<String, Object> result = service.fetchResultUsingPost(TEST_URI, requestBody, headers);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        assertEquals("123", result.get("id"));
        assertEquals("created", result.get("status"));
        verify(restTemplate, times(1)).postForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchResultUsingPost_Success_WithoutHeaders() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", "test");

        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("result", "success");

        when(restTemplate.postForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(expectedResponse);

        // Act
        Map<String, Object> result = service.fetchResultUsingPost(TEST_URI, requestBody, null);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(restTemplate, times(1)).postForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchResultUsingPost_HttpStatusCodeException() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", "test");

        String errorResponse = "{\"error\":\"Conflict\",\"message\":\"Resource already exists\"}";
        HttpStatusCodeException exception = new HttpClientErrorException(
                HttpStatus.CONFLICT, "Conflict", errorResponse.getBytes(), null);

        when(restTemplate.postForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(exception);

        // Act
        Map<String, Object> result = service.fetchResultUsingPost(TEST_URI, requestBody, null);

        // Assert
        assertNotNull(result);
        assertEquals("Conflict", result.get("error"));
        assertEquals("Resource already exists", result.get("message"));
        verify(restTemplate, times(1)).postForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchResultUsingPost_GenericException() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", "test");

        when(restTemplate.postForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        // Act
        Map<String, Object> result = service.fetchResultUsingPost(TEST_URI, requestBody, null);

        // Assert
        assertNull(result);
        verify(restTemplate, times(1)).postForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchResultUsingPost_EmptyHeaders() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", "test");

        Map<String, String> emptyHeaders = new HashMap<>();

        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("result", "success");

        when(restTemplate.postForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(expectedResponse);

        // Act
        Map<String, Object> result = service.fetchResultUsingPost(TEST_URI, requestBody, emptyHeaders);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(restTemplate, times(1)).postForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testConstructor_InitializesObjectMapper() {
        // Act
        OutboundRequestHandlerServiceImpl newService = new OutboundRequestHandlerServiceImpl(restTemplate);

        // Assert - Test that the service is created successfully
        assertNotNull(newService);

        // Test by making a call that uses the objectMapper
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Test exception"));

        Object result = newService.fetchResult(TEST_URI);
        assertNull(result);
    }

    @Test
    void testFetchResultUsingPatch_EmptyHeaders() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "test");

        Map<String, String> emptyHeaders = new HashMap<>();

        Map<String, Object> expectedResponse = new HashMap<>();
        expectedResponse.put("status", "updated");

        when(restTemplate.patchForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(expectedResponse);

        // Act
        Map<String, Object> result = service.fetchResultUsingPatch(TEST_URI, requestBody, emptyHeaders);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(restTemplate, times(1)).patchForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchResult_HttpClientErrorException_InvalidJson() {
        // Arrange
        String invalidErrorResponse = "This is not a valid JSON";
        HttpClientErrorException exception = new HttpClientErrorException(
                HttpStatus.BAD_REQUEST, "Bad Request", invalidErrorResponse.getBytes(), null);

        when(restTemplate.getForObject(TEST_URI, Map.class)).thenThrow(exception);

        // Act
        Object result = service.fetchResult(TEST_URI);

        // Assert - Should return null when JSON parsing fails
        assertNull(result);
        verify(restTemplate, times(1)).getForObject(TEST_URI, Map.class);
    }

    @Test
    void testFetchResultUsingPatch_HttpClientErrorException_InvalidJson() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", "test");

        String invalidErrorResponse = "Invalid JSON response";
        HttpClientErrorException exception = new HttpClientErrorException(
                HttpStatus.BAD_REQUEST, "Bad Request", invalidErrorResponse.getBytes(), null);

        when(restTemplate.patchForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(exception);

        // Act
        Map<String, Object> result = service.fetchResultUsingPatch(TEST_URI, requestBody, null);

        // Assert - Should return null when response is null and JSON parsing fails
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(restTemplate, times(1)).patchForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchResultUsingPost_HttpStatusCodeException_InvalidJson() {
        // Arrange
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", "test");

        String invalidErrorResponse = "Not a JSON response";
        HttpStatusCodeException exception = new HttpClientErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", invalidErrorResponse.getBytes(), null);

        when(restTemplate.postForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(exception);

        // Act
        Map<String, Object> result = service.fetchResultUsingPost(TEST_URI, requestBody, null);

        // Assert - Should return null when JSON parsing fails
        assertNull(result);
        verify(restTemplate, times(1)).postForObject(eq(TEST_URI), any(HttpEntity.class), eq(Map.class));
    }
}
