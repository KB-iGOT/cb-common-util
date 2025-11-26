package org.igot.common;

import lombok.Getter;
import lombok.Setter;

/**
 * Standard API response parameters.
 */
@Getter
@Setter
public class ApiRespParam {
    private String resMsgId;
	private String msgId;
	private String err;
	private String status;
	private String errMsg;

	/**
	 * Default constructor.
	 */
    public ApiRespParam() {
	}

	/**
	 * Constructor with ID parameter.
	 * @param id
	 */
	public ApiRespParam(String id) {
		resMsgId = id;
		msgId = id;
	}
}
