package ly.xstream;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.*;

public class UsageDataPointDeserializer implements JsonDeserializer<Date> {

	DateFormat df = new  SimpleDateFormat("yyyy-MM-dd");

	@Override
	public Date deserialize(JsonElement element, Type type,
			JsonDeserializationContext context) throws JsonParseException {

		try {
			String s = element.toString();
			s=s.trim();
			s=s.replace("\"", "");
			return df.parse(s);
		} catch (ParseException e) {
			throw new JsonParseException("Couldn't parse: "+element.toString(),e);
		}

	}

}