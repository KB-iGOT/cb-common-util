package org.igot.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiRespParamTest {

    @Test
    @DisplayName("Should create ApiRespParam with default constructor")
    void constructor_Default_CreatesEmptyObject() {
        ApiRespParam param = new ApiRespParam();

        assertNull(param.getResMsgId());
        assertNull(param.getMsgId());
        assertNull(param.getErr());
        assertNull(param.getStatus());
        assertNull(param.getErrMsg());
    }

    @Test
    @DisplayName("Should create ApiRespParam with id constructor")
    void constructor_WithId_SetsResmsgidAndMsgid() {
        String id = "test-message-id";
        ApiRespParam param = new ApiRespParam(id);

        assertEquals(id, param.getResMsgId());
        assertEquals(id, param.getMsgId());
        assertNull(param.getErr());
        assertNull(param.getStatus());
        assertNull(param.getErrMsg());
    }

    @Test
    @DisplayName("Should set and get resmsgid")
    void setResmsgid_SetsCorrectly() {
        ApiRespParam param = new ApiRespParam();
        param.setResMsgId("response-msg-id");

        assertEquals("response-msg-id", param.getResMsgId());
    }

    @Test
    @DisplayName("Should set and get msgid")
    void setMsgid_SetsCorrectly() {
        ApiRespParam param = new ApiRespParam();
        param.setMsgId("message-id");

        assertEquals("message-id", param.getMsgId());
    }

    @Test
    @DisplayName("Should set and get err")
    void setErr_SetsCorrectly() {
        ApiRespParam param = new ApiRespParam();
        param.setErr("ERROR_CODE");

        assertEquals("ERROR_CODE", param.getErr());
    }

    @Test
    @DisplayName("Should set and get status")
    void setStatus_SetsCorrectly() {
        ApiRespParam param = new ApiRespParam();
        param.setStatus("SUCCESS");

        assertEquals("SUCCESS", param.getStatus());
    }

    @Test
    @DisplayName("Should set and get errmsg")
    void setErrmsg_SetsCorrectly() {
        ApiRespParam param = new ApiRespParam();
        param.setErrMsg("Error message description");

        assertEquals("Error message description", param.getErrMsg());
    }

    @Test
    @DisplayName("Should handle FAILED status")
    void setStatus_Failed_SetsCorrectly() {
        ApiRespParam param = new ApiRespParam();
        param.setStatus(CommonConstants.FAILED);

        assertEquals(CommonConstants.FAILED, param.getStatus());
    }

    @Test
    @DisplayName("Should set error details correctly")
    void setErrorDetails_SetsAllErrorFields() {
        ApiRespParam param = new ApiRespParam("test-id");
        param.setStatus("FAILED");
        param.setErr("AUTH_ERROR");
        param.setErrMsg("Authentication failed");

        assertEquals("FAILED", param.getStatus());
        assertEquals("AUTH_ERROR", param.getErr());
        assertEquals("Authentication failed", param.getErrMsg());
        assertEquals("test-id", param.getResMsgId());
        assertEquals("test-id", param.getMsgId());
    }

    @Test
    @DisplayName("Should handle empty strings")
    void setFields_EmptyStrings_SetsCorrectly() {
        ApiRespParam param = new ApiRespParam();
        param.setResMsgId("");
        param.setMsgId("");
        param.setErr("");
        param.setStatus("");
        param.setErrMsg("");

        assertEquals("", param.getResMsgId());
        assertEquals("", param.getMsgId());
        assertEquals("", param.getErr());
        assertEquals("", param.getStatus());
        assertEquals("", param.getErrMsg());
    }

    @Test
    @DisplayName("Should handle null values")
    void setFields_NullValues_SetsCorrectly() {
        ApiRespParam param = new ApiRespParam("initial-id");
        param.setResMsgId(null);
        param.setMsgId(null);
        param.setErr(null);
        param.setStatus(null);
        param.setErrMsg(null);

        assertNull(param.getResMsgId());
        assertNull(param.getMsgId());
        assertNull(param.getErr());
        assertNull(param.getStatus());
        assertNull(param.getErrMsg());
    }

    @Test
    @DisplayName("Should handle long strings")
    void setFields_LongStrings_SetsCorrectly() {
        ApiRespParam param = new ApiRespParam();
        String longString = "A".repeat(1000);

        param.setErrMsg(longString);

        assertEquals(longString, param.getErrMsg());
        assertEquals(1000, param.getErrMsg().length());
    }

    @Test
    @DisplayName("Should handle special characters")
    void setFields_SpecialCharacters_SetsCorrectly() {
        ApiRespParam param = new ApiRespParam();
        String specialChars = "Error: !@#$%^&*()_+-=[]{}|;':\",./<>?";

        param.setErrMsg(specialChars);

        assertEquals(specialChars, param.getErrMsg());
    }

    @Test
    @DisplayName("Should handle Unicode characters")
    void setFields_Unicode_SetsCorrectly() {
        ApiRespParam param = new ApiRespParam();
        String unicode = "错误信息 🚨 خطأ";

        param.setErrMsg(unicode);

        assertEquals(unicode, param.getErrMsg());
    }
}
