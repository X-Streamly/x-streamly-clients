require 'json' #gem install json
require 'net/https' unless defined?(Net::HTTPS)
require 'net/http' unless defined?(Net::HTTP)

module XStreamly
	class Client
		def initialize(appKey,email,password)
			@appKey = appKey
			@email = email
			@password = password
			@http = Net::HTTP.new('x-stream.ly', 80)
			@http.use_ssl = true
			
		end
		
		def send(channel, eventName, data)
			req = Net::HTTP::Post.new(URI.encode('/api/v1.0/'+@appKey+'/channels/'+channel+'/events/'+eventName), initheader = {'Content-Type' =>'application/json'})
			req.basic_auth @email, @password

			req.body = data
			response = @http.request(req)
            case response.code.to_i
            when 202
            	return true
            else 
            	raise RuntimeError, "Unknown error (status code #{response.code} ): #{response.body}"
        	end
        end
        
        def setCallback(channel,endPoint,secret,eventName)
       		req = Net::HTTP::Post.new(URI.encode('/api/v1.0/'+@appKey+'/subscriptions'), initheader = {'Content-Type' =>'application/json'})
			req.basic_auth @email, @password

			req.body = '{"channel":"'+channel+'","endpoint":"'+endPoint+'", "secret":"'+secret+'"}'
			response = @http.request(req)
            case response.code.to_i
            when 200
            	return true
            else 
            	raise RuntimeError, "Unknown error (status code #{response.code} ): #{response.body}"
        	end
        
        end
        
        def removeCallback(index)
            req = Net::HTTP::Delete.new(URI.encode('/api/v1.0/'+@appKey+'/subscriptions'+index.to_s), initheader = {'Content-Type' =>'application/json'})
			req.basic_auth @email, @password
			response = @http.request(req)
			
            case response.code.to_i
            when 202
            	return true
            else 
            	raise RuntimeError, "Unknown error (status code #{response.code} ): #{response.body}"
        	end
        end
        
        def getCallbacks()
        	req = Net::HTTP::Get.new(URI.encode('/api/v1.0/'+@appKey+'/subscriptions'), initheader = {'Content-Type' =>'application/json'})
			req.basic_auth @email, @password
			response = @http.request(req)
			
            case response.code.to_i
            when 200
            	return JSON(response.body)
            else 
            	raise RuntimeError, "Unknown error (status code #{response.code} ): #{response.body}"
        	end
        end
        
        def setLocalCallback(channel,eventName)
        
        end
    end
end
