package ly.xstream.streaming;

class CerrioMessage {
	public String action;
	public Object item;
	public Object[] items;
	public String itemKey;
	
	public CerrioMessage(String action, Object item,String itemKey){
		this.action = action;
		this.item = item;
		this.itemKey = itemKey;
	}
}
