package ly.xstream.streaming;

class Cerrio {
	public Connection connection;
	private ILogger logger;
	
	public Cerrio(int port, ILogger logger){
		connection = new Connection(port,logger);
		this.logger = logger;
	}
	
	public Stream subscribe(SubscriptionOptions options){
		Stream stream = new Stream(connection,options,logger);
		StreamStarter streamStarter = new StreamStarter(stream);
		connection.onActive(streamStarter, true);
		return stream;
	}
	
	public void stop(){
		connection.stop();
	}
	
	void sendAdd(String uri,Object data){
		connection.onActive(new DataSender(new CerrioUpdateWrapper("add",uri,connection.id,data,null)), false);
	}
	
	void sendModify(String uri,Object data){	
		connection.onActive(new DataSender(new CerrioUpdateWrapper("modify",uri,connection.id,data,null)), false);
	}
	
	void sendModify(String uri,Object data,String... modifiedFields){	
		connection.onActive(new DataSender(new CerrioUpdateWrapper("modify",uri,connection.id,data,null,modifiedFields)), false);
	}
	
	void sendDelete(String uri,String itemKey){
		connection.onActive(new DataSender(new CerrioUpdateWrapper("delete",uri,connection.id,null,itemKey)), false);
	}
	
	public void applySession(String token){
		connection.applySession(token);
	}
	
	private class DataSender implements IConnectionActiveCallback{
		
		private CerrioUpdateWrapper wrapper;
		
		public DataSender(CerrioUpdateWrapper wrapper){
			this.wrapper = wrapper;
		}
		
		@Override
		public void onConnectionActive(Connection connection) {
			try {
				connection.send("update", wrapper);
			} catch (Exception e) {
				logger.handleError("problem sending data: ",e);
			}
		}
		
	}
	
	private class StreamStarter implements IConnectionActiveCallback{
		Stream stream;
		
		public StreamStarter(Stream stream){
			this.stream = stream;
		}
		
		
		@Override
		public void onConnectionActive(Connection connection) {
			stream.start();
		}
	}
}
