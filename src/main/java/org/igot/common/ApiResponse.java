package org.igot.common;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;

/**
 * Standard API response structure.
 */
@Getter
@Setter
public class ApiResponse {
    private String id;
	private String ver;
	private String ts;
	private ApiRespParam params;
	private HttpStatus responseCode;
	private transient Map<String, Object> response = new HashMap<>();

	/**
	 * Default constructor initializing version, timestamp, and parameters.
	 */
	public ApiResponse() {
		this.ver = "v1";
		this.ts = Instant.now().toString();
		this.params = new ApiRespParam(UUID.randomUUID().toString());
	}
	
	/**
	 * Constructor with ID parameter.
	 * @param id
	 */
	public ApiResponse(String id) {
		this();
		this.id = id;
	}

	/**
	 * Gets a value from the response map.
	 * @param key 	the key to look up
	 * @return	the value associated with the keya
	 */
	public Object get(String key) {
        return response.get(key);
    }

	/**
	 * Puts a key-value pair into the response map.
	 * @param key 	the key
	 * @param vo 	the value object
	 */
    public void put(String key, Object vo) {
        response.put(key, vo);
    }

	/**
	 * Puts all entries from the given map into the response map.
	 * @param map	
	 */
    public void putAll(Map<String, Object> map) {
        response.putAll(map);
    }

	/**
	 * Checks if the response map contains the given key.
	 * @param key 	the key to check
	 * @return 	true if the key exists, false otherwise
	 */
    public boolean containsKey(String key) {
        return response.containsKey(key);
    }

	/**
	 * Gets the entire response map.
	 * @return the response map
	 */
	public Map<String, Object> getResult() {
        return response;
    }

	/**
	 * Sets the entire response map.
	 * @param result 
	 */
    public void setResult(Map<String, Object> result) {
        response = result;
    }

	/**
	 * Creates a default ApiResponse for the given API.
	 * @param api the API identifier
	 * @return the default ApiResponse object
	 */
	public static ApiResponse createDefaultResponse(String api) {
        ApiResponse response = new ApiResponse();
        response.setId(api);
        response.setVer(CommonConstants.API_VERSION_1);
        response.setParams(new ApiRespParam(UUID.randomUUID().toString()));
        response.getParams().setStatus(CommonConstants.SUCCESS);
        response.setResponseCode(HttpStatus.OK);
        response.setTs(Instant.now().toString());
        return response;
    }

	/**
	 * Updates error details in the response.
	 * @param errMsg the error message 
	 * @param responseCode the HTTP response code
	 */
    public void updateErrorDetails(String errMsg, HttpStatus responseCode) {
        this.getParams().setStatus(CommonConstants.FAILED);
        this.getParams().setErrMsg(errMsg);
        this.setResponseCode(responseCode);
    }
}
