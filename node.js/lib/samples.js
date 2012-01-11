var xstreamly = require('./xstreamly');
var client = new xstreamly('10bc1643-c9f5-4210-9814-cae3203af316','dd642fa8-7575-4a27-977e-0e0cb1e9a8ef');

var channel = client.subscribe('stream name',{includeMyMessages:true});

channel.bind('message type',function(data){
  console.log(data.name);
});

channel.trigger('message type',{name:'Brian'});
