var io = require('socket.io-client-xstreamly');
var env = require('./env');

module.exports = Connection;

function Connection(appKey) {
    var bindings = [];
    var subscriptionCount = 0;
    this.state = "initialized";
    this.id=0;
    this.securityTokens = [];
    
    var me = this;
    
    this.bind = function(eventName, callback) {
        bindings.push({ name: eventName, func: callback });
    };
    
    //sets the address that socket.io should connect to
    //can only be casued once
    this.setAddress = function(address){
      if(!this.socket){
        try {
          if (null != io && undefined != io) {
            env.log('trying to connect to '+address);
            me.socket = io.connect(address, {
              "force new connection": true,
            });

            me.setStatus("connecting");
            
            me.socket.on('connecting',function(transport){
              numberOfConnections++;
              env.log('atempting to connect via: '+transport);

            });
            var numberOfConnections = 0;
            me.socket.on('reconnect',function(transport,numTries){
              env.log('atempting to reconect via: '+transport+ ' for the '+numTries+' times');
              if(numberOfConnections>10){
                env.log('stopping trying to connect');
                me.socket.disconnect();
                me.setStatus("error");
              }
            });
          }
          else {
            me.setStatus("unavailible");
          }
                 
          me.socket.on('connect', function() {
            me.id=me.socket.socket.sessionid;
              me.setStatus('awaiting-security-clearance');
              for(var key in me.securityTokens){
                var token = me.securityTokens[key];
                if(token){
                  sendSecurityToken(token);
                }
              }
          });
          
          
          me.socket.on('disconnect', function() {
              me.setStatus("disconnected");
          });
          
          me.socket.on('error', function(msg) {
            if(typeof(msg)=="string"){
                env.log("socket error: " + msg);
              }
              else{
                env.log("socket error: "+JSON.stringify(msg));
              }
          });
          
          me.socket.on('internal:securityTokenResult',function(item){
            env.log('got security call back: '+item.error+' valid: '+item.valid);
            if(item.valid && me.state!=='connected'){
              me.setStatus('connected');
            }
            else if (!item.valid && me.state ==='awaiting-security-clearance'){
              me.setStatus('security-check-failed');
            }
          });
          
        }
        catch (ex) {
            env.log("problem connecting to socket " + ex);
            this.setStatus("failed");
            return;
        }
      }
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

    var sendSecurityToken = function (token){
  	  env.log('sending security token: '+token);
  	  me.socket.emit('internal:securityToken',token);
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
    
    this.subscribe = function(name,includePersistedMessages,member,dataCallback,memberCallback){
      var me = this;
      subscriptionCount++;
      var id = me.id +'|'+name+'|'+subscriptionCount;
      
      me.socket.on('internal:data:'+id,dataCallback);
      me.socket.on('internal:member:'+id,memberCallback);
      
      var subscriptionData = {appKey:appKey,channel:name,id:id,member:member,includePersistedMessages:includePersistedMessages};
      
      me.socket.emit('internal:subscribe',subscriptionData);
      
      return {
        close: function(){
          me.socket.emit('internal:unsubscribe',subscriptionData);
        }
      };
    };

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
    
    this.setStatus("waiting-for-address");
}
