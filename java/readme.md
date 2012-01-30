X-Stream.ly Client
==================

This is a Java client library for interacting with [X-Stream.ly](http://x-stream.ly)


#### To include

Add all the jars from the dist folder your project.


#### To use the RESTfull API

First off you need to import the types

    import ly.xstream.*;


next you need to create your client:

    ResfullClient client = new ResfullClient("app key","email address","password");
  
send a message (data must be a JSON serializable object):

    client.send("MyChannel","MyEvent",data);

To list the active channels:

    Channels channels = client.activeChannels()
  
To create a new security token:

    String token = client.createToken(canRead, canWrite, channel, event, source, isPrivate);
  
To list all security tokens:

    for(Token t:client.getTokens().sessions){
        System.out.println(t.key);
    }
    
To list all security tokens that satisfy a particular condition:

    for(Token t:client.getTokens('channel','event','source').sessions){
        System.out.println(t.key);
    }
Any property left null will match all values.
   
To delete a security token:

    client.deleteToken(token)
   

#### To use the streaming API

First off you need to import the types

    import ly.xstream.streaming.*;
 
 next you need to create your client:
 
    StreamingClient streamingClient = new StreamingClient("app key","security token");
    
    
To subscribe to a channel you need to create a new ChannelOptions and pass it to the the subscribe function
    
    ChannelOptions options = new ChannelOptions();
    options.includeMyMessages = true; //call my callback even on my messages
    options.includePersistedMessags = false; // don't load persisted messages
    Channel channel = streamingClient.subscribe("myChannel", options);
    
To listen for messages from on a channel:

    channel.bindAll(new IXstreamlyMessageHandler() {
      @Override
      public void handleMessage(String eventName, String messageData, Member member, String key) {
        System.out.println("Got message "+eventName+" data: "+messageData+ " from "+member.id);
      }
    });
    
To listen for a specific event on a channel:

    channel.bind("myEvent",new IXstreamlyMessageHandler()
      @Override
      public void handleMessage(String eventName, String messageData, Member member, String key) {
        System.out.println("Got message "+eventName+" data: "+messageData+ " from "+member.id);
      }
    });
   
To receive notifications when users enter or leave a channel:

    channel.bindToChannelEvents(new IXstreamlyChannelEventsHandler() {
      @Override
      public void membmerModified(Member member) {
        System.out.println("Member modified");
      }
      @Override
      public void memberRemoved(Member member) {
        System.out.println("Member removed");
      }
      @Override
      public void memberAdded(Member member) {
        System.out.println("Member added");
      }
      @Override
      public void loaded(Members members) {
       System.out.println("Members loaded:" +members.getCount());
      }
    });
    
To send a message just pass the object to send into the trigger call:

    SimpleData data = new SimpleData();
    data.value1 = "frankasdfasdfasdf";
    data.value2 = "joe";
    Boolean sendAsPersistedMessage = false;
    channel.trigger("myEvent", data, sendAsPersistedMessage);
    
To be notified when anyone enters or leave any channel call listActiveChannels:

    streamingClient.listActiveChannels(new IActiveChannelCallback() {
      @Override
      public void channelChanged(ChannelData channel) {
        System.out.println("Got channel data "+channel.Channel+" max: "+channel.MaxConcurrentConnections+" current: "+channel.CurrentConcurrentConnections);	
      }
    });
    
To be notified when anyone enters or leave a specific channel call getChanelActivity:

    streamingClient.getChanelActivity(channelName,new IActiveChannelCallback() {
      @Override
      public void channelChanged(ChannelData channel) {
        System.out.println("Got channel data "+channel.Channel+" max: "+channel.MaxConcurrentConnections+" current: "+channel.CurrentConcurrentConnections);	
      }
    });
    
To just be notifed when a new channel is created call listChannels:

    streamingClient.listChannels(new IChannelCallback() {
      @Override
      public void channelFound(String channel) {
        System.out.println("new channel found: "+channel);
      }
    });
    
To log internal state and be notified of any errors that happen register a logger:

    streamingClient.addLogger(new ILogger() {
      @Override
      public void log(String s) {
       System.out.println(s);
      }
      @Override
      public void handleError(String message, Exception e) {
        System.out.println(message+ " "+e.toString());
      }
    });
			
##### Notes

   - The Java Streaming API communicates over HTTP and not HTTPS like all our other APIs.  This is because I haven't ben able to get the
     SSLEngine working with NIO to communicate via HTTPS.  This is on the TODO list, if this is a make or break feature for you please
     let us know (support@x-stream.ly) and we will move it higher on the list.
     
#### Build

This project is dependent on many other projects, I have included the more mainstream projects as libs in the repository but I will
leave it to the user to checkout the socket.io library, both to make sure you get the lastest and greatest and to make sure that
clwillingham gets the credit for his amazing work.

   - [java-socket.io.client](https://github.com/clwillingham/java-socket.io.client) by clwillingham


For more detailed instructions check out our [documentation](http://x-stream.ly/documentation.html)