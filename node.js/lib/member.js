module.exports = Member;

function Member(memberId, memberInfo){
    this.id = memberId;
    if(typeof memberInfo === 'string') {
      this.memberInfo = JSON.parse(memberInfo);
    } else if (typeof memberInfo === 'object') {
      this.memberInfo = memberInfo;
    }
    this.records = {};
    this.recordCount = 0;
    
    this.addRecord = function(record){
        this.records[record.Key]=record;
        this.recordCount++;
    }
    
    this.removeRecord = function(key){
        delete this.records[key];
        this.recordCount--;
    }
    
    this.containsSocket = function(socketId){
        for (key in this.records) {
            if (this.records[key].SocketId === socketId) {
                return true;
            }
        }
        
        return false;
    }
    
    this.containsKey = function(key){
        return undefined!==this.records[key];
    }
    
    this.alive = function(){
      return this.recordCount>0;
    }
}
