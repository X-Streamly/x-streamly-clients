package ly.xstream.streaming;

import org.json.JSONObject;

public interface IRawMessageHandler {
	void handleMessae(String eventName, JSONObject data);
}
