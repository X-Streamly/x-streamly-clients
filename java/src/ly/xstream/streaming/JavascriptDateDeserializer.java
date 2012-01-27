package ly.xstream.streaming;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.google.gson.*;

class JavascriptDateDeserializer implements JsonDeserializer<Date>,JsonSerializer<Date>{

	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	
	public JavascriptDateDeserializer(){
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
		
	@Override
	public Date deserialize(JsonElement json, Type typeofT,
			JsonDeserializationContext context) throws JsonParseException {

		String s = json.toString();
		s= s.replace("\"", "");
		
		try{
			DateTime time = new DateTime(s);
			return time.toDate();
		} catch (Exception e) {
			throw new JsonParseException("can't deserialize: "+s+"\n"+e.toString());
		}
		
	}
	@Override
	public JsonElement serialize(Date date, Type type,
			JsonSerializationContext context) {	
		DateTime time = new DateTime(date);
		time= time.toDateTime(DateTimeZone.UTC);
		String s = time.toString();
	
		return new JsonPrimitive(s);
	}
	
}
