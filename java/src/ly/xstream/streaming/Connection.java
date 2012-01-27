package ly.xstream.streaming;

import java.io.IOException;
import java.util.*;

import org.json.JSONException;
import org.json.JSONObject;

import com.clwillingham.socket.io.*;
import com.google.gson.Gson;


class Connection implements MessageCallback, ICerrioMessageHandler{
	
	private ConnectionStates state;
	private HashMap<String,List<ICerrioMessageHandler>> bindings = new HashMap<String,List<ICerrioMessageHandler>>();
	private IOSocket socket;
	public String id;
	private List<String> securityTokens = new Vector<String>();
	private ILogger logger;
	
	public Connection(int port,ILogger logger){
		this.logger=logger;
		try{
			//NOTE this is not a secure connection
			//I couldn't get SSL and the Java NIO
			//to play together nicely
			socket = new IOSocket("http://api.cerrio.com:"+port,this);
			setStatus(ConnectionStates.connecting);
			socket.connect();
		} catch(Exception ex){
			logger.handleError("problem connectiong to socket: ",ex);
			setStatus(ConnectionStates.failed);
			return;
		}
		
		bind("error",this);
	}
	
	public void bind(String eventName,ICerrioMessageHandler handler){
		if(!bindings.containsKey(eventName)){
			bindings.put(eventName, new Vector<ICerrioMessageHandler>());
		}
		
		bindings.get(eventName).add(handler);
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
			for(ICerrioMessageHandler handler: bindings.get(eventName)){
				handler.handleMessae(eventName,  data);
			}
		}
		
		if(bindings.containsKey(null)){
			for(ICerrioMessageHandler handler: bindings.get(null)){
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
		
		if(data instanceof CerrioUpdateWrapper){
			CerrioUpdateWrapper wrapper = (CerrioUpdateWrapper)data;
			gson = wrapper.gson;
		}
			
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
			socket.emit("cerrio:session",token);
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
		if("cerrio:session-started".equals(event)){
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

	private class OnActiveClosure implements ICerrioMessageHandler{
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
