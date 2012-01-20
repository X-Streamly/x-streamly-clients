package ly.xstream 
{
	import flash.events.TimerEvent;
	import flash.utils.Timer;
	import com.adobe.serialization.json.JSON;
	/**
	 * ...
	 * @author Brian Willard
	 */
	internal class PresenceChannel 
	{
		private static var memberId:String;
		
		public var members:Members = new Members();
		
		public var memberId:String;
		public var memberData:Object;
		
		private var uri:String = 'PicTacToe/XStreamly/Presence';
		private var appKey:String;
		private var channelName:String;
		private var loaded:Boolean = false;
		private var key:String;
		private var cerrio:Cerrio;
		private var timer :Timer;
		
		private var mainChannel:Channel;
		
		private var stream:Stream;
		
		private var startActionFired:Boolean = false;
		
		private var isPrivate:Boolean;
		
		public function PresenceChannel(name:String,appKey:String,cerrio:Cerrio,memberId:String,memberData:Object,mainChannel:Channel,isPrivate:Boolean) 
		{
			this.channelName = name;
			this.appKey = appKey;
			this.cerrio = cerrio;
			this.mainChannel = mainChannel;
			this.isPrivate = isPrivate;
			
			if (!memberId) {
				if (!PresenceChannel.memberId) {
					PresenceChannel.memberId=Math.round(Math.random() * 1000000).toString();
				}

				this.memberId = PresenceChannel.memberId.toString();
			} else {
				this.memberId = memberId;
			}
			
			if(!memberData) {
			  memberData = { memberId: memberId }
			}
			else if(!memberData.memberId){
				memberData.memberId = memberId;
			}
			
			this.memberData = memberData;
			
			this.stream = this.cerrio.subscribe({url:this.uri,
				subscription:"@.AppKey='" + appKey + "' and @.Channel ='" + this.channelName + "' and @.Connected and @.Private ="+isPrivate,
				addAction:this.addAction,
				modifyAction:this.modifyAction,
				deleteAction:this.deleteAction});
		}
		
		public function memberInfo(name:Object, value:Object):Object {
			if(null === value && typeof name === 'string') {
				return this.memberData[name];
			} else {
				if(typeof name === 'string') {
					this.memberData[name] = value;
				} else if (typeof name === 'object') {
					for(var p:String in name){
						this.memberData[key] = value;
					}
				}
				this.cerrio.sendModify(this.uri, {
					Key: this.key,
					MemberInfo: JSON.encode(this.memberData)
				});
				return null;
			}
		}
		
		public function addAction (item:Object):void {
			if(null === item.MemberId && item.MemberInfo) {
				try {
				  item.MemberId = JSON.decode(item.MemberInfo).memberId;
				} catch(ex:Error) { }
			}
			
			var member:Member = this.members.get(item.MemberId);
			if(null === member) {
				member = new Member(item.MemberId,item.MemberInfo);
				this.members.add(member);
			}
			member.addRecord(item);
			
			if(this.loaded){
				this.mainChannel.fireEvent("xstreamly:member_added",member,null);
			}
			
			
			if(item.Key===this.key){
				XStreamlyClient.log('joined presence channel with:'+this.members.count);
				this.loaded=true;
				this.fireTimer(null);
				this.timer = new Timer(10 * 1000, 0);
				this.timer.addEventListener(TimerEvent.TIMER, this.fireTimer);
				this.timer.start();
				this.mainChannel.fireEvent("xstreamly:subscription_succeeded",this.members,null);
			}
		}
		
		public function deleteAction(item:Object):void {
			var member:Member;
			this.members.each(function(m:Member):void{
				if(m.containsKey(item.Key)){
					member=m;
				}
			});
			if(null!=member){
				member.removeRecord(item.Key);
				
				if(!member.alive()){
					this.members.remove(member.id);
					this.mainChannel.fireEvent("xstreamly:member_removed",member,null);
				}
			}
		}
		
		public function modifyAction(item:Object):void {
			var member:Member;
			this.members.each(function(m:Member):void{
				if(m.containsKey(item.Key)){
					member=m;
				}
			});
			if(null!=member){
			  if(null!==item.MemberInfo) {
				try {
				  member.memberInfo = JSON.decode(item.MemberInfo);
				  this.mainChannel.fireEvent("xstreamly:member_modified",member,null);
				} catch(ex:Error) { }
			  }
			}
		}
		
		public function fireTimer(event:TimerEvent):void {
			this.cerrio.sendModify(uri, {
				Key: this.key,
				ChallengeTime: new Date().toString(),
				Connected: true
			});
		}
		
		private function action():void {
			if(!this.startActionFired){
				this.key = this.appKey+'|'+this.channelName+'|'+this.cerrio.connection.socket.sessionID+'|'+this.isPrivate;
				cerrio.sendAdd(this.uri,{
					Key:this.key,
					Channel: this.channelName,
					AppKey:this.appKey,
					MemberId:this.memberId,
					MemberInfo: JSON.encode(this.memberData),
					Verified: false,
					Connected: true,
					Challenge:0,
					ChallengeTime: (new Date()).toString(),
					Response:0,
					SocketId:this.cerrio.connection.socket.sessionID,
					Private:this.isPrivate
				});
				//in cas the record already exists but is set to not connected
				//send in a mod            
				this.cerrio.sendModify(this.uri, {
					Key: this.key,
					ChallengeTime: new Date(),
					Connected: true
				});
				this.startActionFired=true;
			}
		}
		
		public function start():void {
			XStreamlyClient.log('starting prsence channel: '+this.channelName);
			this.cerrio.connection.onActive(action,false,this)
		}
		
		public function close():void {
			if(this.timer){
				this.timer.stop();
				this.timer = undefined;
			}
			
			this.cerrio.sendDelete(this.uri,this.key);
			
			if(this.stream){
				this.stream.close();
			}
		}
	}

}