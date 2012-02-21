module.exports = Members;

function Members(){
    this.count=0;
    this.members = {};
    this.each = function(func){
        for (key in this.members) {
            func(this.members[key]);
        }
    }
    
    this.get = function(memberId){
        return this.members[memberId];
    }
    
    this.add = function(member){
        this.members[member.id] = member;
        this.count++;
    }
    
    this.remove = function(memberId){
        delete this.members[memberId];
        this.count--;
    }

    this.socketId = function(socketId) {
        for (key in this.members) {
          var member = this.members[key];
            if (member.containsSocket(socketId)) {
                return member;
            }
        }

        return null;
    }
}
