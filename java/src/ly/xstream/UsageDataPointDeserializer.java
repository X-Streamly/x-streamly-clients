package ly.xstream;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class UsageDataPointDeserializer implements JsonDeserializer<UsageDataPoint> {
	
	DateFormat df = new  SimpleDateFormat("yyyy-MM-dd HH:ss:mm.SSS");
	
	@Override
	public UsageDataPoint deserialize(JsonElement element, Type type,
			JsonDeserializationContext context) throws JsonParseException {
		JsonArray array = element.getAsJsonArray();
		
		UsageDataPoint value= new UsageDataPoint();
		
		try {
			String  s = array.get(0).toString();
			s=s.replace('T', ' ');
			s=s.replace('Z', ' ');
			s=s.replace('"', ' ');
			s=s.trim();
			value.time =df.parse(s);
		} catch (ParseException e) {
			throw new JsonParseException("Couldn't parse: "+element.toString());
		}
		
		value.amount =Integer.parseInt(array.get(1).toString());
		
		return value;
	}

}
