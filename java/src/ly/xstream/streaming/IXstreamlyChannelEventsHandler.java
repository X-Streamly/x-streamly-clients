package ly.xstream.streaming;

public interface IXstreamlyChannelEventsHandler {
	void loaded(Members members);
	
	void memberAdded(Member member);
	void memberRemoved(Member member);
	void membmerModified(Member member);
}
