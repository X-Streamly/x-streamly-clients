X-Stream.ly Client
==================

This is a flash (ActionScript3) client library for interacting with [X-Stream.ly](http://x-stream.ly)


#### To include

Copy the xstreamly.swc file from the flash/bin directory to the lib directory of your project, then include it in your project.


#### To use

The API for this library is exactly the same as for the browser based JS API

first off you need to import the types

    import ly.xstream.XStreamlyClient;
    import ly.xstream.Channel;


next you need to create your client:

    var client:XStreamlyClient = new XStreamlyClient('app key', 'security token');
  
To subscribe to a channel:

   var channel:Channel = client.subscribe("myChannel", { includeMyMessages:true } );

To listen for messages of a specific type:

    channel.bind('message type',function(dataLObject):void{
      //do fun stuff
    });
  
To listen for all messages of a specific type:

    channel.bindAll(function(eventName:String,data:Object):void{
      //do fun stuff
    });
  
To Send a message:

   channel.trigger('message type',{name:'Brian'},false);
   
   
For more detailed instructions check out [documentation](http://x-stream.ly/documentation.html)

#### To Build

I have checked in the [FlashDevelop](http://www.flashdevelop.org/) project I use to build it.

To generate the SWC files I use [Export SWC](http://sourceforge.net/projects/exportswc/)

It is dependent on two excellent GitHub projects.  I have just included the binaries that I referenced but I encourage you 
to check out the actual projects.

   1) [Flash.IO](https://github.com/simb/FlashSocket.IO) by [Simeon Batman](https://github.com/simb), this is used to produce the
   FlashSocket.IO.bwillard.swc file in the libs folder.  (Technically right now it is produced of my branch of his project at: 
   https://github.com/bwillard/FlashSocket.IO so that I could add some functionality I needed.  But I have issued a pull request
   so hopefully that gets integrated soon.
   2) Flash.IO is dependent on [web-socket-js](https://github.com/simb/web-socket-js).
