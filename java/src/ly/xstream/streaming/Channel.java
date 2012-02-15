package ly.xstream.streaming;

import java.util.*;

public class Channel {
	private Connection connection;
	private String name;
	private String appKey;
	private Boolean closed =false;

	private HashMap<String,List<IXstreamlyMessageHandler>> bindings = new HashMap<String,List<IXstreamlyMessageHandler>> ();
		
	private IClosable stream;
	
	private ChannelOptions options;
	
	private ILogger logger;
	
	private boolean membersLoaded = false;
	private final Member member;
	private final Members members = new Members();
	
	private static String _savedMessageId;
	
	private final Vector<IXstreamlyChannelEventsHandler> chanelEventsHandlers = new Vector<IXstreamlyChannelEventsHandler>();
		
	public Channel(String name, String appKey, Connection connection,ChannelOptions options,ILogger logger){
		this.name = name;
		this.appKey = appKey;
		this.connection = connection;
		this.logger =logger;
		
		if(null==options)
		{
			options = new ChannelOptions();
		}
		
		this.options =options;
		
		if(null==options.userId || options.userId.isEmpty()){
			if(null==_savedMessageId){
				_savedMessageId = Double.toString(Math.random());
			}
			options.userId = _savedMessageId;
		}
				
		
		this.member = new Member(options.userId,options.userInfo);
		
		startAction();
	}
	
	public void close(){		
		if(null!=stream){
			stream.close();
			stream = null;
		}	
		closed = true;
	}
	
	public void trigger(String eventName, Object data, Boolean persisted){
		connection.onActive(new TriggerAction(eventName,data,persisted), false);
	}
	
	public void removePersistedMessage(String key){
		connection.onActive(new RemovePersistedMessageAction(key), false);
	}
	
	public void bind(String eventName,IXstreamlyMessageHandler handler){
		if(!bindings.containsKey(eventName)){
			bindings.put(eventName, new Vector<IXstreamlyMessageHandler>());
		}
		
		bindings.get(eventName).add(handler);
	}
	
	public void bindAll(IXstreamlyMessageHandler handler){
		if(!bindings.containsKey(null)){
			bindings.put(null, new Vector<IXstreamlyMessageHandler>());
		}
		
		bindings.get(null).add(handler);
	}
	
	public void bindToChannelEvents(IXstreamlyChannelEventsHandler handler){
		chanelEventsHandlers.add(handler);
		
		if(membersLoaded){
			handler.loaded(members);
		}
	}
	
	private void setMembmersLoaded(){
		membersLoaded = true;
	}
	
	private boolean getMembmersLoaded(){
		return membersLoaded;
	}
	
	private void startAction(){
		final Member me = this.member;
		stream = connection.subscribe(name,member,
				new IDataUpdateHandler() {
					@Override
					public void handleUpdate(DataMessages messages) {
						for(DataMessage message: messages.messages){
							itemRecieved(message);
						}
					}
				}, new IMemberMessageHandler() {
					@Override
					public void handleMessage(MembersMessage memberMessages) {
						for(MemberMessage message: memberMessages.members){
							HashMap<String,String> info 
								= (HashMap<String,String>) StreamingClient.XStreaemlyGson.fromJson(message.item.Info, HashMap.class);
							if(message.action.equals("add")){
								Member member = members.get(message.item.MemberId);
								
								if(null!=member){
									member.addRecord(message);
								} else {
									member = new Member(message.item.MemberId, info);
									members.add(member);
									
									if(message.item.MemberId.equals(me.id)){
										setMembmersLoaded();
										for(IXstreamlyChannelEventsHandler handler:  chanelEventsHandlers){
											handler.loaded(members);
										}
									} 
									else if (getMembmersLoaded()) {
										for(IXstreamlyChannelEventsHandler handler:  chanelEventsHandlers){
											handler.memberAdded(member);
										}
									}
								}
							}
							else if (message.action.equals("delete")){
								Member member = members.get(message.item.MemberId);
								member.removeRecord(message.item.Key);
								if(!member.isAlive()){
									Member removedMember = members.remove(message.item.MemberId);
									for(IXstreamlyChannelEventsHandler handler:  chanelEventsHandlers){
										handler.memberRemoved(removedMember);
									}
								}
							} else if (message.action.equals("modify")){
								Member member = members.get(message.item.MemberId);
								member.memberInfo = info;
								for(IXstreamlyChannelEventsHandler handler:  chanelEventsHandlers){
									handler.membmerModified(member);
								}
							} else {
								logger.handleError("action "+message.action +" is unsuported",new Exception());
							}
						}
						
					}
				});
	}
	
	private void itemRecieved(DataMessage message){
		if(closed)
		{
			logger.log("recieved a message when closed");
			return;
		}
		
		if (message.SocketId != connection.id || options.includeMyMessages) {
			
			//TODO: get member
			Member member = null;
			
			
			//TODO: persistance key
			fireEvent(message.EventName,message.Message,member,null);
		}
	}
	
	void fireEvent(String eventName,String messageData,Member member,String key){
		if(bindings.containsKey(eventName)){
			for(IXstreamlyMessageHandler handler: bindings.get(eventName))
			{
				handler.handleMessage(eventName, messageData, member, key);
			}
		}
		
		if(bindings.containsKey(null)){
			for(IXstreamlyMessageHandler handler: bindings.get(null))
			{
				handler.handleMessage(eventName, messageData, member, key);
			}
		}
	}
	
		
	private class RemovePersistedMessageWaitingOnSubscriptionLoad implements SimpleAction{
		private String key;
		
		public RemovePersistedMessageWaitingOnSubscriptionLoad(String key){
			this.key =key;
		}

		@Override
		public void fireAction() {
			removePersistedMessage(key);
		}
	}
	
	private class TriggerAction implements IConnectionActiveCallback{
		private String eventName;
		private Object data;
		private Boolean persisted;
		
		public TriggerAction(String eventName, Object data, Boolean persisted){
			this.eventName =eventName;
			this.data = data;
			this.persisted = persisted;
		}
		@Override
		public void onConnectionActive(Connection connection) {
			
			DataMessage  message = new DataMessage(appKey,name,eventName,connection.id,
					StreamingClient.XStreaemlyGson.toJson(data));
			DataMessages messages = new DataMessages(new DataMessage[]{message});
			try{
				connection.send("internal:send",messages);
			} catch(Exception ex){
				
			}
		}
	}

	private class RemovePersistedMessageAction implements IConnectionActiveCallback{
		private String key;
		
		public RemovePersistedMessageAction(String key){
			this.key =key;
		}
		@Override
		public void onConnectionActive(Connection connection) {	
			//TODO: impliment delete persisted messages
		}
	}
}
