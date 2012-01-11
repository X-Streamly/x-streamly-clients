module.exports.port = undefined;
module.exports.MESSAGE_URL = 'PicTacToe/XStreamly/Messages';

module.exports.log = function(message){
  console.log(message);
}

module.exports.reportError = function(error){
  console.error(error);
}
