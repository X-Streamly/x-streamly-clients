package ly.xstream.streaming;

import java.util.*;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class StreamingClient implements ILogger {
	
	public static int Port = 80;
	static final String MessageUrl = "PicTacToe/XStreamly/Messages";
	static Gson XStreaemlyGson;
	
	private String appKey;
	
	private Cerrio cerrio;
	
	private List<ILogger> loggers = new Vector<ILogger>();
	
		
	public StreamingClient(String appKey, String securityToken){
		this.appKey =appKey;
		cerrio = new Cerrio(StreamingClient.Port,this);
		cerrio.applySession(securityToken);
		
		GsonBuilder builder = new GsonBuilder();
		
		builder.registerTypeAdapter(Date.class, new JavascriptDateDeserializer());
				
		XStreaemlyGson = builder.create();
		
	}
	
	/** Subscribes to a channel, once you have a channel you can bind to
	 * events that happen on the channel and send messages on the channel
	 * 
	 * @param channelName		The name of the channel
	 * @param options			Options that specify the behavcior of the channel
	 */
	public Channel subscribe(String channelName,ChannelOptions options){
		if(!Pattern.matches("^[\\w\\s:-]+$",channelName)){
			throw new Error(channelName+" isn't a valid name, channel names should only contains letters numbers and -_");
		}
		
		Channel channel = new Channel(channelName, appKey, cerrio, options,this);
		
		return channel;
	}
	
	/** Deletes all persisted messages associated with a channel
	 * 
	 * @param channelName		The name of the channel
	 */
	public void deleteChannel(String channelName) {
		
		SubscriptionOptions options = new SubscriptionOptions();
		options.uri=StreamingClient.MessageUrl;
		options.subscription = "@.AppKey='" + appKey + "' and @.Channel = '"+channelName+"'";
		options.addAction = new IDataUpdateHandler() {
			@Override
			public void handleUpdate(String data) {
				XstreamlyMessage message = StreamingClient.XStreaemlyGson.fromJson(data, XstreamlyMessage.class);
				cerrio.sendDelete(StreamingClient.MessageUrl,message.Key);
				
			}
		};
		
		SubscriptionCloser closer = new SubscriptionCloser();
		
		options.subscriptionLoaded = closer;
		
		Stream stream = this.cerrio.subscribe(options);
		closer.stream = stream;
	}
	

	/** Will stream a list of channels to the callback specified
	 * 
	 * @param callback		The IChannelCallback to be called when a channel is created (it will also be called for all existing channels)
	 */
	public IClosable listChannels(IChannelCallback callback) {
		SubscriptionOptions options = new SubscriptionOptions();
		options.uri=StreamingClient.MessageUrl;
		options.subscription = "modified(@.Key) and @.AppKey='" + appKey + "'";
		
		ChannelStreamer streamer = new ChannelStreamer(callback);
		
		options.addAction = streamer;
		
		streamer.stream = this.cerrio.subscribe(options);
		return streamer;
	}
	
	/** Will stream events to the callback when ever a client enters of leaves a channel
	 * 
	 * @param callback		The IActiveChannelCallback to be called when a user enters or leaves a channel
	 */
	public IClosable listActiveChannels(IActiveChannelCallback callback) {
		SubscriptionOptions options = new SubscriptionOptions();
		options.uri="PicTacToe/XStreamly/StatsPresence";
		options.subscription = "@.AppKey='" + appKey + "'";
		
		ActiveChannelStreamer streamer = new ActiveChannelStreamer(callback);
		
		options.addAction = streamer;
		options.modifyAction = streamer;
		
		streamer.stream = this.cerrio.subscribe(options);
		return streamer;
	}
	
	/** Will stream events to the callback when ever a client enters of leaves a specific channel
	 * 
	 * @param channel		The channel name to watch
	 * @param callback		The IActiveChannelCallback to be called when a user enters or leaves the specified channel
	 */
	public IClosable getChanelActivity(String channel,IActiveChannelCallback callback) {
		SubscriptionOptions options = new SubscriptionOptions();
		options.uri="PicTacToe/XStreamly/StatsPresence";
		options.subscription = "@.AppKey='" + appKey + "' and @.Channel = '"+channel+"'";
		
		ActiveChannelStreamer streamer = new ActiveChannelStreamer(callback);
		
		options.addAction = streamer;
		options.modifyAction = streamer;
		
		streamer.stream = this.cerrio.subscribe(options);
		return streamer;
	}
	
	/** Total stops all connections to the X-Stream.ly server
	 */
	public void stop() {
		log("stopping");
		this.cerrio.stop();
	}
	
	/** Adds extra permissions to the connection
	 * 
	 * @param securityToken		The GUID for the security token to add to the connection
	 */
	public void addSecurityToken(String securityToken){
		this.cerrio.applySession(securityToken);
	}
	
	/** Adds a logger that will be notified when any errors happen inside the library
	 * 
	 * @param handler		The ILogger to notify
	 */
	public void addLogger(ILogger handler){
		loggers.add(handler);
	}
	
	/** Removes a logger
	 * 
	 * @param handler		The ILogger to remove
	 */
	public void removeLogger(ILogger handler){
		loggers.remove(handler);
	}

	@Override
	public void handleError(String message,Exception e) {
		log("Error: "+message+" "+e.toString());
		for(ILogger logger: loggers){
			logger.handleError(message,e);
		}
	}
	
	public void log(String message){
		for(ILogger logger: loggers){
			logger.log(message);
		}
	}

	private class SubscriptionCloser implements SimpleAction{
		
		Stream stream = null;
		
		@Override
		public void fireAction() {
			if(null!=stream){
				stream.close();
			}
		}
		
	}

	private class ChannelStreamer implements IDataUpdateHandler,IClosable{

		Stream stream;
		private IChannelCallback callback;
		private HashSet<String> knownChannels = new HashSet<String>();
		
		public ChannelStreamer(IChannelCallback callback) {
			this.callback = callback;
		}
		
		@Override
		public void handleUpdate(String data) {
			XstreamlyMessage message = StreamingClient.XStreaemlyGson.fromJson(data, XstreamlyMessage.class);
			if(!knownChannels.contains(message.Channel)){
				callback.channelFound(message.Channel);
				knownChannels.add(message.Channel);
			}
		}
		
		@Override
		public void close(){
			if(null!=stream){
				stream.close();
			}
		}
		
	}
	
	private class ActiveChannelStreamer implements IDataUpdateHandler,IClosable{

		Stream stream;
		private IActiveChannelCallback callback;
		
		private HashMap<String,ChannelData> savedChannelData = new HashMap<String, ChannelData>();
		
		public ActiveChannelStreamer(IActiveChannelCallback callback) {
			this.callback = callback;
		}
		
		@Override
		public void handleUpdate(String data) {
			
			ChannelData newMessage = StreamingClient.XStreaemlyGson.fromJson(data, ChannelData.class);
			ChannelData savedMessage;
			if(!savedChannelData.containsKey(newMessage.Key)){
				savedChannelData.put(newMessage.Key, newMessage);
				savedMessage = newMessage;
			} else {
				savedMessage = savedChannelData.get(newMessage.Key);
				if(data.contains("MaxConcurrentConnections")){
					savedMessage.MaxConcurrentConnections=newMessage.MaxConcurrentConnections;
				}
				
				if(data.contains("CurrentConcurrentConnections")){
					savedMessage.CurrentConcurrentConnections=newMessage.CurrentConcurrentConnections;
				}
			}
			
			callback.channelChanged(savedMessage);
		}
		
		@Override
		public void close(){
			if(null!=stream){
				stream.close();
			}
		}
		
	}
}
