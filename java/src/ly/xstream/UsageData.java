package ly.xstream;

public class UsageData {
	public String user;
	public UsageDataPoint[] data;
	
	@Override
	public String toString(){
		String s= "User: "+user+" data: ";
			for(UsageDataPoint p: data){
				s+="["+p.time+", "+p.amount+"], ";
			}
		return s;
	}
}
