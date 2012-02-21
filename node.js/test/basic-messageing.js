var XStreamly = require('../lib/xstreamly');

module.exports = {
  setUp: function(callback) {
    this.client = new XStreamly('tests','402ab188-7408-4d6d-a90f-59ba0a2f177e');
    this.messageType= 'messageType';
    this.eventType = 'eventType';
    this.channelName = 'unitTests';
    callback();
  },
  tearDown: function(callback){
    if(this.client){
      this.client.stop();
    }
    callback();
  },
  simpleMessage: function(test){
    var channel = this.client.subscribe(this.channelName,{includeMyMessages:true});
    var name = 'Brian';
    channel.bind(this.messageType,function(data){
      test.equals(name,data.name);
      test.done();
    });

    channel.trigger(this.messageType,{name:name});
  },
  bindAll: function(test){
    var channel = this.client.subscribe(this.channelName,{includeMyMessages:true});
    var name = 'Brian';
    var self = this;
    channel.bind_all(function(messageType,data){
      if(messageType===self.messageType){
        test.equals(name,data.name);
        test.done();
      }
    });

    channel.trigger(this.messageType,{name:name});
  },
  persistedMessageSent: function(test){
    var channel = this.client.subscribe(this.channelName,{includeMyMessages:true,includePersistedMessages:true});

    channel.bind(this.messageType,function(data,key){
      //the persisted message should be skipped
      //TODO:
      //channel.removePersistedMessage(key);
      
      if(data.valid2){
        test.done();
      }
    });

    
    channel.trigger(this.messageType,{valid2:true},true);
  }
}


