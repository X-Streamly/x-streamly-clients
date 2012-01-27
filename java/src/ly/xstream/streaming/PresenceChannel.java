package ly.xstream.streaming;

import java.util.*;

import com.google.gson.reflect.TypeToken;

class PresenceChannel {
	private static String theMemberId;
	private static final String uri = "PicTacToe/XStreamly/Presence";
	
	private String appKey;
	private String channelName;
	private Boolean loaded = false;
	private String key;
	private Cerrio cerrio;
	private Boolean isPrivate;
	
	private Boolean startActionFired = false;
	
	public String memberId;
	public HashMap<String,String> memberInfo;
	
	private Stream stream;
	
	private Timer timer;
	
	private XstreamlyPresenceMessage message;
	
	Members members = new Members();
	
	private ILogger logger;
	
	private List<IXstreamlyChannelEventsHandler> channelEventsHandlers = new Vector<IXstreamlyChannelEventsHandler>();
	
	public PresenceChannel(final String name,final String appKey, final Cerrio cerrio, String memberId, HashMap<String,String> memberInfo, Boolean isPrivate,ILogger logger)
	{
		channelName = name;
		this.appKey = appKey;
		this.cerrio = cerrio;
		this.isPrivate=isPrivate;
		this.logger = logger;
		if(null==memberInfo){
			memberInfo = new HashMap<String, String>();
		}
		
		if(null==memberId){
			if(null==PresenceChannel.theMemberId){
				Random rand = new Random();
				PresenceChannel.theMemberId = Integer.toString(rand.nextInt(1000000));
			}
			this.memberId=PresenceChannel.theMemberId;
		} else {
			this.memberId = memberId;
		}
			
		memberInfo.put("memberId", this.memberId);
		
		this.memberInfo = memberInfo;
		
		SubscriptionOptions options = new SubscriptionOptions();
		options.subscription = "@.AppKey='" + appKey + "' and @.Channel ='" + this.channelName + "' and @.Connected and @.Private ="+isPrivate;
		options.uri=uri;
		options.addAction = new IDataUpdateHandler() {
			@Override
			public void handleUpdate(String data) {
				XstreamlyPresenceMessage message = StreamingClient.XStreaemlyGson.fromJson(data, XstreamlyPresenceMessage.class);
				addAction(message);

			}
		};
		options.modifyAction = new IDataUpdateHandler() {
			@Override
			public void handleUpdate(String data) {
				XstreamlyPresenceMessage message = StreamingClient.XStreaemlyGson.fromJson(data, XstreamlyPresenceMessage.class);
				modifyAction(message);
			}
		};
		options.deleteAction = new IDataUpdateHandler() {
			@Override
			public void handleUpdate(String data) {
				XstreamlyPresenceMessage message = StreamingClient.XStreaemlyGson.fromJson(data, XstreamlyPresenceMessage.class);
				deleteAction(message);
			}
		};
		
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if(null!=message)
				{
					message.Connected = true;
					message.ChallengeTime = new Date();
					cerrio.sendModify(uri, message,"Connected","ChallengeTime","Key");
				}
				
			}
		},10*1000, 10*1000);
		
		stream = cerrio.subscribe(options);
	}
	
	public void start(){
		cerrio.connection.onActive(new IConnectionActiveCallback() {
			@Override
			public void onConnectionActive(Connection connection) {
				startAction();
			}
		}, false);
	}
	
	public void close()
	{
		if(null!=timer){
			timer.cancel();
			timer = null;
		}
		
		cerrio.sendDelete(uri, key);
		
		if(null!=stream){
			stream.close();
			stream = null;
		}
	}
	
	private void startAction(){
		if(startActionFired){
			return;
		}
		
		
		key = appKey+"|"+channelName+"|"+cerrio.connection.id+"|"+isPrivate;
		
		message = new XstreamlyPresenceMessage();
		
		message.memberId = this.memberId;
		message.memberInfo = this.memberInfo;
		
		message.Key=key;
		message.Channel=channelName;
		message.AppKey=appKey;
		message.MemberInfo=StreamingClient.XStreaemlyGson.toJson(memberInfo);
		message.Verified=false;
		message.Connected=true;
		message.Challenge=0;
		message.ChallengeTime=new Date();
		message.Response =0;
		message.SocketId = cerrio.connection.id;
		message.Private = false;
		
		cerrio.sendAdd(uri, message);
		cerrio.sendModify(uri, message);
		
		startActionFired = true;
	}
	
	public void bindToChannelEvents(IXstreamlyChannelEventsHandler handler){
			channelEventsHandlers.add(handler);
	}
	
	private void addAction(XstreamlyPresenceMessage message){
		if(null!= message.MemberInfo){
			message.memberInfo = StreamingClient.XStreaemlyGson.fromJson(message.MemberInfo,  new TypeToken<HashMap<String,String>>() {}.getType());
			message.memberId = message.memberInfo.get("memberId");
		}
		
		Member member = members.get(message.memberId);
		if(null==member){
			member = new Member(message.memberId,message.memberInfo);
			members.add(member);
		}
		
		member.addRecord(message);
		
		if(loaded){
			for(IXstreamlyChannelEventsHandler handler: channelEventsHandlers){
				handler.memberAdded(member);
			}
		}
		
		if(message.Key.equals(key)){
			logger.log("joined presence channel with: "+this.members.getCount());
			loaded = true;
			for(IXstreamlyChannelEventsHandler handler: channelEventsHandlers){
				handler.loaded(members);
			}
		}
	}
	
	private void modifyAction(XstreamlyPresenceMessage message){
		for(Member m: members.members.values()){
			if(m.containsKey(message.Key)){
				if(null!=message.MemberInfo){
					m.memberInfo = StreamingClient.XStreaemlyGson.fromJson(message.MemberInfo,  new TypeToken<HashMap<String,String>>() {}.getType());
					for(IXstreamlyChannelEventsHandler handler: channelEventsHandlers){
						handler.membmerModified(m);
					}
					return;
				}
			}
		}
	}

	private void deleteAction(XstreamlyPresenceMessage message){
		for(Member m: members.members.values()){
			if(m.containsKey(message.Key)){
				m.removeRecord(message.Key);
				if(!m.isAlive()){
					members.remove(m.id);
					for(IXstreamlyChannelEventsHandler handler: channelEventsHandlers){
						handler.memberRemoved(m);
					}
					return;
				}
			}
		}
	}
}
