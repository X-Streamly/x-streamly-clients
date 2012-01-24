package ly.xstream.streaming;

class SubscriptionOptions {
	public String uri;
	public String subscription;
	
	public Boolean includeMyMessages = true;
	public Boolean includePersistedMessags = false;
	public Boolean updatesOnly = false;
	
	
	public SimpleAction streamResetAction;
	public SimpleAction subscriptionLoaded;;
	
	public IDataUpdateHandler addAction;
	public IDataUpdateHandler modifyAction;
	public IDataUpdateHandler deleteAction;
}
