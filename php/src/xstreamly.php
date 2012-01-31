<?php

namespace XStreamly;

if(!function_exists('curl_init')){
  throw new \Exception('XStreamly need the CURL PHP extension.');
}

if(!function_exists('json_decode')){
  throw new \Exception('XStreamly need the JSON PHP extension.');
}

class XStreamlyClient{
  private $appKey;
  private $email;
  private $password;
  private $baseUrl = 'https://secure.x-stream.ly'; 

  public function __construct($appKey,$email,$password){
    $this->appKey = $appKey;
    $this->email = $email;
    $this->password = $password;
  }
  
  public function send($channel, $eventName, $data,$persisted = false){
    return $this->genericPost('/api/v1.1/' . $this->appKey . '/channels/' . $channel . '/events/' . $eventName . '?persisted=' . ($persisted?'true':'false'),$data);
  }
  
  //should be an array of objects with properties channel,event,message,[persisted]
  public function sendBatch($messages){
    return $this->genericPost('/api/v1.1/' . $this->appKey . '/messages',$messages);
  }
  
  public function setCallback($channel, $endPoint,$secret, $eventName){
    $data = array('channel'=>$channel,'endpoint'=>$endPoint,'secret'=>$secret,'event'=>$eventName);
    return $this->genericPost('/api/v1.1/' . $this->appKey . '/feeds/out/custom',$data);
  }
  
  public function removeCallback($key){
    return $this->genericDelete('/api/v1.1/' . $this->appKey . '/feeds/out/custom/'.$key);
  }
  
  public function getCallbacks(){
    return $this->genericGet('/api/v1.1/' . $this->appKey . '/feeds/out/custom')->items;
  }
  
  public function getActiveChannels(){
    return $this->genericGet('/api/v1.1/' . $this->appKey . '/activeChannels');
  }
  
  public function getConnectionUsage(){
    return $this->genericGet('/usage/connections');
  }
  
  public function getMessageUsage(){
    return $this->genericGet('/usage/messages');
  }
  
  public function getTokens($channel=null, $eventName=null, $source=null){
    $params = '';
    
    if(!is_null($channel)){
      $params .='channel='.$channel;
    }
    
    if(!is_null($eventName)){
      $params .='event='.$eventName;
    }
    
    if(!is_null($source)){
      $params .='source='.$source;
    }
    
    if(strlen($params)){
      $params = "?" . $params;
    }
  
    return $this->genericGet('/api/v1.1/' . $this->appKey . '/security/' . $params);
  }
  
  public function createToken($canRead = true, $canWrite = true, $channel = null, $event = null, $source = null, $isPrivate = false){
    $data = array();
    
    if(!$canRead || !$canWrite){
      if($canWrite){
        $action = 'read';
      } else {
        $action = 'write';
      }
      
      $data['action']=$action;
    }
    
    
    if(null!=$channel){
      $data['channel']=$channel;
    }
    
    if(null!=$event){
      $data['event']=$event;
    }
    
    if(null!=$source){
      $data['source']=$source;
    }
    
    return $this->genericPost('/api/v1.1/' . $this->appKey . '/security',$data);
  }
  
  public function deleteToken($token){
    return $this->genericDelete('/api/v1.1/' . $this->appKey . '/security/' . $token);
  }
  
  private function genericDelete($url){
    $request = $this->getRequest($url);
    curl_setopt($request,CURLOPT_CUSTOMREQUEST,'DELETE');
    
    return $this->execute($request);
  }
  
  private function genericGet($url){
    $request = $this->getRequest($url);
        
    return json_decode($this->execute($request));
  }
  
  private function genericPost($url,$data){
    $request = $this->getRequest($url);
    
    $stringData =json_encode($data);
        
    curl_setopt($request,CURLOPT_POST,1);
    curl_setopt($request,CURLOPT_HTTPHEADER,array('Content-type: application/json'));
    curl_setopt($request,CURLOPT_POSTFIELDS,$stringData);
    
    return $this->execute($request);   
  }
  
  private function getRequest($url){
    $request = curl_init($this->baseUrl . $url);
    curl_setopt($request,CURLOPT_USERPWD,$this->email.':'.$this->password);
    curl_setopt($request,CURLOPT_RETURNTRANSFER,1);
    curl_setopt($request,CURLOPT_FAILONERROR,1);
    return $request;
  }
  
  private function execute($request){
    $response = curl_exec($request);
       
    $error = curl_error($request);
    if($error){
      throw new \Exception("couldn't send message, got error: ". $error);
    }
    
    $http_status = curl_getinfo($request,CURLINFO_HTTP_CODE);
    curl_close($request);
    
    if($http_status ==302 && strpos($response,'login.html')){
      throw new \Exception('Sorry your credentials are invalid, please check your app Key, e-mail address and password to make sure they are correct.');
    }
    
    if($http_status == 200 || $http_status == 202){
      return $response;
    } else {
      throw new \Exception("couldn't send message, got response code: ". $http_status. " response: ".$response);
    }
  }
}

?>
