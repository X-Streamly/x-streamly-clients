var env = require('../env');

module.exports = Stream;

function Stream(connection, options) {
    var running = false;
    var self = this;
    
    if(!options){
    	throw new Error("options must be set.");
    }
    
    if(!options.url){
    	throw new Error("options.url can't be empty");
    }

    this.start = function() {
        env.log("starting " + options.url + "?" + options.subscription);
        var randomNumber = Math.random();
        self.sourceId = connection.socket.socket.sessionid + "|" + options.url + "|" + options.subscription + "|" + randomNumber;
        running = true;
        
        connection.socket.on('closed-'+self.sourceId,function(){
        	env.log('connection to the hog was reset, need to start streaming again');
        	if(options.streamResetAction){
        		options.streamResetAction();
        	}
        	startStreaming();
        });

        connection.socket.on(self.sourceId, function(update) {
            if (running && null != options) {
                if (update.action === "add" && null != options.addAction) {
                    options.addAction(update.item);
                }
                if (update.action === "modify" && null != options.modifyAction) {
                    options.modifyAction(update.item);
                }
                if (update.action === "delete" && null != options.deleteAction) {
                    options.deleteAction(update.item);
                }
            }
        });
        var startStreaming = function(){
		    if (options.subscription) {
		        var updatesOnly = false;

		        if (null != options && null != options.updatesOnly) {
		            updatesOnly = options.updatesOnly;
		        }
		        env.log('starting streaming');
		        connection.socket.emit('stream', {
		            id: self.sourceId,
		            uri: options.url,
		            updatesOnly: updatesOnly,
		            subscription: options.subscription
		        },
			    function(data) {
			    	env.log('subscription loaded');
			        if (options.subscriptionLoaded) {
			            options.subscriptionLoaded();
			        }
			    });
		    }
		}
		startStreaming();
    }

    this.close = function() {
        running = false;
        //TOOD: realy un subscribe
    }
}
