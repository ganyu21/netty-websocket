package com.hy.netty.dto;

import java.util.HashMap;
import java.util.Map;

public class ChatResponse {
    private int code=0; // 成功时 0 ,如果大于 0 则表示则显示message
    private String message;
    private Map<String,Object> data = new HashMap<String,Object>();

    public ChatResponse() {
    }

    public ChatResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}
}
