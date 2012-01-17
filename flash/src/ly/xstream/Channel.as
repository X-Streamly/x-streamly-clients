package ly.xstream 
{
	import com.adobe.serialization.json.JSON;
	/**
	 * ...
	 * @author Brian Willard
	 */
	public class Channel 
	{
		private var cerrio:Cerrio;
		private var name:String;
		private var appKey:String;
		private var options:Object;
		private var uri:String = XStreamlyClient.MESSAGE_URL;
		private var isPrivate:Boolean = false;
		private var closed:Boolean = false;
		private var presenceChannel:PresenceChannel;
		private var subscriptionLoaded:Boolean = false;
		private var triggersWaitingOnSubscriptionLoad:Array = [];
		private var nonPersistedId:String;
		
		private var memberId:String;
		private var memberInfo:Function;
		
		private var stream:Stream;
		private var xstreamly:XStreamlyClient;
		
		private var bindings:Array = [];
		
		public function Channel(name:String, appKey:String, cerrio:Cerrio, mainXStreamly:XStreamlyClient, options:Object) {
			this.name = name;
			this.appKey = appKey;
			this.cerrio = cerrio;
			this.xstreamly = mainXStreamly;
			
			var createPresenceChannel:Boolean = true;
			
			this.options = options || { };
			
			if(options.presence!==undefined){
				createPresenceChannel = options.presence;
			}
			
			if(options.private){
				isPrivate = options.private;
			}
			
			if (createPresenceChannel) {
				this.presenceChannel = new PresenceChannel(name, appKey, cerrio, options.userId, options.userInfo, this,isPrivate);
				this.presenceChannel.start();
				this.memberId = this.presenceChannel.memberId;
				this.memberInfo = function(name:String, value:Object):void {
				  this.presenceChannel.memberInfo(name, value);
				};
			}
			
			startAction();
		}

		public function bind(eventName:String, callback:Function):void {
			if(this.closed){
				throw new Error("can't bind to a closed channel");
			}
			this.bindings.push({ name: eventName, func: callback });
		}
		
		public function bindAll(callback:Function):void {
			if(this.closed){
				throw new Error("can't bind to a closed channel");
			}
			this.bind(null, callback);
		}
		
		public function close():void {
			if(this.presenceChannel){
				this.presenceChannel.close();
				this.presenceChannel = undefined;
			}
			
			if(this.stream){
				this.stream.close();
				this.stream = undefined;
			}
			
			this.bindings = [];
			
			delete xstreamly.channels[name];
			this.closed = true;
		}
		
		public function trigger(eventName:String, data:Object, persisted:Boolean):void {
			var me:Channel = this;
			
			if(!this.subscriptionLoaded){
				this.triggersWaitingOnSubscriptionLoad.push(function():void{
					me.trigger(eventName,data,persisted);
				});
			} else{
				this.cerrio.connection.onActive(function():void{
					if(me.nonPersistedId && persisted===false){
						me.cerrio.sendModify(me.uri,{
							Key:me.nonPersistedId,
							Channel: me.name,
							AppKey:me.appKey,
							EventName: eventName,
							TimeStamp :(new Date()).toUTCString(),
							Message : JSON.encode(data),
							SocketId :me.cerrio.connection.id,
							Persisted: persisted,
							Private : me.isPrivate
						});
					}
					else{
						me.cerrio.sendAdd(me.uri,{
							Key:'',
							Channel: me.name,
							AppKey:me.appKey,
							EventName: eventName,
							TimeStamp :(new Date()).toUTCString(),
							Message : JSON.encode(data),
							SocketId :me.cerrio.connection.id,
							Persisted: persisted,
							Source:'',
							ClientInfo:'',
							Private : me.isPrivate
						});
					}
				 },false);
		   }
		}
		
		public function removePersistedMessage(key:String):void {
			var me:Channel = this;
			
			if(!this.subscriptionLoaded){
				this.triggersWaitingOnSubscriptionLoad.push(function():void{
					me.removePersistedMessage(key);
				});
			} else{
				me.cerrio.connection.onActive(function():void{
					me.cerrio.sendDelete(this.uri,key);
				 },false);
		   }
		}
		
		internal function fireEvent(eventName:String, data:Object, dataKey:String):void {
			if(this.closed){
				throw new Error("can't fire an event on a closed channel");
			}
			for (var key:String in this.bindings) {
				if(this.bindings.hasOwnProperty(key)){
					try{
						var binding:Object = this.bindings[key];
						if(binding && binding.func){
							var event:String = binding.name;
							var numberOfArgs:int = binding.func.length;
							if (event == eventName) {
								if (numberOfArgs === 1) {
									binding.func(data);
								} else {
									binding.func(data, dataKey);
								}
							} else if (event == null) {
								if (numberOfArgs === 1) {
									binding.func(eventName);
								} else if (numberOfArgs ===2) {
									binding.func(eventName,data);
								}else if (numberOfArgs ===3) {
									binding.func(eventName, data, dataKey);
								}
							}
						}
					} catch (ex:Error){
						XStreamlyClient.reportError(ex);
					}
				}
			}
		}
		
		private function itemRecieved(item:Object):void {
			try{
				var data:Object = JSON.decode(item.Message);

				if (this.presenceChannel) {
					data.member = this.presenceChannel.members.socketId(item.SocketId);
				}
			} catch(ex:Error){
				data = item.Message;
			}
			fireEvent(item.EventName, data, item.Persisted ? item.Key : null);
		}
		
		private function startAction():void {
			var me:Channel = this;
			this.stream = cerrio.subscribe({
				url: uri,
				subscription: "@.AppKey='" + me.appKey + "' and @.Channel ='" + me.name + "' and @.SocketId != 'placeholder' and @.Private="+me.isPrivate,
				addAction: function(item:Object):void {
					if (item.Persisted === false) {
						me.nonPersistedId = item.Key;
					}
					if ((me.options.includePersistedMessages && item.Persisted) || me.subscriptionLoaded) {
						if (item.SocketId != me.cerrio.connection.id || me.options.includeMyMessages) {
							me.itemRecieved(item);
						}
					}
				},
				modifyAction: function(item:Object):void {
					if (item.SocketId != me.cerrio.connection.id || me.options.includeMyMessages) {
						me.itemRecieved(item);
					}
				},
				//updatesOnly:updatesOnly,
				subscriptionLoaded: function():void {
					me.subscriptionLoaded = true;
					for (var t:Object in me.triggersWaitingOnSubscriptionLoad) {
						try{
							me.triggersWaitingOnSubscriptionLoad[t]();
						}
						catch(ex:Error){
							XStreamlyClient.log(ex.message+ ex.getStackTrace());
						}
					}
					if(me.options.subscriptionLoaded){
						me.options.subscriptionLoaded();
					}
				},
				streamResetAction:function():void{
					me.subscriptionLoaded = false;
					me.nonPersistedId = undefined;
				}
			});

		}
	
	}
}