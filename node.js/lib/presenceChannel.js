var env = require('./env');
var Members = require('./members');
var Member = require('./member');

module.exports = PresenceChannel;

function PresenceChannel(name, appKey, cerrio,memberId,memberData,mainChannel, isPrivate){
    this.uri = 'PicTacToe/XStreamly/Presence';
    this.appKey = appKey;
    this.channelName = name;
    this.members = new Members();
    this.loaded = false;
    this.key = null;
    PresenceChannel.Instances= {};
    PresenceChannel.Instances[this.channelName] = this;
    var channelName = this.channelName;
    var timerId;

    //if no member id is defined then generate one that will be
    //constant across reconects
    if (!memberId) {
        if (!PresenceChannel.memberId) {
            PresenceChannel.memberId=Math.round(Math.random() * 1000000);
        }

        memberId = PresenceChannel.memberId.toString();
    }
    this.memberId = memberId;
    if(!memberData) {
      memberData = { memberId: memberId }
    }
    else if(!memberData.memberId){
    	memberData.memberId = memberId;
    }
    
    var me = this;

    this._memberInfo = memberData;

    this.memberInfo = function(name, value) {
      if(undefined === value && typeof name === 'string') {
        return me._memberInfo[name];
      } else {
        if(typeof name === 'string') {
          me._memberInfo[name] = value;
        } else if(typeof name === 'object') {
          $.each(name, function(key, value) {
            me._memberInfo[key] = value;
          });
        }
        cerrio.modify(uri, {
            Key: me.key,
            MemberInfo: JSON.stringify(me._memberInfo)
        });
      }
    }

    this.addAction =function(item){
      if(undefined === item.MemberId && item.MemberInfo) {
        try {
          item.MemberId = JSON.parse(item.MemberInfo).memberId;
        } catch(ex) { }
      }
    	var member = me.members.get(item.MemberId);
        if(undefined === member) {
            member = new Member(item.MemberId,item.MemberInfo);
            me.members.add(member);
        }
        member.addRecord(item);
        
        if(me.loaded){
            mainChannel.fireEvent("xstreamly:member_added",member);
        }
        
        
        if(item.Key===me.key){
            env.log('joined presence channel with:'+me.members.count);
            me.loaded=true;
            me.fireTimer();
            timerId =setInterval(me.fireTimer,10*1000);
            mainChannel.fireEvent("xstreamly:subscription_succeeded",me.members);
        }
    };
    
    this.deleteAction=function (item){
    	var member;
        me.members.each(function(m){
            if(m.containsKey(item.Key)){
                member=m;
            }
        });
        if(undefined!=member){
            member.removeRecord(item.Key);
            
            if(!member.alive()){
                me.members.remove(member.id);
                mainChannel.fireEvent("xstreamly:member_removed",member);
            }
        }
    };
    
    this.modifyAction=function(item){
    	var member;
        me.members.each(function(m){
            if(m.containsKey(item.Key)){
                member=m;
            }
        });
        if(undefined!=member){
          if(undefined!==item.MemberInfo) {
            try {
              member.memberInfo = JSON.parse(item.MemberInfo);
              mainChannel.fireEvent("xstreamly:member_modified",member);
            } catch(ex) { }
          }
        }
    };
       
    var stream = cerrio.subscribe({url:this.uri,
    	subscription:"@.AppKey='" + appKey + "' and @.Channel ='" + this.channelName + "' and @.Connected and @.Private ="+isPrivate,
    	addAction:this.addAction,
    	modifyAction:this.modifyAction,
    	deleteAction:this.deleteAction});

    this.fireTimer = function() {
	    cerrio.modify(uri, {
	        Key: me.key,
	        ChallengeTime: new Date(),
	        Connected: true
	    });
    };

    
    var fired = false;
    var uri = this.uri;
    var socket =this.socket;
    var action = function(){
        if(!fired){
        	me.key = appKey+'|'+channelName+'|'+cerrio.connection.socket.socket.sessionid+'|'+isPrivate;
            cerrio.add(uri,{
            	Key:me.key,
                Channel: channelName,
                AppKey:appKey,
                MemberId:memberId,
                MemberInfo: JSON.stringify(memberData),
                Verified: false,
                Connected: true,
                Challenge:0,
                ChallengeTime: (new Date()),
                Response:0,
                SocketId:cerrio.connection.socket.socket.sessionid,
                Private:isPrivate
            });
            //in cas the record already exists but is set to not connected
            //send in a mod            
           	cerrio.modify(uri, {
	        	Key: me.key,
	        	ChallengeTime: new Date(),
	        	Connected: true
	    	});
            fired=true;
        }
     };

     this.start = function() {
        env.log('starting prsence channel: '+this.channelName);
        cerrio.connection.onActive(action)
     };
     
     this.close = function(){
     	if(timerId){
     		clearInterval(timerId);
     		timerId = undefined;
     	}
     	
     	cerrio.sendDelete(uri,me.key);
     	
     	if(stream){
        	stream.close();
       	}
     }
}
