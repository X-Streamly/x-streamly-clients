<?php



require_once '../src/xstreamly.php';

$xstreamlyClient = new XStreamly\XstreamlyClient('app key','email address','password');


//mesage sending

$xstreamlyClient->send('MyChannel','MyEvent',"some data",false);

$xstreamlyClient->send('MyChannel','MyEvent',array("xstreamly"=> "is cool","this"=>"is fun"),false);

$message1 = array("channel"=>"MyChannel","event"=>"MyEvent","message"=>"{\"json\":\"goes here\"}");
$message2 = array("channel"=>"MyChannel2","event"=>"MyEvent2","message"=>"{\"json\":\"goes here\"}");

$xstreamlyClient->sendBatch(array($message1,$message2));


//registering callbacks
$callBackKey = $xstreamlyClient->setCallback('MyChannel','http://my.endponit','secret','event');
echo "callback key: " . $callBackKey . "\n";
$callbacks = $xstreamlyClient->getCallbacks();
$firstcallback = $callbacks[0]; 
echo "callback: " . json_encode($firstcallback) . "\n";

$xstreamlyClient->removeCallback($callBackKey);


//usage data
echo "active channels: " . json_encode($xstreamlyClient->getActiveChannels()) ."\n";

echo "message usage: " . json_encode($xstreamlyClient->getMessageUsage()) ."\n";

echo "connection usage: " . json_encode($xstreamlyClient->getConnectionUsage()) ."\n";


//managing security tokens
$securityToken = $xstreamlyClient->createToken(true,true,null,"MyEvent",null,false);
echo "security token key: " . $securityToken . "\n";

echo "callback: " . json_encode($xstreamlyClient->getTokens(null,"MyEvent",null)) . "\n";

echo "token: " . json_encode($xstreamlyClient->getTokens()) . "\n";

$xstreamlyClient->deleteToken($securityToken);

?>
