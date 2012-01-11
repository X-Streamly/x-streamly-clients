X-Stream.ly Client
==================

This is a node.js client library for intercting with [X-Stream.ly](http://x-stream.ly)


To include
----------
npm xstreamly-client


### Temporary hack

right now the npm version of socket.io-client doesn't support wss which
x-stream.ly requires this is due to issue # 336.  There is a pull request
pending for the fix.  Right now to make this library work you need to manual
install the version of socket.io-client, to so so run:

    npm install http://github.com/kazuyukitanimura/socket.io-client/tarball/master


#### To use

The API for this library is exactly the same as for the browser based JS API

first off you need to require the library:

    var xstreamly = require('xstreamly-client');


next you need to create your client:

    var client = new xstreamly('app key','security token');
  
To subscribe to a channel:

    var channel = client.subscribe('stream name',{includeMyMessages:true});

To listen for messages of a specific type:

    channel.bind('message type',function(data){
      //do fun stuff
    });
  
To listen for all messages of a specific type:

    channel.bind_all(function(eventName,data){
      //do fun stuff
    });
  
To Send a message:

   channel.trigger('message type',{name:'Brian'});
   
   
For more detailed instructions check out out [documentation](http://x-stream.ly/documentation.html)

#### To test

install nodeunit

    npm install nodeunit -g
    
run with nodeunit

    nodeunit ./test/
