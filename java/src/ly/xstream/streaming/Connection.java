package ly.xstream.streaming;

import java.io.IOException;
import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;

import com.clwillingham.socket.io.*;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;


class Connection implements MessageCallback, IRawMessageHandler{
	
	private ConnectionStates state;
	private HashMap<String,List<IRawMessageHandler>> bindings = new HashMap<String,List<IRawMessageHandler>>();
	private IOSocket socket;
	public String id;
	private List<String> securityTokens = new Vector<String>();
	private ILogger logger;
	private Integer subscriptionCount =0;
	private String appKey;
	
	public Connection(String appKey, String endPoint,ILogger logger){
		this.logger=logger;
		this.appKey = appKey;
		try{
			//NOTE this is not a secure connection
			//I couldn't get SSL and the Java NIO
			//to play together nicely
			logger.log("connecting to: "+endPoint);
			socket = new IOSocket(endPoint,this);
			setStatus(ConnectionStates.connecting);
			socket.connect();
		} catch(Exception ex){
			logger.handleError("problem connectiong to socket: ",ex);
			setStatus(ConnectionStates.failed);
			return;
		}
		
		bind("error",this);
	}
	
	public IClosable subscribe(final String channel, final Member member,final IDataUpdateHandler handler,final IMemberMessageHandler memberHandler){
		final String id = socket.getSessionID()+'|'+channel+'|'+subscriptionCount.toString();
		subscriptionCount++;
		final SubscriptionData subscriptionData =  new SubscriptionData(this.appKey,channel,id,member);
		
		
		onActive(new IConnectionActiveCallback() {
			@Override
			public void onConnectionActive(Connection connection) {
				try {
					connection.send("internal:subscribe",subscriptionData );
				} catch (Exception e) {
					logger.handleError("problem subscribing", e);
				}
			}
		}, true);
		
		final IRawMessageHandler rawHandler = new IRawMessageHandler() {
			@Override
			public void handleMessae(String eventName, JSONObject data) {
				DataMessages messages = StreamingClient.XStreaemlyGson.fromJson(data.toString(), DataMessages.class);
				handler.handleUpdate(messages);
			}
		};
		
		final IRawMessageHandler presenceHandler = new IRawMessageHandler() {
			@Override
			public void handleMessae(String eventName, JSONObject data) {
				try{
					MembersMessage members = StreamingClient.XStreaemlyGson.fromJson(data.toString(), MembersMessage.class);
					memberHandler.handleMessage(members);
				} catch(JsonSyntaxException e){
					logger.handleError("problem parsing: "+data.toString(), e);
				}
				
			}
		};
		
		bind("internal:data:"+id, rawHandler);
		bind("internal:member:"+id,presenceHandler);
		
		IClosable closable= new IClosable() {
			@Override
			public void close() {
				unbind("internal:data:"+id, rawHandler);
				unbind("internal:member:"+id,presenceHandler);
				onActive(new IConnectionActiveCallback() {
					
					@Override
					public void onConnectionActive(Connection connection) {
						try {
							connection.send("internal:unsubscribe", subscriptionData);
						} catch (Exception e) {
							logger.handleError("problem unsubscribing", e);
						}
					}
				}, true);
				
			}
		};
		
		return closable;
	}
	
	IClosable subscribeActiveChannels(final IActiveChannelCallback callback){
		final String id = socket.getSessionID()+"|activeChannels"+Double.toString(Math.random());
		final ListActiveChannelsData data = new ListActiveChannelsData(id,this.appKey);
		final IRawMessageHandler messageHandler = new IRawMessageHandler() {
			@Override
			public void handleMessae(String eventName, JSONObject data) {
				ChannelDataWrapper channelDataWrapper = StreamingClient.XStreaemlyGson.fromJson(data.toString(), ChannelDataWrapper.class);
				for(ChannelData channelData: channelDataWrapper.channels){
					callback.channelChanged(channelData);
				}
			}
		};
		
		bind("internal:channel:"+id, messageHandler);
		
		IClosable result = new IClosable() {
			@Override
			public void close() {
				unbind("internal:channel:"+id, messageHandler);
				onActive(new IConnectionActiveCallback() {
					@Override
					public void onConnectionActive(Connection connection) {
						try {
							connection.send("internal:channels:unsubscribe", data);
						} catch (Exception e) {
							logger.handleError("problem unsubscribing to active channels", e);
						}
					}
				}, true);
			}
		};
		
		onActive(new IConnectionActiveCallback() {
			@Override
			public void onConnectionActive(Connection connection) {
				try {
					connection.send("internal:channels:subscribe",data );
				} catch (Exception e) {
					logger.handleError("problem subscribing to channels", e);
				}
			}
		}, true);
		
		return result;
	}
	
	public void bind(String eventName,IRawMessageHandler handler){
		if(!bindings.containsKey(eventName)){
			bindings.put(eventName, new Vector<IRawMessageHandler>());
		}
		
		bindings.get(eventName).add(handler);
	}
	
	public void unbind(String eventName,IRawMessageHandler handler){
		if(bindings.containsKey(eventName)){
			bindings.get(eventName).remove(handler);
		}
	}
	
	public void onActive(final IConnectionActiveCallback action,Boolean repeat){
		Boolean fired = false;
		
		if(state == ConnectionStates.connected){
			action.onConnectionActive(this);
			fired = true;
		}
		
		if(!fired || repeat){
			OnActiveClosure handler = new OnActiveClosure(repeat,this,action);
			
			bind("connected",handler);
		}
	}
	
	public void stop(){
		if(null!=socket){
            socket.disconnect();
            socket= null;
        }
	}
	
	public void fireEvent(String eventName,JSONObject data){
		if(bindings.containsKey(eventName)){
			for(IRawMessageHandler handler: bindings.get(eventName)){
				handler.handleMessae(eventName,  data);
			}
		}
		
		if(bindings.containsKey(null)){
			for(IRawMessageHandler handler: bindings.get(null)){
				handler.handleMessae(eventName,  data);
			}
		}
	}
	
	public void applySession(String token){
		securityTokens.add(token);
		
		if(state == ConnectionStates.awaitingSecurityClearence
				||state == ConnectionStates.securityCheckFailed
				||state == ConnectionStates.connected){
			sendSecurityToken(token);
		}
	}
	
	public void send(String event,Object data) throws IOException, JSONException{
		send(event,data,null);
	}
	
	public void send(String event,Object data,AckCallback callback) throws IOException, JSONException{
		Gson gson = StreamingClient.XStreaemlyGson;
				
		String json = gson.toJson(data);
		JSONObject obj = new JSONObject(json);
		if(null==callback){
			socket.emit(event, obj);
		} else {
			socket.emit(event, obj,callback);
		}
	}
	
	private void sendSecurityToken(String token){
		logger.log("sending security token: "+token);
		
		try{
			socket.emit("internal:securityToken",token);
		} catch (Exception ex){
			logger.handleError("problem sending security token: ", ex);
			setStatus(ConnectionStates.failed);
		}
	}
	
	private void setStatus(ConnectionStates status){
		state = status;
		logger.log("connection status set to: "+status);
		fireEvent(status.toString(),null);
	}
	
	private void handelSecurtyCallback(JSONObject json)
	{
		Gson gson = new Gson();
		SecurityCallback data= gson.fromJson(json.toString(), SecurityCallback.class);
		
		logger.log("got security call back: "+data.error+ " valid: "+data.valid);
		if(data.valid && state!=ConnectionStates.connected){
			setStatus(ConnectionStates.connected);
		} else if(!data.valid && state ==ConnectionStates.awaitingSecurityClearence){
			setStatus(ConnectionStates.securityCheckFailed);
		}
	}
	
	
	@Override
	public void on(String event, JSONObject... data) {
		if("internal:securityTokenResult".equals(event)){
			handelSecurtyCallback(data[0]);
		} else {
			fireEvent(event,data[0]);
		}
	}

	@Override
	public void onMessage(String message) {
		logger.log("got unexpected onMessage "+message);
	}

	@Override
	public void onMessage(JSONObject json) {
		logger.log("got unexpected onMessage(json) "+json.toString());
	}

	@Override
	public void onConnect() {
		id = socket.getSessionID();
		setStatus(ConnectionStates.awaitingSecurityClearence);
		for(String token: securityTokens){
			sendSecurityToken(token);
		}
		
	}

	@Override
	public void onDisconnect() {
		setStatus(ConnectionStates.disconnected);
	}

	@Override
	public void onConnectFailure() {
		logger.log("connection failure");
		setStatus(ConnectionStates.failed);
	}
	
	private class SubscriptionData
	{
		public SubscriptionData(String appKey,String channel,String id,Member member)
		{
			this.appKey = appKey;
			this.channel = channel;
			this.id = id;
			this.member  = member;
		}
		
		@SuppressWarnings("unused")//used via JSON
		public String appKey;
		@SuppressWarnings("unused")//used via JSON
		public String channel;
		@SuppressWarnings("unused")//used via JSON
		public String id;
		@SuppressWarnings("unused")//used via JSON
		public Member member;
	}

	private class OnActiveClosure implements IRawMessageHandler{
		private Boolean beenFired = false;
		
		private Boolean repeat;
		private Connection connection;
		private IConnectionActiveCallback action;
		
		public OnActiveClosure(Boolean repeat,Connection connection,IConnectionActiveCallback action){
			this.repeat=repeat;
			this.connection = connection;
			this.action=action;
		}
		
		@Override
		public void handleMessae(String eventName,JSONObject data) {
			 if(!beenFired || repeat){
				 action.onConnectionActive(connection);
				 beenFired = true;
			 }
			
		}
	}
	
	private class ListActiveChannelsData
	{
		public ListActiveChannelsData(String id, String appKey)
		{
			this.id = id;
			this.appKey=appKey;
		}
		
		@SuppressWarnings("unused")//used via JSON
		public String id;
		
		@SuppressWarnings("unused")//used via JSON
		public String appKey;
	}
	
	private class SecurityCallback{
		public String error;
		public Boolean valid;
	}

	@Override
	public void handleMessae(String eventName, JSONObject data) {
		if(eventName.equals("error")){
			logger.log("Got error: "+ data.toString());
		}
	}
}
