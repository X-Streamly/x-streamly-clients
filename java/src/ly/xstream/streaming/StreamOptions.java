package ly.xstream.streaming;

class StreamOptions {
	public StreamOptions(String id, String uri, Boolean updatesOnly, String subscription){
		this.id = id;
		this.uri = uri;
		this.updatesOnly =updatesOnly;
		this.subscription = subscription;
	}
	
	public String id;
	public String uri;
	public Boolean updatesOnly;
	public String subscription;
	
}
