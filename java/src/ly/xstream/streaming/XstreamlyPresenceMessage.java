package ly.xstream.streaming;

import java.util.Date;
import java.util.HashMap;

class XstreamlyPresenceMessage {
	String Key;
	String AppKey;
	String Channel;
	String MemberInfo;
	String SocketId;
	Boolean Connected;
	Integer Challenge;
	Date ChallengeTime;
	Integer Response;
	Boolean Verified;
	Boolean Private;
	
	//these fields are not really sent to X-Stream.ly but are just around
	//to make life easier for people
	public transient String memberId;
	public transient HashMap<String,String> memberInfo; 
}
