package ly.xstream 
{
	/**
	 * ...
	 * @author Brian Willard
	 */
	public class Cerrio 
	{
		internal var connection:Connection;
		
		public function Cerrio(port:int) 
		{
			this.connection = new Connection(port);
		}
		
		public function subscribe(options:Object):Stream {
			var stream:Stream = new Stream(this.connection, options);
			this.connection.onActive(function():void { 
				stream.start(); 
			}, true);
			return stream;
		}
		
		public function stop():void {
			this.connection.stop();
		}
		
		public function sendAdd(uri:String, data:Object):void {
			if (!uri) {
				throw new Error("uri can't be null");
			}
			this.connection.onActive(function():void {
				this.connection.socket.emit('update', {
					id: this.connection.id + ":add",
					uri: uri,
					update: {
						action: 'add',
						item: data
					}
				});
			}, false,this);
		}
		
		public function sendModify(uri:String, data:Object):void {
			if(!uri){
				throw new Error("uri can't be null");
			}
			this.connection.onActive(function():void{
				this.connection.socket.emit('update', {
					id: this.connection.id + ":modify",
					uri: uri,
					update: {
						action: 'modify',
						item: data
					}
				});
			},false,this);
		}
		
		public function sendDelete(uri:String, key:String):void {
			if(!uri){
				throw new Error("uri can't be null");
			}
			this.connection.onActive(function():void{
				this.connection.socket.emit('update', {
					id: this.connection.id + ":delete",
					uri: uri,
					update: {
						action: 'delete',
						itemKey: key
					}
				});
			},false,this);
		}
		
		public function applySession(token:String):void {
			this.connection.applySession(token);
		}
	}
}