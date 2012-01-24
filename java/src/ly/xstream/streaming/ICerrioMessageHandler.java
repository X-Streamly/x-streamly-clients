package ly.xstream.streaming;

import org.json.JSONObject;

interface ICerrioMessageHandler {
	void handleMessae(String eventName, JSONObject data);
}
