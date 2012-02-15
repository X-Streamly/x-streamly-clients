package ly.xstream.streaming;

class MemberMessage {

	public String action;
	public MemberData item;
	
	class MemberData{
		public boolean Connected;
		public String Channel;
		public String Key;
		public String SocketId;
		public String AppKey;
		public String MemberId;
		public String Info;
	}
}
