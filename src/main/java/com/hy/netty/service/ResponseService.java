package com.hy.netty.service;

import com.hy.netty.dto.ChatResponse;
import com.hy.netty.entity.ChatMessage;

public class ResponseService {

    public static ChatResponse buildResponse(ChatMessage chatMessage,String type) {
        ChatResponse res = new ChatResponse();
        res.getData().put("id", chatMessage.getId());
        res.getData().put("type", type);
        res.getData().put("serverMsgId", chatMessage.getId());
        res.getData().put("from", chatMessage.getBody().getFrom());
        res.getData().put("message", chatMessage.getBody().getMsg());
        return res;
    }
}
