package ly.xstream;

public class UsageData {
	public UsageDataPoint[] items;
	
	@Override
	public String toString(){
		String s="";
			for(UsageDataPoint p: items){
				s+="["+p.date+", connections:"+p.maxConcurentConnections+" messages sent:"+p.messagesSent+"], ";
			}
		return s;
	}
}
