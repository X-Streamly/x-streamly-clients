package ly.xstream 
{
	/**
	 * ...
	 * @author Brian Willard
	 */
	public class XStreamlyClient 
	{
		static public var port:int = 0;
		static private const validChannels:RegExp = /^[\w\s:-]+$/i; 
		static public const MESSAGE_URL:String = 'PicTacToe/XStreamly/Messages';
		
		private var key:String;
		private var securityToken:String;
		
		private var cerrio:Cerrio;
		
		private var options:Object;
		
		internal var channels:Object = { };
		private var bindings:Array = [];
		private var onActiveBindings:Array = [];
		
		public static function reportError(error:Error):void {
			trace("ERROR: "+error.message+ " "+error.getStackTrace());
		}
		
		public static function log(message:String):void {
			trace(message);
		}
		
		
		function XStreamlyClient(key:String,securityToken:String) {
			this.key = key;
			var me:XStreamlyClient = this;
			this.securityToken = securityToken;
			this.cerrio = new Cerrio(XStreamlyClient.port);
			
			this.cerrio.connection.onActive(function():void {
				for each(var value:Function in me.onActiveBindings){
				  try {
						if (value.length == 0) {
							value();
						} else {
							value(me.cerrio.connection.socket.sessionID);
						}
					} catch (ex :Error){
						XStreamlyClient.reportError(ex);
					}
				}
			},false);
		  
		  
			this.cerrio.applySession(securityToken);
		}
		
		public function subscribe(channelName:String, options:Object):Channel {
			if(!XStreamlyClient.validChannels.test(channelName)){
				throw new Error(channelName+" isn't a valid name, channel names should only contains letters numbers and -_");
			}
			options = options || {};
			if (channelName in this.channels) {
				return this.channels[channelName];
			}

			var channel:Channel = new Channel(channelName, this.key, this.cerrio,this,options);
			this.channels[channelName] = channel;

			for (var key:String in this.bindings) {
				channel.bind(this.bindings[key].name,this.bindings[key].func);
			}
			  
			return channel;
		}
		
		public function listChannels(callback:Function):Object {
		  var knownChannels:Object = {};
		  var subscription:Stream = this.cerrio.subscribe({
			  url: XStreamlyClient.MESSAGE_URL,
			  //only get adds
			  subscription: "modified(@.Key) and @.AppKey='" + this.key + "'",
			  addAction: function(item:Object):void {
			  if (!knownChannels.hasOwnProperty(item.Channel)) {
					  knownChannels[item.Channel] = true;
					  callback(item.Channel);
				  }
			  }
		  });

		  return { cancel: 
			function():void {
				subscription.close();
			} 
		  };
		}
	  
		public function listActiveChannels(callback:Function):Object {
		  var knownChannels:Object = {};
		  XStreamlyClient.log(this.key);
		  var subscription:Stream = this.cerrio.subscribe({
			  url: 'PicTacToe/XStreamly/StatsPresence',
			  //only get adds
			  subscription: "@.AppKey='" + this.key + "'",
			  addAction: function(item:Object):void {
					knownChannels[item.Key] = item;
					callback(item);
			  },
			  modifyAction: function(item:Object):void {
					var saved:Object = knownChannels[item.Key];
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
				function():void {
					subscription.close();
				} 
			};
		}

		public function unsubscribe(channelName:String):void {
		  if (channelName in this.channels) {
			  this.channels[channelName].close();
			  delete this.channels[channelName];
		  }
		}
	  
		public function deleteChannel(channelName:String):void {
			var me:XStreamlyClient = this;
			var subscription:Stream = this.cerrio.subscribe({
			url: XStreamlyClient.MESSAGE_URL,
			  //only get adds
			  subscription: "@.AppKey='" + me.key + "' and @.Channel = '"+channelName+"'",
			  addAction: function(item:Object):void {
					me.cerrio.sendDelete(XStreamlyClient.MESSAGE_URL,item.Key);
			  },
			  subscriptionLoaded:function():void{
				subscription.close();
			  }
		  });
		}
	  
		public function channel(channelName:String):Channel{
			return this.channels[channelName];
		};

		public function bind(eventName:String, callback:Function):void {
			for (var key:String in this.channels) {
				this.channels[key].bind(eventName, callback);
			}
			this.bindings.push({ name: eventName, func: callback});
		}

		public function bindAll(callback:Function):void {
			this.bind(null, callback);
		}
	  
		public function stop():void {
			XStreamlyClient.log('stopping');
			this.cerrio.stop();
			for (var key:String in this.channels) {
				this.channels[key].close();
			}
			this.channels = {};
			this.onActiveBindings = [];
			this.bindings = [];
		}
	  
		public function addSecurityToken(securityToken:String):void{
			this.cerrio.applySession(securityToken);
		}
	  
		public function onActive(callback:Function):void{
			this.onActiveBindings.push(callback);
		}
	  
		public function removeOnActive(callback:Function):void{
			var index:int = this.onActiveBindings.indexOf(callback);
			if(index>-1){
				this.onActiveBindings.splice(index,1);
			}
		}
	}
}