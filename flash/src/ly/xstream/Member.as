package ly.xstream 
{
	import com.adobe.serialization.json.JSON;
	/**
	 * ...
	 * @author Brian Willard
	 */
	public class Member 
	{
		public var id:String;
		public var memberInfo:Object;
		
		private var records:Object = { };
		private var recordCount:int = 0;
		
		
		public function Member(memberId:String, memberInfo:Object) 
		{
			this.id = memberId;
			if(typeof memberInfo === 'string') {
				this.memberInfo = JSON.decode(memberInfo as String);
			} else if (typeof memberInfo === 'object') {
				this.memberInfo = memberInfo;
			}
		}
   
    
		public function addRecord(record:Object):void{
			this.records[record.Key]=record;
			this.recordCount++;
		}
		
		public function removeRecord(key:String):void{
			delete this.records[key];
			this.recordCount--;
		}
		
		public function containsSocket(socketId:String):Boolean{
			for (var key:Object in this.records) {
				if (this.records[key].SocketId === socketId) {
					return true;
				}
			}
			
			return false;
		}
		
		public function containsKey(key:String):Boolean{
			return undefined!==this.records[key];
		}
		
		public function alive():Boolean{
		  return this.recordCount>0;
		}
			
	}

}