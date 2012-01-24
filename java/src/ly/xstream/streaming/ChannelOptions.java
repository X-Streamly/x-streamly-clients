package ly.xstream.streaming;

import java.util.HashMap;

public class ChannelOptions {
	public Boolean includeMyMessages = false;
	public Boolean includePersistedMessags = false;
	public Boolean createPresence = true;
	public Boolean isPrivate = false;
	
	public String userId;
	public HashMap<String,String> userInfo;
	
	public SimpleAction subscriptionLoaded;
}
