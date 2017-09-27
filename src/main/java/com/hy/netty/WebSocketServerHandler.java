package com.hy.netty;

import static io.netty.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.hy.netty.dto.ChatResponse;
import com.hy.netty.entity.ChatMessage;
import com.hy.netty.service.GroupService;
import com.hy.netty.service.RequestService;
import com.hy.netty.service.ResponseService;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
	
	private Logger logger = LoggerFactory.getLogger(getClass());

    // websocket 服务的 uri
    private static final String WEBSOCKET_PATH = "/chat";

    // groupId : channelGroup
    private static Map<String, ChannelGroup> channelGroupMap = new ConcurrentHashMap<>();
    
    private static Map<String, Set<String>> user2GroupIds=GroupService.initUser2GroupIds();
    
    private static Map<String, ChannelHandlerContext> userId2Ctx = new ConcurrentHashMap<>();
    
    // 本次请求的 code
    private static final String HTTP_USERTOKEN = "user_token";
    private String userId;
    private static final String HTTP_SIGN = "sign";

    private WebSocketServerHandshaker handshaker;

    @Override
    public void messageReceived(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
    	if(!verifyHttpRequest(ctx,req)){
    		return;
    	}

    	handleHandshaker(ctx,req);
        
    }
    
    private void handleHandshaker(ChannelHandlerContext ctx, FullHttpRequest req){
    	// Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, true);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            ChannelFuture channelFuture = handshaker.handshake(ctx.channel(), req);

            // 握手成功之后,业务逻辑
            if (channelFuture.isSuccess()) {
            	System.out.println(userId + " 连接成功");
            	sendOfflineMessage(ctx);
            	
            }
        }
    }

    private void broadcast(ChannelHandlerContext ctx, ChatMessage chatMessage) {
    	String groupId=chatMessage.getBody().getTo();
    	String userToken=chatMessage.getBody().getFrom();
    	String userId=GroupService.findUserIdByUserToken(userToken);
    	Set<String> groupIds=user2GroupIds.get(userId);
    	if(!groupIds.contains(groupId)){
            sendCallback(ctx,chatMessage,111,"用户没有在此群里面！");
            return;
    	}
    	
    	
    	ChannelGroup channelGroup=addChannelToGroup(groupId,ctx);
        
    	sendBroadcast(ctx,chatMessage,channelGroup);
        
        sendCallback(ctx,chatMessage,0,null);

    }
    
    private void sendBroadcast(ChannelHandlerContext ctx, ChatMessage chatMessage,ChannelGroup channelGroup) {
    	ChatResponse response = ResponseService.buildResponse(chatMessage,"broadcast");
        String msg = JSON.toJSONString(response);
        channelGroup.writeAndFlush(new TextWebSocketFrame(msg));
    }
    
    private ChannelGroup addChannelToGroup(String groupId,ChannelHandlerContext ctx){
    	ChannelGroup channelGroup=channelGroupMap.get(groupId);
        if (channelGroup==null) {
        	channelGroup=new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        	channelGroupMap.put(groupId, channelGroup);
        }
        if(!channelGroup.contains(ctx.channel())){
        	channelGroup.add(ctx.channel());
        }
        return channelGroup;
    }
    
    private void sendCallback(ChannelHandlerContext ctx, ChatMessage chatMessage,int code,String message){
    	ChatResponse response = ResponseService.buildResponse(chatMessage,"sendCallback");
    	response.setCode(code);
    	response.setMessage(message);
        String msg = JSON.toJSONString(response);
        ctx.channel().writeAndFlush(new TextWebSocketFrame(msg));
    }
    
    private void sendOfflineMessage(ChannelHandlerContext ctx){
    	List<String> messages=new ArrayList();
    	for(String message : messages){
    		ctx.channel().writeAndFlush(message);
    	}
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {

        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
        }
        
        String message = ((TextWebSocketFrame) frame).text();
        ChatMessage chatMessage = RequestService.buildChatRequest(message);
        if("ping".equals(chatMessage.getType())){
        	Set<String> groupIds=user2GroupIds.get(userId);
        	for(String groupId:groupIds){
        		ChannelGroup channelGroup=addChannelToGroup(groupId,ctx);
        	}
        	userId2Ctx.put(userId, ctx);
        	sendCallback(ctx,chatMessage,0,null);
        	return;
        }
        if("notice".equals(chatMessage.getType())){
        	ChatResponse response = ResponseService.buildResponse(chatMessage,"single");
            String msg = JSON.toJSONString(response);
            userId2Ctx.get(chatMessage.getBody().getTo()).writeAndFlush(new TextWebSocketFrame(msg));
            sendCallback(ctx,chatMessage,0,null);
        	return;
        }
//        if (chatMessage.getBody().getTo() == null) {
//            ChatResponse response = new ChatResponse(1001, "群号不可缺省");
//            String msg = JSON.toJSONString(response);
//            ctx.channel().write(new TextWebSocketFrame(msg));
//            return;
//        }
//        if (chatMessage.getBody().getFrom() == null) {
//            ChatResponse response = new ChatResponse(1001, "没登录不能聊天哦");
//            String msg = JSON.toJSONString(response);
//            ctx.channel().write(new TextWebSocketFrame(msg));
//            return;
//        }
        if("txt".equals(chatMessage.getType())){
        	System.out.println(" 收到 " + chatMessage.getBody().getFrom() + ":"+chatMessage.getValue());
            
        	broadcast(ctx, chatMessage);
        }
        
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpHeaderUtil.setContentLength(res, res.content().readableBytes());
        }

        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpHeaderUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    private boolean verifyHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req){
    	// Handle a bad request.
        if (!req.decoderResult().isSuccess()) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return false;
        }

        // Allow only GET methods.
        if (req.method() != GET) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
            return false;
        }

        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.uri());
        Map<String, List<String>> parameters = queryStringDecoder.parameters();

        if (parameters.size() == 0) {
            System.err.printf("参数不可缺省");
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
            return false;
        }
        if (!parameters.containsKey(HTTP_USERTOKEN)) {
            System.err.printf(HTTP_USERTOKEN + "参数不可缺省");
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
            return false;
        }
        if (!parameters.containsKey(HTTP_SIGN)) {
            System.err.printf(HTTP_SIGN + "参数不可缺省");
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND));
            return false;
        }
        
        String sign=parameters.get(HTTP_SIGN).get(0);
        if(!verifySign(sign)){
        	System.err.printf(HTTP_SIGN + "验证不通过");
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return false;
        }

        String userToken=parameters.get(HTTP_USERTOKEN).get(0);
        this.userId=GroupService.findUserIdByUserToken(userToken);
        
        
        return true;
    }
    
    
    private boolean verifySign(String sign){
    	if("Iamkey".equals(sign)){
    		return true;
    	}else{
    		return false;
    	}
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Channel incoming = ctx.channel();
        System.out.println("收到" + incoming.remoteAddress() + " 握手请求");
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
    	
        if (this.userId != null ) {
        	Set<String> groupIds=user2GroupIds.get(this.userId);
        	for(String groupId:groupIds){
        		if(channelGroupMap.get(groupId)!=null)
        			channelGroupMap.get(groupId).remove(ctx.channel());
        	}
        }
        System.out.println(this.userId + "退出 成功");
    }

    private static String getWebSocketLocation(FullHttpRequest req) {
        String location = req.headers().get(HOST) + WEBSOCKET_PATH;
        return "ws://" + location;
    }

	
}
