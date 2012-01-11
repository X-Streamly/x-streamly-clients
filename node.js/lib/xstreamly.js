var Channel = require('./channel');
var Cerrio = require('./cerrio/cerrio');
var env = require ('./env');

module.exports= XStreamly;

function XStreamly(key,securityToken){
  this.key = key;
  this.cerrio = new Cerrio(env.port);
  this.connection = this.cerrio.connection;
  this.channels = {};
  this.bindings = [];
  this.onActiveBindings = [];
  var self = this;
      
  this.subscribe = function(channelName,options) {
  	if(!channelName.match(/^[\w\s:-]+$/)){
  		throw channelName+" isn't a valid name, channel names should only contains letters numbers and -_";
  	}
  	options = options || {};
      if (channelName in this.channels) {
          return this.channels[channelName];
      }

      var channel = new Channel(channelName, this.key, this.cerrio,this,options);
      this.channels[channelName] = channel;

      for (key in this.bindings) {
          channel.bind(this.bindings[key].name,this.bindings[key].func);
      }
      
      return channel;
  };

  this.listChannels = function(callback) {
      var knownChannels = {};
      var subscription = this.cerrio.subscribe({
          url: env.MESSAGE_URL,
          //only get adds
          subscription: "modified(@.Key) and @.AppKey='" + this.key + "'",
          addAction: function(item) {
          if (!knownChannels.hasOwnProperty(item.Channel)) {
                  knownChannels[item.Channel] = true;
                  callback(item.Channel);
              }
          }
      });

      return { cancel: 
      	function() {
          	subscription.close();
      	} 
      };
  };
  
  this.listActiveChannels = function(callback) {
      var knownChannels = {};
      env.log(this.key);
      var subscription = this.cerrio.subscribe({
          url: 'PicTacToe/XStreamly/StatsPresence',
          //only get adds
          subscription: "@.AppKey='" + this.key + "'",
          addAction: function(item) {
                knownChannels[item.Key] = item;
                callback(item);
          },
          modifyAction: function(item) {
                var saved = knownChannels[item.Key];
                if(undefined!==item.MaxConcurrentConnections){
                  saved.MaxConcurrentConnections = item.MaxConcurrentConnections;
                }
                
                if(undefined!==item.CurrentConcurrentConnections){
                  saved.CurrentConcurrentConnections = item.CurrentConcurrentConnections;
                }
                callback(saved);
          }
      });

      return { cancel: 
      	function() {
          	subscription.close();
      	} 
      };
  };

  this.unsubscribe = function(channelName) {
      if (channelName in this.channels) {
          this.channels[channelName].close();
          delete this.channels[channelName];
      }
  };
  
  this.deleteChannel = function(channelName){
    var subscription = this.cerrio.subscribe({
          url: env.MESSAGE_URL,
          //only get adds
          subscription: "@.AppKey='" + this.key + "' and @.Channel = '"+channelName+"'",
          addAction: function(item) {
                self.cerrio.sendDelete(env.MESSAGE_URL,item.Key);
          },
          subscriptionLoaded:function(){
            subscription.close();
          }
      });
  }
  
  this.channel = function(channelName){
      return this.channels[channelName];
  };

  this.bind = function(eventName, callback) {
      for (key in this.channels) {
          var channel = this.channels[key].bind(eventName, callback);
      }
      this.bindings.push({ name: eventName, func: callback});
  };

  this.bind_all = function(callback) {
      this.bind(undefined, callback);
  }
  
  this.stop = function() {
  	env.log('stopping');
      this.cerrio.stop();
      for (key in this.channels) {
          this.channels[key].close();
      }
      this.channels = {};
      this.onActiveBindings = [];
      this.bindings = [];
  };
  
  this.addSecurityToken = function(securityToken){
  	this.cerrio.applySession(securityToken);
  };
  
  this.onActive = function(callback){
  	this.onActiveBindings.push(callback);
  };
  
  this.removeOnActive = function(callback){
  	var index = this.onActiveBindings.indexOf(callback);
  	if(index>-1){
  		this.onActiveBindings.splice(index,1);
  	}
  };
  
  this.cerrio.connection.onActive(function(){
  	self.onActiveBindings.forEach(function(value){
  	  try{
  		  value(self.cerrio.connection.socket.socket.sessionid);
  		} catch (ex){
  		  env.reportError(ex);
  		}
  	});
  });
  
  
  this.cerrio.applySession(securityToken);
}
