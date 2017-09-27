package com.hy.netty.entity;

import java.util.HashMap;
import java.util.Map;

public class ChatBody {
	
	private String id;
	private String to;
	private String from;
	private String msg;
	private String chatType;
	private Map ext=new HashMap();
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public String getChatType() {
		return chatType;
	}
	public void setChatType(String chatType) {
		this.chatType = chatType;
	}
	public Map getExt() {
		return ext;
	}
	public void setExt(Map ext) {
		this.ext = ext;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	
	

}
