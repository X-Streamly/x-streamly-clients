package ly.xstream;

public class Callback {
	public String channel;
	public String endpoint;
	public String secret;
	public String event;
	public String key;
	
	public String toString(){
		return key+ ": Channel: "+channel+" endpoint: "+endpoint+" event: "+event;
		
	}
}
