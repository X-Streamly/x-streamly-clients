package ly.xstream 
{
	import com.pnwrain.flashsocket.events.FlashSocketEvent;
	
	/**
	 * ...
	 * @author Brian Willard
	 */
	public class Stream 
	{
	
		private var running:Boolean = false;
		private var connection:Connection;
		private var options:Object;
		private var sourceId:String;
		
		public function Stream(connection:Connection,options:Object) 
		{
			if(!options){
				throw new Error("options must be set.");
			}
			
			if(!connection){
				throw new Error("connection must be set.");
			}
			
			if(!options.url){
				throw new Error("options.url can't be empty");
			}
			
			this.options = options;
			this.connection = connection;
		}
		
		public function start():void {
			XStreamlyClient.log("starting " + this.options.url + "?" + this.options.subscription);
			var randomNumber:String = Math.random().toString();
			this.sourceId = this.connection.socket.sessionID + "|" + this.options.url + "|" + this.options.subscription + "|" + randomNumber;
			this.running = true;
			
			var me:Stream = this;
			
			this.connection.socket.addEventListener('closed-'+this.sourceId,function():void{
				XStreamlyClient.log('connection to the hog was reset, need to start streaming again');
				if(me.options.streamResetAction){
					me.options.streamResetAction();
				}
				me.startStreaming();
			});
			
			var handleUpdate:Function = function(update:Object):void {
				if (me.running && null != me.options) {
					if (update.action === "add" && null != me.options.addAction) {
						me.options.addAction(update.item);
					}
					if (update.action === "modify" && null != me.options.modifyAction) {
						me.options.modifyAction(update.item);
					}
					if (update.action === "delete" && null != me.options.deleteAction) {
						me.options.deleteAction(update.item);
					}
				}
			}

			this.connection.socket.addEventListener(this.sourceId, function(flashEvent:FlashSocketEvent):void {
				var update:Object = flashEvent.data;
				if (update is Array) {
					for each(var u:Object in (update as Array)) {
						handleUpdate(u);
					}
				}
				else {
					handleUpdate(update);
				}
			});
			this.startStreaming();
		}
		
		public function close():void {
			this.running = false;
			//TODO: implement
		}
		
		private function startStreaming():void{
		    if (this.options.subscription) {
		        var updatesOnly:Boolean = false;

		        if (null != this.options.updatesOnly) {
		            updatesOnly = this.options.updatesOnly;
		        }
				
				var me:Stream = this;
							
		        XStreamlyClient.log('starting streaming with id:'+this.sourceId);
		        this.connection.socket.emit('stream',{
		            id: this.sourceId,
		            uri: this.options.url,
		            updatesOnly: updatesOnly,
		            subscription: this.options.subscription
				},
			    function():void {
			    	XStreamlyClient.log('subscription loaded');
			        if (me.options.subscriptionLoaded) {
			            me.options.subscriptionLoaded();
			        }
			    });
		    }
		}
	}

}