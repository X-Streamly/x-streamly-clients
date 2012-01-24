package ly.xstream.streaming;

import java.util.Date;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;

import com.clwillingham.socket.io.AckCallback;
import com.google.gson.*;

import org.json.JSONObject;

public class Stream {
	private Connection connection;
	private SubscriptionOptions options;
	
	private Boolean running = false;
	private String sourceid;
	
	private ILogger logger;
	
	private Gson gson;
	
	public Stream(Connection connection, SubscriptionOptions options,ILogger logger){
		this.connection = connection;
		this.options = options;
		this.logger = logger;
		
		if(null==options){
			throw new IllegalArgumentException("options can not be null");
		}
		
		if(null==options.uri){
			throw new IllegalArgumentException("options.uri can not be null");
		}
		
		GsonBuilder builder = new GsonBuilder();
		
		builder.registerTypeAdapter(Date.class, new JavascriptDateDeserializer());
		builder.addDeserializationExclusionStrategy(new ExclusionStrategy() {
			
			@Override
			public boolean shouldSkipField(FieldAttributes fieldAttributes) {
				String name = fieldAttributes.getName();
				if(name.equals("item")||name.equals("items")){
					return true;
				}
				return false;
			}
			
			@Override
			public boolean shouldSkipClass(Class<?> arg0) {
				return false;
			}
		});
		
		gson = builder.create();
	}
	
	public void start(){
		logger.log("starting " + this.options.uri + "?" + this.options.subscription);
		Random rn = new Random();
		Double randomNumber = rn.nextDouble();
		
		sourceid=connection.id+"|"+options.uri+"|"+options.subscription+"|"+randomNumber.toString();
		running = true;
		
		connection.bind("closed-"+sourceid, new ICerrioMessageHandler(){
			@Override
			public void handleMessae(String eventName, JSONObject data) {
				logger.log("Connection to the hog was reset, need to start streaming again");
				
				if(null!=options.streamResetAction){
					options.streamResetAction.fireAction();
				}
				
				startStreaming();
			}
		});
		
		
		connection.bind(sourceid, new ICerrioMessageHandler(){
			@Override
			public void handleMessae(String eventName, JSONObject data) {
				//we need to just partial resolve the JSON here as we just want the
				//item JSON
				CerrioMessage cerrioMessage = gson.fromJson(data.toString(), CerrioMessage.class);

				try {
					JSONObject item = data.getJSONObject("item");
					if(null!=item){
						String s = item.toString();
						handleUpdate(cerrioMessage.action,s );
					}
				} catch (JSONException e) {
					logger.handleError("problem deserializing event: "+eventName, e);
				}
			}
		});
		
		startStreaming();
	}
	
	public void close(){
		this.running =false;
	}
	
	private void handleUpdate(String action, String update){
		if(running){
			if("add".equals(action) && null!=options.addAction){
				options.addAction.handleUpdate(update);
			} else if("modify".equals(action) && null!=options.modifyAction){
				options.modifyAction.handleUpdate(update);
			} else if("delete".equals(action) && null!=options.deleteAction){
				options.deleteAction.handleUpdate(update);
			} 
		}
	}
	
	private void startStreaming() {
		try {
			if(null!=options.subscription){
				logger.log("starting streaming with id: "+sourceid);
				
				connection.send("stream",
						new StreamOptions(sourceid,options.uri,options.updatesOnly,options.subscription),
						new AckCallback() {
							
							@Override
							public void callback(JSONArray data) throws JSONException {
								logger.log("subscription loaded to "+options.uri);
								if(null!=options.subscriptionLoaded){
									options.subscriptionLoaded.fireAction();
								}
								
							}
						});
			}
		} catch (Exception e) {
			running= false;
			logger.handleError("failed to start stream: ",e);
		}
	}
	
}
