var FmzgIM = function() {}

FmzgIM.connection = function(domain,userToken) {
	this.from=userToken;
	this._msgHash={};
    if (!window.WebSocket) {
        window.WebSocket = window.MozWebSocket;
    }
    if (window.WebSocket) {
    	var sign='Iamkey';
    	this.socket = new WebSocket(domain+"/chat/?user_token="+userToken+"&sign="+sign);
    	
    	//ping
    	var id = this.getUniqueId(); 
    	var msg = new FmzgIM.message(id,"ping");      // 创建文本消息
        msg.set({
        	success: function (id, serverMsgId) {
            },
            fail: function (message) {
            }
        });
    	this.send(msg);
    }
    
}

var _msgHash={};

FmzgIM.connection.prototype.listen = function(opt) {
	this.socket.onmessage = function (event) {
		var msgStr=event.data;
        var responseObj=JSON.parse(msgStr);
        var type=responseObj.data.type;
        if(type=="sendCallback"){
        	var code = responseObj.code;
        	var msgId = responseObj.data.id;
            var msgServerId = responseObj.data.serverMsgId;
            var msg=_msgHash[msgId];
        	if(code!='0'){
        		msg.body.fail(responseObj.message);
        	}else{
        		msg.body.success(msgId,msgServerId);
        	}
        	_msgHash[msgId]=null;
        }else{
        	opt.onMessage(event.data);
        }
        
    };
    this.socket.onopen = function (event) {
        opt.onOpened(event.data);
    };
    this.socket.onclose = function (event) {
        opt.onClosed(event.data);
    };
}

FmzgIM.connection.prototype.getUniqueId = function() {
	var cdate = new Date();
    var offdate = new Date(2010, 1, 1);
    var offset = cdate.getTime() - offdate.getTime();
    var hexd = parseInt(offset).toString(16);

    return 'FmzgIM_' + this.from +'_'+ hexd;
}

FmzgIM.connection.prototype.send = function(msg) {
	if (!window.WebSocket) {
        return;
    }
	msg.body.from=this.from;
    if (this.socket.readyState == WebSocket.OPEN) {
    	_msgHash[msg.id]=msg;
    	this.socket.send(JSON.stringify(msg));
    } else {
        console.log("The socket is not open.");
    }
}

FmzgIM.message = function(id,type) {
	this.id = id;
	//txt,image,newhouse,ershoufang,zufang
	this.type=type;
	this.body = {};
}

FmzgIM.message.prototype.set = function (opt) {
    this.value = opt.msg;
    this.body = {
        id: this.id,
        //userId 或 groupId
        to: opt.to,
        msg: opt.msg,
        //groupChat,singleChat
        chatType: opt.chatType,
        ext: opt.ext || {},
        success: opt.success,
        fail : opt.fail
    };

};