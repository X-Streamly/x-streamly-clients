var env = require('./env');
var Members = require('./members');
var Member = require('./member');
module.exports = Channel; 

function Channel(name, appKey, connection,mainXStreamly,options) {
    this.connection = connection;
    this.name = name;
    this.appKey = appKey;
    var securityCheckPassed = undefined;
    options = options || {};
    this.closed = false;
    var self = this;
    this.bindings = [];
    var members = new Members();
    

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
    	if(self.stream){
      	self.stream.close();
      	self.stream = undefined;
     	}
       	
     	self.bindings = [];
     	self.closed = true;
    };

    this.fireEvent = function(eventName, data, dataKey) {
    	if(self.closed){
    		throw new Error("can't fire an event on a closed channel");
    	}
    	
    	if(typeof(data)==='string'){
    	  try{
    	    data= JSON.parse(data);
    	  } catch(ex){
    	    //oh well
    	  }
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
    var triggersWaitingOnSubscriptionLoad = [];
    var subscriptionLoaded=false;
    
    this.trigger = function(eventName, data,persisted) {
    	persisted = persisted || false;
    	if(securityCheckPassed ===false){
    	    throw "Security check not passed yet";
    	} else {
    	  connection.onActive(function(){
          connection.socket.emit("internal:send",{messages:[{AppKey:appKey,Channel:self.name,EventName:eventName, Message:data,Persisted:persisted}]});
        },false);
      }
     };
     
     this.removePersistedMessage = function(key) {
    	if(securityCheckPassed ===false){
    	    throw "Security check not passed yet";
    	}	else if(!subscriptionLoaded){
    		triggersWaitingOnSubscriptionLoad.push(function(){
    			self.removePersistedMessage(key);
    		});
    	} else{
    		var uri =this.uri;
    		connection.onActive(function(){
		        sendDelete(uri,key);
			 },false);
       }
     };
     
     this.memberInfo = function(name,value){
      if(undefined === value && typeof name === 'string') {
        return options.userInfo[name];
      } else {
        if(typeof name === 'string') {
          options.userInfo[name] = value;
        } else if(typeof name === 'object') {
          $.each(name, function(key, value) {
            options.userInfo[key] = value;
          });
        }
        connection.onActive(function(){
          connection.socket.emit('internal:member_update',{appKey:appKey,channel:self.name,memberInfo:options.userInfo});
        },false);
      }
     }


    var itemRecieved = function(item) {
    	item.Message.member = members.socketId(item.SocketId);
      fireEvent(item.EventName, item.Message, undefined);
    };

    var startAction = function() {
        connection.onActive(function(){
          var membersLoaded = false;
          stream = connection.subscribe(name,options.includePersistedMessages,{id:options.userId,memberInfo:options.userInfo},
            function(data){
              for(var key in data.messages){
                itemRecieved(data.messages[key]);
              }
            },
            function(memberInfos){
              for(var memberInfoKey in memberInfos.members){
                var memberInfo = memberInfos.members[memberInfoKey];
                if(memberInfo.action === 'add'){
                  var member = members.get(memberInfo.item.MemberId);
                  
                  if(!member){
                    member = new Member(memberInfo.item.MemberId,memberInfo.item.Info);
                    members.add(member);

                    if(memberInfo.item.MemberId==options.userId){
                      membersLoaded = true;
                      fireEvent("xstreamly:subscription_succeeded",members);
                    } else {
                      fireEvent("xstreamly:member_added",member);
                    }
                  }
                  
                  member.addRecord(memberInfo.item);
                } else if (memberInfo.action === 'delete'){
                  var member = members.get(memberInfo.item.MemberId);
                  if(member){
                    member.removeRecord(memberInfo.item.Key);
                    if(!member.alive()){
                      members.remove(memberInfo.item.MemberId);
                      fireEvent("xstreamly:member_removed",member);
                    }
                  }
                } else if (memberInfo.action === 'modify'){
                  var member = members.get(memberInfo.item.MemberId);
                  if(member){
                    try{
                      member.memberInfo = JSON.parse(memberInfo.item.Info);
                    } catch(ex){
                      env.log('couldn\'t parse member info: '+memberInfo.item.Info+ ' ex: '+ex)
                    }
                    fireEvent("xstreamly:member_modified",member);
                  }
                }
              }
            }
          );
          
        },true);
    };
    
    startAction();
}
