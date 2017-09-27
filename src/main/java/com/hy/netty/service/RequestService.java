package com.hy.netty.service;

import com.alibaba.fastjson.JSON;
import com.hy.netty.entity.ChatMessage;



public class RequestService {

    /**
     * 根据客户端的请求生成 Client
     *
     * @param request 例如 {id:1,rid:21,token:'43606811c7305ccc6abb2be116579bfd'}
     * @return
     */
    public static ChatMessage buildChatRequest(String message) {
    	ChatMessage chatMessage = JSON.parseObject(message,ChatMessage.class);
        return chatMessage;
    }

}
