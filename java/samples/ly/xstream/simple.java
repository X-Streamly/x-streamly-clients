package ly.xstream;

import java.util.Date;

import ly.xstream.streaming.*;

public class simple {
	private static final String AppKey = "put my appKey here";
	private static final String Email = "put my e-mail address here";
	private static final String Password = "put my password here";
	private static final String SecurityToken = "put my security token here";
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			ResfullClient client = new ResfullClient(AppKey,Email,Password);
			Callback c = new Callback();
			c.channel="C";
			c.event="asdfadsf2";
			c.secret=new Date().toString();
			
			client.send("MyChannel","MyEvent",c);
			
			client.setCallback("MyChannel", "http://asdfasdfasdf.com", "Don't tell anyone", "bob");
			for(Callback callback : client.getCallbacks().items){
				System.out.println(callback);
				client.removeCallback(callback.key);
			}
			
			Channels channels = client.activeChannels();
			for(String key : channels.channels.keySet()){
				System.out.println("Channel "+key+", "+channels.channels.get(key));
			}
			
			System.out.println(client.usageConnections());
			
			System.out.println(client.usageConnections());
			
			String token = client.createToken(true, false, null, "Event", null, false);
			
			System.out.println(token);
			
			client.deleteToken(token);
			
			for(Token t:client.getTokens().sessions){
				System.out.println(t.key);
			}
			
			StreamingClient streamingClient = new StreamingClient(AppKey,SecurityToken);
			streamingClient.addLogger(new ILogger() {
				@Override
				public void log(String s) {
					System.out.println(s);
				}
				@Override
				public void handleError(String message, Exception e) {
					//already logged
				}
			});
			ChannelOptions options = new ChannelOptions();
			options.includeMyMessages = true; //call my callback even on my messages
			options.includePersistedMessags = false; // don't load persisted messages
			Channel channel = streamingClient.subscribe("myChannel", options);
			
			
			channel.bindAll(new IXstreamlyMessageHandler() {
				@Override
				public void handleMessage(String eventName, String messageData,
						Member member, String key) {
					System.out.println("Got message "+eventName+" data: "+messageData+ " from "+member.id);
					
				}
			});
			
			channel.bind("myEvent",new IXstreamlyMessageHandler() {
				@Override
				public void handleMessage(String eventName, String messageData,
						Member member, String key) {
					System.out.println("Got message "+eventName+" data: "+messageData+ " from "+member.id);
					
				}
			});
			
			channel.bindToChannelEvents(new IXstreamlyChannelEventsHandler() {
				@Override
				public void membmerModified(Member member) {
					System.out.println("Member modified");
				}
				@Override
				public void memberRemoved(Member member) {
					System.out.println("Member removed");
				}
				@Override
				public void memberAdded(Member member) {
					System.out.println("Member added");
				}
				@Override
				public void loaded(Members members) {
					System.out.println("Members loaded:" +members.getCount());
				}
			});
			
			SimpleData data = new SimpleData();
			data.value1 = "frankasdfasdfasdf";
			data.value2 = "joe";
			Boolean sendAsPersistedMessage = false;
			
			channel.trigger("myEvent", data, sendAsPersistedMessage);
			
			streamingClient.listActiveChannels(new IActiveChannelCallback() {
				@Override
				public void channelChanged(ChannelData channel) {
					System.out.println("Got channel data "+channel.Channel+" max: "+channel.MaxConcurrentConnections+" current: "+channel.CurrentConcurrentConnections);
					
				}
			});
			
			streamingClient.listChannels(new IChannelCallback() {
				@Override
				public void channelFound(String channel) {
					System.out.println("new channel found: "+channel);
				}
			});

			System.in.read();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("--Success"); 
	}
}
