package org.igot.common.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.igot.common.model.ApiRespParam;
import org.igot.common.model.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ProjectUtil {
    public static final String API_VERSION_1 = "1.0";
    public static final String SUCCESS = "success";
    public static final String FAILED = "Failed";

    @Autowired
    private ObjectMapper mapper;

    TypeReference<List<Map<String, Object>>> LIST_OF_MAP_TYPE = new TypeReference<List<Map<String, Object>>>() {
    };

    TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    public ApiResponse createDefaultResponse(String api) {
        ApiResponse response = new ApiResponse();
        response.setId(api);
        response.setVer(API_VERSION_1);
        response.setParams(new ApiRespParam(UUID.randomUUID().toString()));
        response.getParams().setStatus(SUCCESS);
        response.setResponseCode(HttpStatus.OK);
        response.setTs(java.time.LocalDateTime.now().toString());
        return response;
    }

    public void errorResponse(ApiResponse response, String errorMessage, HttpStatus httpStatus) {
        response.setResponseCode(httpStatus);
        response.getParams().setErrMsg(errorMessage);
        response.getParams().setStatus(FAILED);
    }

    public List<Map<String, Object>> parseListOfMap(String json) throws IOException {
        return mapper.readValue(json, LIST_OF_MAP_TYPE);
    }

    public Map<String, Object> parseMap(String json) throws IOException {
        return mapper.readValue(json, MAP_TYPE);
    }

    public String convertToString(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (IOException e) {
            log.error("Error converting object to string: {}", e.getMessage(), e);
            return null;
        }
    }
}
