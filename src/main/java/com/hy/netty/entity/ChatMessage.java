package com.hy.netty.entity;

public class ChatMessage {
    private String id;
    private String type;
    private String value;
    
	private ChatBody body=new ChatBody();
    
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public ChatBody getBody() {
		return body;
	}
	public void setBody(ChatBody body) {
		this.body = body;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
    

}
