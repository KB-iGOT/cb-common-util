package org.igot.common.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiRespParam {
    private String resMsgId;
    private String msgId;
    private String err;
    private String status;
    private String errMsg;

    public ApiRespParam() {
    }

    public ApiRespParam(String id) {
        resMsgId = id;
        msgId = id;
    }
}
