package ly.xstream.streaming;

class DataMessage {
	public String AppKey;
	public String Channel;
	public String EventName;
	public String SocketId;
	public String Message;
	
	public DataMessage(String appKey, String channel,String eventName,String socketId, String message){
		AppKey = appKey;
		Channel= channel;
		EventName = eventName;
		SocketId=socketId;
		Message= message;
	}
}
