package org.igot.common.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.igot.common.CommonConstants;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation for handling outbound HTTP requests to external services.
 * <p>
 * This service provides a centralized mechanism for making HTTP calls (GET, POST, PATCH)
 * to external REST APIs using Spring's RestTemplate. It includes comprehensive error
 * handling, logging, and response parsing capabilities.
 * </p>
 * <p>
 * Features:
 * <ul>
 *   <li>Support for multiple HTTP methods (GET, POST, PATCH)</li>
 *   <li>Automatic error response parsing and logging</li>
 *   <li>Configurable HTTP headers</li>
 *   <li>Generic type support for responses</li>
 *   <li>Debug logging for request/response tracking</li>
 * </ul>
 * </p>
 *
 * @author Karthikeyan R (karthik-tarento)
 */
@Service
@Slf4j
public class OutboundRequestHandlerServiceImpl {
    /**
     * Spring RestTemplate for making HTTP requests.
     */
    private final RestTemplate restTemplate;

    /**
     * Jackson ObjectMapper for JSON serialization/deserialization.
     * Configured to not fail on empty beans.
     */
	private final ObjectMapper objectMapper;

	/**
	 * Constructs a new OutboundRequestHandlerServiceImpl with the specified RestTemplate.
	 * <p>
	 * Initializes the ObjectMapper with configuration to not fail on empty beans.
	 * </p>
	 *
	 * @param restTemplate the RestTemplate to use for HTTP operations
	 */
	public OutboundRequestHandlerServiceImpl(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		this.objectMapper = new ObjectMapper();
		this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
	}

    /**
     * Fetches a result from the specified URI using HTTP GET method.
     * <p>
     * This method performs a simple GET request and returns the response as a Map.
     * It includes comprehensive error handling and logging. If an error occurs,
     * it attempts to parse and return the error response body.
     * </p>
     *
     * @param uri the URI to fetch data from
     * @return the response as an Object (typically a Map), or the error response if the request fails
     */
    public Object fetchResult(String uri) {

		Object response = null;
		try {
			if (log.isDebugEnabled()) {
				StringBuilder str = new StringBuilder(this.getClass().getCanonicalName())
						.append(CommonConstants.FETCH_RESULT_CONSTANT).append(System.lineSeparator());
				str.append(CommonConstants.URI_CONSTANT).append(uri).append(System.lineSeparator());
				log.debug(str.toString());
			}
			response = restTemplate.getForObject(uri, Map.class);
		} catch (HttpClientErrorException e) {
			log.error("Failed to call rest URL: {}", uri, e);
			try {
				response = objectMapper.readValue(e.getResponseBodyAsString(),
						new TypeReference<Map<String, Object>>() {
						});
			} catch (Exception e1) {
				log.debug("Failed to parse error response: {}", e.getResponseBodyAsString(), e1);
			}
			log.error("Error received: " + e.getResponseBodyAsString(), e);
		} catch (Exception e) {
			log.error("Failed to call rest URL: {}", uri, e);
			try {
				log.warn("Error Response: " + objectMapper.writeValueAsString(response));
			} catch (Exception e1) {
				log.debug("Failed to parse error response: ", e1);
			}
		}
		return response;
	}

	/**
	 * Fetches a result from the specified URI using HTTP GET with type-safe response handling.
	 * <p>
	 * This method uses RestTemplate's exchange method to perform a GET request with
	 * support for generic types through ParameterizedTypeReference. This allows for
	 * type-safe deserialization of complex response types including collections.
	 * </p>
	 *
	 * @param <T> the type of the response body
	 * @param uri the URI to fetch data from
	 * @param responseType the ParameterizedTypeReference describing the expected response type
	 * @return the response body of the specified type, or null if the request fails
	 */
	public <T> T fetchResultUsingExchange(String uri, ParameterizedTypeReference<T> responseType) {
		Object response = null;
		try {
			if (log.isDebugEnabled()) {
				StringBuilder str = new StringBuilder(this.getClass().getCanonicalName())
						.append(CommonConstants.FETCH_RESULT_CONSTANT).append(System.lineSeparator());
				str.append(CommonConstants.URI_CONSTANT).append(uri).append(System.lineSeparator());
				log.debug(str.toString());
			}
			ResponseEntity<T> responseEntity = restTemplate.exchange(uri, HttpMethod.GET, null, responseType);
			return responseEntity.getBody();
		} catch (HttpClientErrorException e) {
			log.error("Failed to call rest URL: {}, received error: {}", uri, e.getResponseBodyAsString(), e);
			try {
				response = objectMapper.readValue(e.getResponseBodyAsString(),
						new TypeReference<Map<String, Object>>() {
						});
			} catch (Exception e1) {
				log.debug("Failed to parse error response: {}", e.getResponseBodyAsString(), e1);
			}
			log.error("Error received: " + e.getResponseBodyAsString(), e);
		} catch (Exception e) {
			log.error("Failed to call rest URL: {}", uri, e);
			try {
				log.warn("Error Response: " + objectMapper.writeValueAsString(response));
			} catch (Exception e1) {
				log.debug("Failed to parse error response: ", e1);
			}
		}
		return null;
	}

	/**
	 * Performs an HTTP PATCH request to the specified URI with custom headers.
	 * <p>
	 * This method sends a PATCH request with the provided request body and custom headers.
	 * The Content-Type is automatically set to application/json. If an error occurs,
	 * it attempts to parse and return the error response body.
	 * </p>
	 *
	 * @param uri the URI to send the PATCH request to
	 * @param request the request body object to be sent
	 * @param headersValues optional map of custom header key-value pairs
	 * @return the response as a Map, or an empty sorted map if the request fails or response is null
	 */
	public Map<String, Object> fetchResultUsingPatch(String uri, Object request, Map<String, String> headersValues) {
		Map<String, Object> response = null;
		try {
			HttpHeaders headers = new HttpHeaders();
			if (!MapUtils.isEmpty(headersValues)) {
				headersValues.forEach((k, v) -> headers.set(k, v));
			}
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Object> entity = new HttpEntity<>(request, headers);
			if (log.isDebugEnabled()) {
				log.info(uri, request);
			}
			response = restTemplate.patchForObject(uri, entity, Map.class);
			if (log.isDebugEnabled()) {
				log.info(uri, response);
			}
		} catch (HttpClientErrorException e) {
			try {
				response = (new ObjectMapper()).readValue(e.getResponseBodyAsString(),
						new TypeReference<HashMap<String, Object>>() {
						});
			} catch (Exception e1) {
			}
			log.error("Error received: " + e.getResponseBodyAsString(), e);
		}
		if (response == null) {
			return MapUtils.EMPTY_SORTED_MAP;
		}
		return response;
	}


	/**
	 * Performs an HTTP POST request to the specified URI with custom headers.
	 * <p>
	 * This method sends a POST request with the provided request body and custom headers.
	 * The Content-Type is automatically set to application/json. Debug logging is enabled
	 * to track request and response details when debug level is active.
	 * </p>
	 * <p>
	 * Error handling includes:
	 * <ul>
	 *   <li>Parsing and returning HTTP error responses as Maps</li>
	 *   <li>Comprehensive logging of errors and responses</li>
	 *   <li>Graceful handling of JSON processing exceptions</li>
	 * </ul>
	 * </p>
	 *
	 * @param uri the URI to send the POST request to
	 * @param request the request body object to be sent
	 * @param headersValues optional map of custom header key-value pairs
	 * @return the response as a Map, or null if the request fails
	 */
    public Map<String, Object> fetchResultUsingPost(String uri, Object request, Map<String, String> headersValues) {
        Map<String, Object> response = null;
        try {
            HttpHeaders headers = new HttpHeaders();
            if (!MapUtils.isEmpty(headersValues)) {
                headersValues.forEach((k, v) -> headers.set(k, v));
            }
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(request, headers);
            if (log.isDebugEnabled()) {
                StringBuilder str = new StringBuilder(this.getClass().getCanonicalName()).append(".fetchResult")
                        .append(System.lineSeparator());
                str.append("URI: ").append(uri).append(System.lineSeparator());
                str.append("Request: ").append(objectMapper.writeValueAsString(request)).append(System.lineSeparator());
                log.debug(str.toString());
            }
            response = restTemplate.postForObject(uri, entity, Map.class);
            if (log.isDebugEnabled()) {
                StringBuilder str = new StringBuilder("Response: ");
                str.append(objectMapper.writeValueAsString(response)).append(System.lineSeparator());
                log.debug(str.toString());
            }
        } catch (HttpStatusCodeException hce) {
            try {
                response = (new ObjectMapper()).readValue(hce.getResponseBodyAsString(),
                        new TypeReference<HashMap<String, Object>>() {
                        });
            } catch (Exception e1) {
                log.debug("Failed to parse error response: {}", hce.getResponseBodyAsString(), e1);
            }
            log.error("Error received: " + hce.getResponseBodyAsString(), hce);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            try {
                log.warn("Error Response: " + objectMapper.writeValueAsString(response));
            } catch (Exception e1) {
                log.debug("Failed to parse error response: ", e1);
            }
        } catch (Exception e) {
            log.error("Failed to call rest URL: {}", uri, e);
            try {
                log.warn("Error Response: " + objectMapper.writeValueAsString(response));
            } catch (Exception e1) {
                log.debug("Failed to parse error response: ", e1);
            }
        }
        return response;
    }

	/**
	 * Performs an HTTP GET request to the specified URI with custom headers.
	 * <p>
	 * This method sends a GET request with the provided custom headers and returns
	 * the response as a Map. Unlike {@link #fetchResult(String)}, this method allows
	 * passing custom HTTP headers which is useful for authentication tokens, API keys,
	 * or other header-based configurations.
	 * </p>
	 * <p>
	 * Error handling includes:
	 * <ul>
	 *   <li>Parsing and returning HTTP error responses as Maps</li>
	 *   <li>Comprehensive logging of errors and responses</li>
	 *   <li>Graceful handling of JSON processing exceptions</li>
	 * </ul>
	 * </p>
	 *
	 * @param uri the URI to fetch data from
	 * @param headersValues optional map of custom header key-value pairs to include in the request
	 * @return the response as a Map, or the error response if the request fails, or null if parsing fails
	 */
	public Map<String, Object> fetchUsingGetWithHeadersProfile(String uri, Map<String, String> headersValues) {
        Map<String, Object> response = null;
        try {
            if (log.isDebugEnabled()) {
                StringBuilder str = new StringBuilder(this.getClass().getCanonicalName())
                        .append(CommonConstants.FETCH_RESULT_CONSTANT).append(System.lineSeparator());
                str.append(CommonConstants.URI_CONSTANT).append(uri).append(System.lineSeparator());
                log.debug(str.toString());
            }
            HttpHeaders headers = new HttpHeaders();
            if (!MapUtils.isEmpty(headersValues)) {
                headersValues.forEach((k, v) -> headers.set(k, v));
            }
            HttpEntity<Object> entity = new HttpEntity<>(headers);
            response = restTemplate.exchange(uri, HttpMethod.GET, entity, Map.class).getBody();
        } catch (HttpClientErrorException e) {
            try {
                response = (new ObjectMapper()).readValue(e.getResponseBodyAsString(),
                        new TypeReference<HashMap<String, Object>>() {
                        });
            } catch (Exception e1) {
            }
            log.error("Error received: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error(e.getMessage());
            try {
                log.warn("Error Response: " + objectMapper.writeValueAsString(response));
            } catch (Exception e1) {
            }
        }
        return response;
    }
}
