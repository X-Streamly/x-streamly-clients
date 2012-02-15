package ly.xstream.streaming;

import java.util.*;

public class Member {
	public String id;
	HashMap<String,String> memberInfo;
	
	private HashMap<String,MemberMessage> records = new HashMap<String, MemberMessage>();
	
	Member(String id, HashMap<String,String> memberInfo){
		this.id = id;
	    this.memberInfo = memberInfo;
	}
	
	public Map<String,String> getMemberInfo(){
		return memberInfo;
	}
		
	void addRecord(MemberMessage record){
		records.put(record.item.Key, record);
	}
	
	void removeRecord(String key){
		records.remove(key);
	}
	
	Boolean containsSocket(String socketId){
		for(String key: records.keySet()){
			if(records.get(key).item.SocketId.equals(socketId)){
				return true;
			}
		}
		return false;
	}
	
	Boolean containsKey(String key){
		return records.containsKey(key);
	}
	
	Boolean isAlive(){
		return !records.isEmpty();
	}
}
