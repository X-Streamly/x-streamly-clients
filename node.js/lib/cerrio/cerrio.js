var Connection = require('./connection');
var Stream = require('./stream');

module.exports = Cerrio;

function Cerrio(port) {  
    this.connection = new Connection(port);

    this.subscribe = function(options) {
        var stream = new Stream(this.connection, options);
        this.connection.onActive(function() { stream.start(); }, true);
        return stream;
    }

    this.stop = function() {
    	this.connection.stop();
    }
    
    var connection = this.connection;

    this.add = function(uri, data) {
        if (!uri) {
            throw new Error("uri can't be null");
        }
        var sourceId = this.sourceId;
        connection.onActive(function() {
            connection.socket.emit('update', {
                id: sourceId + ":add",
                uri: uri,
                update: {
                    action: 'add',
                    item: data
                }
            });
        }, false);
    };

    this.modify = function(uri,data) {
    	if(!uri){
    		throw new Error("uri can't be null");
    	}
        var sourceId = this.sourceId;
        connection.onActive(function(){
            connection.socket.emit('update', {
                id: sourceId + ":modify",
                uri: uri,
                update: {
                    action: 'modify',
                    item: data
                }
            });
        },false);
    };

    this.sendDelete = function(uri,key) {
    	if(!uri){
    		throw new Error("uri can't be null");
    	}
        var sourceId = this.sourceId;
        connection.onActive(function(){
            connection.socket.emit('update', {
                id: sourceId + ":delete",
                uri: uri,
                update: {
                    action: 'delete',
                    itemKey: key
                }
            });
        },false);
    };
    
    this.applySession = function(token){
    	connection.applySession(token);
    };
}
