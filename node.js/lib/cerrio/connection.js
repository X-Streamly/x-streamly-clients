var Cerrio = require('./cerrio');
var io = require('socket.io-client-xstreamly');
var env = require('../env');

module.exports = Connection;

function Connection(port) {
    var bindings = [];
    this.state = "initialized";
    this.id=0;
    this.securityTokens = [];
    
    port = port || 443;
    var me = this;
    
    this.bind = function(eventName, callback) {
        bindings.push({ name: eventName, func: callback });
    };

    this.onActive = function(action, repeat) {
        var fired = false;
        if (this.state === "connected") {
            action(this);
            fired = true;
        }

        if (!fired || repeat) {
            me.bind("connected", function() {
                if (!fired || repeat) {
                    fired = true;
                    action(me);
                }
            });
        }
    }
    
    this.stop = function(){
    	if(me.socket){
            me.socket.disconnect();
        }
    }
    
    var i=0;
    
    var sendSecurityToken = function (token){
      //this is a hack to try to prevent Nurph from spaming our server
      if(i<25){
        i++;
    	  env.log('sending security token: '+token);
    	  me.socket.emit('cerrio:session',token);
    	}
    }
    
    this.applySession = function(token){
    	me.securityTokens.push(token);
    	
    	//if we are already connected send the token over
    	if(me.state==='awaiting-security-clearance'
    	||me.state==='security-check-failed'
    	||me.state==='connected'){
    		sendSecurityToken(token);
    	}
    };

    this.setStatus = function(status) {
        env.log("changing status from " + this.state + " to " + status);
        this.state = status;
        fireEvent(status);
        
    };

    try {
        if (null != io && undefined != io) {
            this.socket = io.connect("https://api.cerrio.com:"+port, {
            "force new connection": true,
            "sync disconnect on unload": false
            });
            this.setStatus("connecting");
        }
        else {
            this.setStatus("unavailible");
        }
    }
    catch (ex) {
        env.log("problem connecting to socket " + ex);
        this.setStatus("failed");
        return;
    }

    this.socket.on('connect', function() {
    	me.id=me.socket.socket.sessionid;
        me.setStatus('awaiting-security-clearance');
        me.securityTokens.forEach(function(token){
          if(token){
        	  sendSecurityToken(token);
        	}
        });
    });
    this.socket.on('disconnect', function() {
        me.setStatus("disconnected");
    });
    this.socket.on('error', function(msg) {
    	if(typeof(msg)=="string"){
        	env.log("socket error: " + msg);
       	}
       	else{
       		env.log("socket error: "+JSON.stringify(msg));
       	}
    });
    
    this.socket.on('cerrio:session-started',function(item){
    	env.log('got security call back: '+item.error+' valid: '+item.valid);
    	if(item.valid && me.state!=='connected'){
    		me.setStatus('connected');
    	}
    	else if (!item.valid && me.state ==='awaiting-security-clearance'){
    		me.setStatus('security-check-failed');
    	}
    });

    function fireEvent(eventName, data) {
        env.log("firing event: " + eventName);
        for (var key in bindings) {
          if(bindings.hasOwnProperty(key)){
            var event = bindings[key].name;
            if (event == eventName) {
                bindings[key].func(data);
            }
            else if (event === undefined) {
                bindings[key].func(eventName, data);
            }
          }
        }
    }
}
