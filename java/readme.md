X-Stream.ly Client
==================

This is a Java client library for interacting with [X-Stream.ly](http://x-stream.ly)


#### To include

Add the xstreamly.jar to you project.


#### To use

The API for this library is exactly the same as for the browser based JS API

first off you need to import the types

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
   
To delete a security token:

   client.deleteToken(token)
   
   
For more detailed instructions check out [documentation](http://x-stream.ly/documentation.html)