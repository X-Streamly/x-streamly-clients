package ly.xstream.streaming;

import java.util.HashMap;

public class Members {
	HashMap<String,Member> members = new HashMap<String,Member>();
	
	public Member get(String memberId){
		return members.get(memberId);
	}
	
	void add(Member member){
		members.put(member.id, member);
	}
	
	void remove(String memberId){
		members.remove(memberId);
	}
	
	Member socketId(String socketId){
		for(Member m:members.values()){
			if(m.containsSocket(socketId)){
				return m;
			}
		}
		
		return null;
	}
	
	public int getCount(){
		return members.size();
	}
}
