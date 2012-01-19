package ly.xstream;

import java.util.Date;

public class simple {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ResfullClient client = new ResfullClient("10bc1643-c9f5-4210-9814-cae3203af316","bwillard@x-stream.ly","dcba1234");
		try {
			Callback c = new Callback();
			c.channel="C";
			c.event="asdfadsf2";
			c.secret=new Date().toString();
			
			client.send("MyChannel","MyEvent",c);
			
			/*client.setCallback("MyChannel", "http://asdfasdfasdf.com", "Don't tell anyone", "bob");
			for(Callback callback : client.getCallbacks().items){
				System.out.println(callback);
				client.removeCallback(callback.key);
			}*/
			
			/*Channels channels = client.activeChannels();
			for(String key : channels.channels.keySet()){
				System.out.println("Channel "+key+", "+channels.channels.get(key));
			}*/
			
			//System.out.println(client.usageConnections());
			
			//System.out.println(client.usageConnections());
			
			//String token = client.createToken(true, false, null, "Event", null, false);
			
			//System.out.println(token);
			
			//client.deleteToken(token);
			
			/*for(Token t:client.getTokens().sessions){
				System.out.println(t.key);
			}*/
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Success"); 
	}
}
