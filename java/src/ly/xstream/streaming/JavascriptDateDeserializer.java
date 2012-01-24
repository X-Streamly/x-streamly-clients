package ly.xstream.streaming;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

class JavascriptDateDeserializer implements JsonDeserializer<Date>{

	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	@Override
	public Date deserialize(JsonElement json, Type typeofT,
			JsonDeserializationContext context) throws JsonParseException {
		
			String s = json.toString();
			s=s.replaceAll("\"", "");
			s=s.replaceAll("T", " ");
			s=s.replaceAll("Z", "");
		try{
			return format.parse(s);
		} catch (Exception e) {
			throw new JsonParseException("can't deserialize: "+s+"\n"+e.toString());
		}
		
	}
	
}
