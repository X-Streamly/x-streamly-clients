package ly.xstream.streaming;

public interface IXstreamlyMessageHandler {
	void handleMessage(String eventName, String messageData,Member memberInfo, String key);
}
