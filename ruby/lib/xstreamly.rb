require 'json' #gem install json
require 'base64'
require 'cgi'
require 'openssl'
require 'net/https'
require 'net/http'

module XStreamly
	class Client
    def initialize(appKey,email,password)
      @appKey = appKey
      @email = email
      @password = password
      @http = Net::HTTP.new('secure.x-stream.ly', 443)
      @http.use_ssl = true
    end
		
    def send(channel, eventName, data)
	    req = Net::HTTP::Post.new(URI.encode('/api/v1.0/'+@appKey+'/channels/'+channel+'/events/'+eventName), initheader = {'Content-Type' =>'application/json'})
	    req.basic_auth @email, @password

	    req.body = data
	    response = @http.request(req)
	    result = response.code.to_i
      case result
          when 200..299
          	return true
          else 
          	raise RuntimeError, "Unknown error (status code #{result}): #{response.body}"
          end
    end
        
		def setCallback(channel,endPoint,secret,eventName)
      req = Net::HTTP::Post.new(URI.encode('/api/v1.1/'+@appKey+'/feeds/out/custom'), initheader = {'Content-Type' =>'application/json'})
	    req.basic_auth @email, @password

	    req.body = '{"channel":"'+channel+'","endpoint":"'+endPoint+'", "secret":"'+secret+'"}'
	    response = @http.request(req)
          
      case response.code.to_i
        when 200..299
        	return true
        else 
        	raise RuntimeError, "Unknown error (status code #{response.code} ): #{response.body}"
    	end
    end
        
    def removeCallback(index)
      req = Net::HTTP::Delete.new(URI.encode('/api/v1.1/'+@appKey+'/feeds/out/custom/'+index.to_s), initheader = {'Content-Type' =>'application/json'})
	    req.basic_auth @email, @password
      response = @http.request(req)
	
      case response.code.to_i
        when 200..299
        	return true
        else 
        	raise RuntimeError, "Unknown error (status code #{response.code} ): #{response.body}"
    	end
    end
        
    def getCallbacks()
    	req = Net::HTTP::Get.new(URI.encode('/api/v1.1/'+@appKey+'/feeds/out/custom'), initheader = {'Content-Type' =>'application/json'})
	    req.basic_auth @email, @password
	    response = @http.request(req)
	
      case response.code.to_i
        when 200..299
        	return (JSON(response.body))['items']
        else 
        	raise RuntimeError, "Unknown error (status code #{response.code} ): #{response.body}"
    	end
    end
    
    def setTwitterStream(channel,event,requestData)
      req = Net::HTTP::Post.new(URI.encode('/api/v1.1/'+@appKey+'/feeds/in/twitter'), initheader = {'Content-Type' =>'application/json'})
	    req.basic_auth @email, @password

	    req.body = '{"channel":"'+channel+'","event":"'+event+'", "requestData":"'+requestData+'"}'
	    response = @http.request(req)
          
      case response.code.to_i
        when 200..299
        	return true
        else 
        	raise RuntimeError, "Unknown error (status code #{response.code} ): #{response.body}"
    	end
    end
    
    def generateRequestData(trackParameter,consumerKey,consumerSecret,accessToken,accessTokenSecret)
      #generate a random nonce
      nonce  =(0...10).map{65.+(rand(25)).chr}.join
      timestamp = (Time.now.to_i).to_s
      track = CGI.escape(trackParameter)
      
      base = "POST&https%3A%2F%2Fstream.twitter.com%2F1%2Fstatuses%2Ffilter.json&" +
        "oauth_consumer_key%3D" + consumerKey +
        "%26oauth_nonce%3D"+nonce +
        "%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D" + timestamp +
        "%26oauth_token%3D" + accessToken +
        "%26oauth_version%3D1.0"+ 
        "%26track%3D" +track
        
      key = consumerSecret +'&'+CGI.escape(accessTokenSecret);
      
      digest  = OpenSSL::Digest::Digest.new('sha1')
      signature=CGI.escape(Base64.encode64(OpenSSL::HMAC.digest(digest,key,base)).strip)
      
      requestData = "oauth_signature=" + signature +
        "&oauth_token=" + accessToken +
        "&track=" + track +
        "&oauth_consumer_key=" + consumerKey +
        "&oauth_signature_method=HMAC-SHA1&oauth_version=1.0" +
        "&oauth_nonce=" + nonce +
        "&oauth_timestamp=" + timestamp
      
      return requestData
    end
        
    def removeTwitterStream(index)
      req = Net::HTTP::Delete.new(URI.encode('/api/v1.1/'+@appKey+'/feeds/in/twitter/'+index.to_s), initheader = {'Content-Type' =>'application/json'})
	    req.basic_auth @email, @password
      response = @http.request(req)
	
      case response.code.to_i
        when 200..299
        	return true
        else 
        	raise RuntimeError, "Unknown error (status code #{response.code} ): #{response.body}"
    	end
    end
        
    def getTwitterStreams()
    	req = Net::HTTP::Get.new(URI.encode('/api/v1.1/'+@appKey+'/feeds/in/twitter'), initheader = {'Content-Type' =>'application/json'})
	    req.basic_auth @email, @password
	    response = @http.request(req)
	
      case response.code.to_i
        when 200..299
        	return (JSON(response.body))['items']
        else 
        	raise RuntimeError, "Unknown error (status code #{response.code} ): #{response.body}"
    	end
    end

  end
end

client = XStreamly::Client.new('10bc1643-c9f5-4210-9814-cae3203af316','bwillard@x-stream.ly','dcba4321');
client.setCallback('frank','http://127.0.0.1:3002','secret','event');
