package ly.xstream 
{
	/**
	 * ...
	 * @author Brian Willard
	 */
	internal class Connection 
	{
		import com.pnwrain.flashsocket.FlashSocket;
		import com.pnwrain.flashsocket.events.FlashSocketEvent;
		import flash.events.Event;
		import com.adobe.serialization.json.JSON;
		
		public var id:String;
		public var state:String = "initialized";

		private var securityTokens:Array = [];
		private var bindings:Array = [];
		private var port:int;
		public var socket:FlashSocket;
		
		
		public function Connection(port:int) 
		{
			this.port = port || 443;
			var me:Connection = this;
			
			try {
				//this.socket = new FlashSocket("https://api.cerrio.com:"+port);
				this.socket = new FlashSocket("localhost:3001");
				this.setStatus("connecting");

			}
			catch (ex:Error) {
				XStreamlyClient.log("problem connecting to socket " + ex);
				this.setStatus("failed");
				return;
			}
			
			
			this.socket.addEventListener(FlashSocketEvent.CONNECT, function():void {
				me.id = me.socket.sessionID;
				me.setStatus('awaiting-security-clearance');
				for each(var token:String in me.securityTokens){
				  if(token){
					  sendSecurityToken(token);
					}
				}
			});
			
			this.socket.addEventListener(FlashSocketEvent.DISCONNECT, function():void {
				me.setStatus("disconnected");
			});
			
			this.socket.addEventListener(FlashSocketEvent.CLOSE, function():void {
				me.setStatus("disconnected");
			});
			
			this.socket.addEventListener(FlashSocketEvent.IO_ERROR, onError);
			this.socket.addEventListener(FlashSocketEvent.SECURITY_ERROR, onError);
			this.socket.addEventListener(FlashSocketEvent.CONNECT_ERROR , onError);
			
			this.socket.addEventListener('cerrio:session-started', function(data:FlashSocketEvent):void {
				var item:Object = data.data[0];
				XStreamlyClient.log('got security call back: '+item.error+' valid: '+item.valid);
				if(item.valid && this.state!=='connected'){
					me.setStatus('connected');
				}
				else if (!item.valid && this.state ==='awaiting-security-clearance'){
					me.setStatus('security-check-failed');
				}
			});
		}
		
		private function onError(error:FlashSocketEvent):void {
			XStreamlyClient.log("socket error: "+JSON.encode(error));
		}
		
		public function bind(eventName:String, callback:Function):void {
			bindings.push({ name: eventName, func: callback });
		}
		
		public function onActive(action:Function, repeat:Boolean,caller:Object= null):void {
			var fired:Boolean = false;
			if (this.state === "connected") {
				if (action.length === 0) {
					action.call(caller);
				} else {
					action.call(caller,this);
				}
				fired = true;
			}

			if (!fired || repeat) {
				this.bind("connected", function():void {
					if (!fired || repeat) {
						fired = true;
						if (action.length === 0) {
							action.call(caller);
						} else {
							action.call(caller,this);
						}
					}
				});
			}
		}
		
		public function stop():void {
			if(this.socket){
				//TODO how to disconect
				//this.socket.disconnect();
			}
		}
		
		public function applySession(token:String):void {
			this.securityTokens.push(token);
    	
			//if we are already connected send the token over
			if(this.state==='awaiting-security-clearance'
			||this.state==='security-check-failed'
			||this.state==='connected'){
				this.sendSecurityToken(token);
			}
		}
		
		private function sendSecurityToken(token:String):void{
			XStreamlyClient.log('sending security token: ' + token);
			this.socket.send(token,'cerrio:session');
		}
		
		private function setStatus(status:String):void {
			this.state = status;
			this.fireEvent(status,null);
			
		};
		
		public function fireEvent(eventName:String, data:Object):void {
			XStreamlyClient.log("firing event: " + eventName);
			for (var key:Object in this.bindings) {
			  if(this.bindings.hasOwnProperty(key)){
				var event:String = bindings[key].name;
				if (event == eventName) {
					bindings[key].func(data);
				}
				else if (event === null) {
					bindings[key].func(eventName, data);
				}
			  }
			}
		}
		
	}

}