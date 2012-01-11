var env = require('./env');
var PresenceChannel = require('./presenceChannel');
module.exports = Channel; 

function Channel(name, appKey, cerrio,mainXStreamly,options) {
    this.cerrio = cerrio;
    this.name = name;
    this.appKey = appKey;
    var securityCheckPassed = undefined;
    options = options || {};
    this.uri = env.MESSAGE_URL;
    var createPresenceChannel = true;
    var isPrivate = false;
    
    if(options.presence!==undefined){
    	createPresenceChannel = options.presence;
    }
    
    if(options.private){
    	isPrivate = options.private;
    }
    
    this.closed = false;
    this.presenceChannel = null;
    var self = this;
    
    this.bindings = [];

    this.bind = function(eventName, callback) {
    	if(self.closed){
    		throw new Error("can't bind to a closed channel");
    	}
      self.bindings.push({ name: eventName, func: callback });
    };

    this.bind_all = function(callback) {
    	if(self.closed){
    		throw new Error("can't bind to a closed channel");
    	}
        this.bind(undefined, callback);
    };

    this.close = function() {
    
        if(self.presenceChannel){
       		self.presenceChannel.close();
       		self.presenceChannel = undefined;
       	}
       	
    	if(self.stream){
        	self.stream.close();
        	self.stream = undefined;
       	}
       	
       	self.bindings = [];
       	
       	delete mainXStreamly.channels[name];
       	self.closed = true;
    };

    this.fireEvent = function(eventName, data, dataKey) {
    	if(self.closed){
    		throw new Error("can't fire an event on a closed channel");
    	}
      for (key in self.bindings) {
        if(self.bindings.hasOwnProperty(key)){
          try{
          	var binding = self.bindings[key];
          	if(binding && binding.func){
	            var event = binding.name;
	            if (event == eventName) {
	                binding.func(data, dataKey);
	            } else if (event == undefined) {
	                binding.func(eventName, data, dataKey);
	            }
	          }
          } catch (ex){
            env.reportError(ex);
          }
	      }
      }
    };

    var fireEvent = this.fireEvent;
    var uri = this.uri;
    var triggersWaitingOnSubscriptionLoad = [];
    var subscriptionLoaded=false;
    var nonPersistedId;
    
    this.trigger = function(eventName, data,persisted) {
    	var me =this;
    	persisted = persisted || false;
    	if(securityCheckPassed ===false){
    	    throw "Security check not passed yet";
    	}
    	else if(!subscriptionLoaded){
    		triggersWaitingOnSubscriptionLoad.push(function(){
    			me.trigger(eventName,data,persisted);
    		});
    	} else{
    		var uri =this.uri;
    		cerrio.connection.onActive(function(){
    		    if(nonPersistedId && persisted===false){
    		        cerrio.modify(uri,{
					    Key:nonPersistedId,
			            Channel: name,
			            AppKey:appKey,
			            EventName: eventName,
			            TimeStamp :(new Date()).toUTCString(),
			            Message : JSON.stringify(data),
			            SocketId :cerrio.connection.id,
			            Persisted: persisted,
			            Private : isPrivate
			   	    });
    		    }
    		    else{
    		        cerrio.add(uri,{
					    Key:'',
			            Channel: name,
			            AppKey:appKey,
			            EventName: eventName,
			            TimeStamp :(new Date()).toUTCString(),
			            Message : JSON.stringify(data),
			            SocketId :cerrio.connection.id,
			            Persisted: persisted,
			            Source:'',
			            ClientInfo:'',
			            Private : isPrivate
			   	    });
    		    }
			 },false);
       }
     };
     
     this.removePersistedMessage = function(key) {
    	var me =this;
    	if(securityCheckPassed ===false){
    	    throw "Security check not passed yet";
    	}
    	else if(!subscriptionLoaded){
    		triggersWaitingOnSubscriptionLoad.push(function(){
    			me.removePersistedMessage(key);
    		});
    	} else{
    		var uri =this.uri;
    		cerrio.connection.onActive(function(){
		        cerrio.sendDelete(uri,key);
			 },false);
       }
     };

    var mainChannel = this;

    var itemRecieved = function(item) {
    	try{
		    var data = JSON.parse(item.Message);

		    if (self.presenceChannel) {
		        data.member = self.presenceChannel.members.socketId(item.SocketId);
		    }
      } catch(ex){
    	  data = item.Message;
      }
      fireEvent(item.EventName, data, item.Persisted ? item.Key : undefined);
    };

    var startAction = function() {
        self.stream = cerrio.subscribe({
            url: uri,
            subscription: "@.AppKey='" + appKey + "' and @.Channel ='" + name + "' and @.SocketId != 'placeholder' and @.Private="+isPrivate,
            addAction: function(item) {
                if (item.Persisted === false) {
                    nonPersistedId = item.Key;
                }
                if ((options.includePersistedMessages && item.Persisted) || subscriptionLoaded) {
                    if (item.SocketId != cerrio.connection.id || options.includeMyMessages) {
                        itemRecieved(item);
                    }
                }
            },
            modifyAction: function(item) {
                if (item.SocketId != cerrio.connection.id || options.includeMyMessages) {
                    itemRecieved(item);
                }
            },
            //updatesOnly:updatesOnly,
            subscriptionLoaded: function() {
                subscriptionLoaded = true;
                for (t in triggersWaitingOnSubscriptionLoad) {
                  try{
                    triggersWaitingOnSubscriptionLoad[t]();
                  }
                  catch(ex){
                    XStreamly.log(ex);
                  }
                }
                if(options.subscriptionLoaded){
                  options.subscriptionLoaded();
                }
            },
            streamResetAction:function(){
            	subscriptionLoaded = false;
            	nonPersistedId = undefined;
            }
        });

        if (createPresenceChannel) {
            self.presenceChannel = new PresenceChannel(name, appKey, cerrio, options.userId, options.userInfo, mainChannel,isPrivate);
            self.presenceChannel.start();
            mainChannel.memberId = self.presenceChannel.memberId;
            mainChannel.memberInfo = function(name, value) {
              self.presenceChannel.memberInfo(name, value);
            };
        }
    };
    
    startAction();
}
