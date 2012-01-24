package ly.xstream.streaming;

import java.util.*;

public class Channel {
	private Cerrio cerrio;
	private String name;
	private String appKey;
	private String uri =StreamingClient.MessageUrl;
	private Boolean isPrivate = false;
	private Boolean closed =false;
	private PresenceChannel presenceChannel;
	private Boolean isSubscriptionLoaded = false;
	private List<SimpleAction> triggersWaitingOnSubscriptionLoad = new Vector<SimpleAction>();
	private String nonPersistedId;
	
	private HashMap<String,List<IXstreamlyMessageHandler>> bindings = new HashMap<String,List<IXstreamlyMessageHandler>> ();
		
	private Stream stream;
	
	private ChannelOptions options;
	
	private ILogger logger;
		
	public Channel(String name, String appKey, Cerrio cerrio,ChannelOptions options,ILogger logger){
		this.name = name;
		this.appKey = appKey;
		this.cerrio = cerrio;
		this.logger =logger;
		
		if(null==options)
		{
			options = new ChannelOptions();
		}
		
		this.options =options;
		
		
		Boolean createPresenceChannel =  options.createPresence;
		Boolean isPrivate = options.isPrivate;
		
		if(createPresenceChannel){
			presenceChannel = new PresenceChannel(name, appKey, cerrio, options.userId, options.userInfo, isPrivate,logger);
			presenceChannel.start();
		}
		
		startAction();
	}
	
	public void close(){
		if(null!=presenceChannel){
			presenceChannel.close();
			presenceChannel = null;
		}
		
		if(null!=stream){
			stream.close();
			stream = null;
		}	
		closed = true;
	}
	
	public void trigger(String eventName, Object data, Boolean persisted){
		if(isSubscriptionLoaded){
			cerrio.connection.onActive(new TriggerAction(eventName,data,persisted), false);
		} else {
			triggersWaitingOnSubscriptionLoad.add(new TriggerWaitingOnSubscriptionLoad(eventName,data,persisted));
		}
	}
	
	public void removePersistedMessage(String key){
		if(isSubscriptionLoaded){
			cerrio.connection.onActive(new RemovePersistedMessageAction(key), false);
		}else{
			triggersWaitingOnSubscriptionLoad.add(new RemovePersistedMessageWaitingOnSubscriptionLoad(key));
		}
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
		if(null!=presenceChannel)
		{
			presenceChannel.bindToChannelEvents(handler);
		}
	}
	
	private void startAction(){
		final SubscriptionOptions options = new SubscriptionOptions();
		options.uri = uri;
		options.subscription = "@.AppKey = '"+appKey+"' and @.Channel = '"+name+"' and @.SocketId != 'placeholder' and @.Private="+isPrivate;
		options.includePersistedMessags = this.options.includePersistedMessags;
		options.addAction =  new IDataUpdateHandler() {
			@Override
			public void handleUpdate(String data) {
				XstreamlyMessage message = StreamingClient.XStreaemlyGson.fromJson(data, XstreamlyMessage.class);
				if(!message.Persisted){
					nonPersistedId = message.Key;
				}
				if((options.includePersistedMessags && message.Persisted)||isSubscriptionLoaded){
					itemRecieved(message);
				}
				
			}
		};
		options.modifyAction =new IDataUpdateHandler() {
			@Override
			public void handleUpdate(String data) {
				XstreamlyMessage message= StreamingClient.XStreaemlyGson.fromJson(data, XstreamlyMessage.class);
				itemRecieved(message);
			}
		};
				
		options.subscriptionLoaded = new SimpleAction() {
			@Override
			public void fireAction() {
				subscriptionLoaded();
				
			}
		};
				
		options.streamResetAction = new SimpleAction() {
			@Override
			public void fireAction() {
				streamResetAction();
			}
		};
		
		stream = cerrio.subscribe(options);
	}
	
	private void itemRecieved(XstreamlyMessage message){
		if(closed)
		{
			logger.log("recieved a message when closed");
			return;
		}
		
		if (message.SocketId != cerrio.connection.id || options.includeMyMessages) {
			
			Member member = null;
			
			if(null!=presenceChannel)
			{
				member = presenceChannel.members.socketId(message.SocketId);
			}
			
			fireEvent(message.EventName,message.Message,member,message.Key);
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
	
	private void subscriptionLoaded(){
		isSubscriptionLoaded= true;
		for(SimpleAction action: triggersWaitingOnSubscriptionLoad){
			action.fireAction();
		}
		
		if(null!=options.subscriptionLoaded)
		{
			options.subscriptionLoaded.fireAction();
		}
	}
	
	private void streamResetAction(){
		isSubscriptionLoaded = false;
		nonPersistedId= null;
	}
		
	private class TriggerWaitingOnSubscriptionLoad implements SimpleAction{
		private String eventName;
		private Object data;
		private Boolean persisted;
		
		public TriggerWaitingOnSubscriptionLoad(String eventName, Object data, Boolean persisted){
			this.eventName =eventName;
			this.data = data;
			this.persisted = persisted;
		}

		@Override
		public void fireAction() {
			trigger(eventName,data,persisted);
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
			
			XstreamlyMessage message = new XstreamlyMessage();
			message.Channel = name;
			message.AppKey = appKey;
			message.EventName = eventName;
			message.TimeStamp = new Date();
			message.Message= StreamingClient.XStreaemlyGson.toJson(data);
			message.SocketId = connection.id;
			message.Persisted = persisted;
			message.Private = isPrivate;
			message.Key="";
			
			if(null==nonPersistedId || persisted){
				cerrio.sendAdd(uri, message);
			} else {
				message.Key=nonPersistedId;
				cerrio.sendModify(uri, message);
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
			cerrio.sendDelete(uri, key);
		}
	}
}
