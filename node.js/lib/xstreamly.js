var Channel = require('./channel');
var env = require ('./env');
var http = require('http');
var Connection = require('./connection');

module.exports= XStreamly;

XStreamly.overrideServer = undefined;
XStreamly.restEndPoint = 'api.x-stream.ly';

function XStreamly(key,securityToken){
  this.key = key;
  this.connection = new Connection(key);
  this.onActiveBindings = [];
  var self = this;
  
  
  if(XStreamly.overrideServer){
      this.connection.setAddress(XStreamly.overrideServer);
  } else {
    http.get({
        host: XStreamly.restEndPoint,
        port: 80,
        path: '/api/v1.1/'+key+'/appPool/'
      },
      function(res){
        var data='';
        res.on('data',function(chunk){
          data+=chunk;
        });
        res.on('end',function(){
          try{
            var responseObject = JSON.parse(data);
            self.connection.setAddress(responseObject.machine);
          } catch(ex){
            env.log('got error parsing: '+data+ ' '+ex.message);
          }
        });      }
    ).on('error',function(e){
      env.log('got error: '+e.message);
    });
  }
  
  this.connection.onActive(function(){
  	self.onActiveBindings.forEach(function(value){
  	  try{
  		  value(self.connection.socket.socket.sessionid);
  		} catch (ex){
  		  env.reportError(ex);
  		}
  	});
  });
  
  
  this.connection.applySession(securityToken);
      
  this.subscribe = function(channelName,options) {
  	if(!channelName.match(/^[\w\s:-]+$/)){
  		throw channelName+" isn't a valid name, channel names should only contains letters numbers and -_";
  	}
  	options = options || {};

    return new Channel(channelName, this.key, this.connection,this,options);
  };

  
  this.listActiveChannels = function(callback) {
    var id = this.connection.id+'|activeChannels'+Math.random();
    var closed= false;
    var subscriptionData = {id:id,appKey:key};
    this.connection.onActive(function(connection){
      connection.socket.emit('internal:channels:subscribe',subscriptionData);
      
      connection.socket.on('internal:channel:'+id,function(data){
        if(!closed){
          for(var key in data){
            callback(data[key]);
          }
        }
      });
    },true);
    
    return {close:function(){
      closed = true;
      self.connection.onActive(function(connection){
        connection.socket.emit('internal:channels:unsubscribe',subscriptionData);
      },false);
    }}
  };


  this.stop = function() {
  	env.log('stopping');
    this.connection.stop();

    this.onActiveBindings = []
  };
  
  this.addSecurityToken = function(securityToken){
  	this.connection.applySession(securityToken);
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
}
