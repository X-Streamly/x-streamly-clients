package ly.xstream.streaming;

import java.util.HashSet;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class CerrioUpdateWrapper {
	public String id;
	public String uri;
	public CerrioMessage update;
	public CerrioMessage[] updates;
	
	public transient HashSet<String> modifiedSet;
	public transient Gson gson = new Gson();
	
	public CerrioUpdateWrapper()
	{
	}
	
	public CerrioUpdateWrapper(String action, String uri,String connectionId,Object item,String itemKey,String... modifiedFields){
		if(null!=modifiedFields && modifiedFields.length>0){
			modifiedSet = new HashSet<String>();
			
			for(String field: modifiedFields){
				modifiedSet.add(field);
			}
			

			GsonBuilder builder = new GsonBuilder();
			
			builder.addSerializationExclusionStrategy(new ExclusionStrategy() {
				@Override
				public boolean shouldSkipField(FieldAttributes field) {
					if(field.getDeclaringClass()==CerrioUpdateWrapper.class
							||field.getDeclaringClass()==CerrioMessage.class){
						return false;
					}
					
					return !modifiedSet.contains(field.getName());
				}
				
				@Override
				public boolean shouldSkipClass(Class<?> arg0) {
					return false;
				}
			});
			
			gson = builder.create();
		}
		
		id = connectionId+":"+action;
		this.uri = uri;
	
		update = new CerrioMessage(action,item,itemKey);
	}

}
