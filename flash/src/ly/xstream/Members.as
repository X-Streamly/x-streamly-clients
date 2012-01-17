package ly.xstream 
{
	/**
	 * ...
	 * @author Brian Willard
	 */
	public class Members 
	{
		public var count:int=0;
		public var members:Object = {};
		
		public function Members() 
		{
			
		}
		
		
		public function each(func:Function):void{
			for (var key:Object in this.members) {
				func(this.members[key]);
			}
		}
		
		public function get(memberId:String):Member{
			return this.members[memberId];
		}
		
		public function add(member:Object):void{
			this.members[member.id] = member;
			this.count++;
		}
		
		public function remove(memberId:String):void{
			delete this.members[memberId];
			this.count--;
		}

		public function socketId(socketId:String):Member{
			for (var key:Object in this.members) {
			  var member:Member = this.members[key];
				if (member.containsSocket(socketId)) {
					return member;
				}
			}

			return null;
		}
	}
}