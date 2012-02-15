using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Runtime.Serialization;
using System.Runtime.Serialization.Json;
using System.Security.Authentication;
using System.Security.Cryptography;
using System.Text;
using System.Web;
using System.Web.Script.Serialization;

namespace XStreamly.Client
{
    /// <summary>
    /// A client to interact with X-Stream.ly
    /// </summary>
    public class Client
    {
        private static readonly string s_xstreamlyHost = "https://api.x-stream.ly";
        private static readonly DateTime s_epoch = new DateTime(1970, 1, 1);
        private static readonly string s_callbackFormatString = "/api/v1.1/{0}/feeds/out/custom";
        private static readonly string s_presenceCallbackFormatString = "/api/v1.1/{0}/feeds/out/presence";
        private static readonly string s_twitterStreamFormatString = "/api/v1.1/{0}/feeds/in/twitter";
        private static readonly string s_tokenFormatString = "/api/v1.1/{0}/security";
        private static readonly string s_usageFormatString = "/api/v1.1/{0}/usage";

        private readonly string m_appKey;
        private readonly string m_emailAddress;
        private readonly string m_password;

        /// <summary>
        /// Create a new client object to interact with X-Stream.ly
        /// </summary>
        /// <param name="appKey">Your application key</param>
        /// <param name="emailAddress">The e-mail address you use to sign into X-Stream.ly with</param>
        /// <param name="password">The password you use to sign into X-Stream.ly with</param>
        public Client(string appKey, string emailAddress, string password)
        {
            m_appKey = appKey;
            m_emailAddress = emailAddress;
            m_password = password;
        }

        /// <summary>
        /// Send a message to a X-Stream.ly that will be distributed to all
        /// client listening to a particular channel
        /// </summary>
        /// <param name="channel">The channel name to send the message to</param>
        /// <param name="eventName">The event name the message will be sent with</param>
        /// <param name="data">The data that will be JSON serialized into the message</param>
        public void Send(string channel, string eventName, object data)
        {
            string url = "/api/v1.1/" + m_appKey + "/channels/" + channel + "/events/" + eventName;

            JavaScriptSerializer ser = new JavaScriptSerializer();
            string stringData = ser.Serialize(data);

            PostData(url, stringData);
        }

        /// <summary>
        /// Sends a batch of messages
        /// </summary>
        /// <param name="messages">One or messages to be sent</param>
        public void SendBatch(IEnumerable<Message> messages)
        {
            string url = "/api/v1.1/" + m_appKey + "/messages/";

            DataContractJsonSerializer ser = new DataContractJsonSerializer(typeof(IEnumerable<Message>));
            MemoryStream ms = new MemoryStream();
            ser.WriteObject(ms, messages);
            string stringData = Encoding.Default.GetString(ms.ToArray());

            //Console.WriteLine(stringData);
            PostData(url, stringData);
        }

        #region Callbacks
        /// <summary>
        /// Register a new callback for X-Stream.ly
        /// 
        /// A callback is used to register an end point on a server that you control
        /// with X-Stream.ly so ever time there is a message sent with X-Stream.ly to
        /// a specific channel (and optionally event name) your end point will be notified
        /// </summary>
        /// <param name="callback">The call back definition</param>
        public string SetCallback(Callback callback)
        {
            return SetCallback(callback, s_callbackFormatString);
        }

        /// <summary>
        /// Register a new presence callback for X-Stream.ly
        /// 
        /// A presence callback is used to register an end point on a server that you control
        /// with X-Stream.ly so ever time a channel becomes active or empty
        /// your end point will be notified
        /// </summary>
        /// <param name="callback">The call back definition</param>
        private string SetPresenceCallback(Callback callback)
        {
            return SetCallback(callback, s_presenceCallbackFormatString);
        }

        public string SetCallback(Callback callback,string formatString)
        {
            DataContractJsonSerializer ser = new DataContractJsonSerializer(typeof(Callback));

            MemoryStream ms = new MemoryStream();
            ser.WriteObject(ms, callback);
            string stringData = Encoding.Default.GetString(ms.ToArray());

            return PostData(string.Format(formatString, m_appKey), stringData);
        }

        /// <summary>
        /// Removes an existing callback from X-Stream.ly
        /// </summary>
        /// <param name="key"></param>
        public void RemoveCallback(string key)
        {
            Delete(string.Format(s_callbackFormatString, m_appKey) + "/" + key);
        }

        /// <summary>
        /// Removes an existing presence callback from X-Stream.ly
        /// </summary>
        /// <param name="key"></param>
        private void RemovePresenceCallback(string key)
        {
            Delete(string.Format(s_presenceCallbackFormatString, m_appKey) + "/" + key);
        }

        /// <summary>
        /// Returns a collection of all currently active callbacks
        /// </summary>
        public IEnumerable<Callback> Callbacks
        {
            get
            {
                return GetWithWrapper<Callback>(string.Format(s_callbackFormatString, m_appKey));
            }
        }

        /// <summary>
        /// Returns a collection of all currently active callbacks
        /// </summary>
        private IEnumerable<Callback> PresenceCallbacks
        {
            get
            {
                return GetWithWrapper<Callback>(string.Format(s_presenceCallbackFormatString, m_appKey));
            }
        }
        #endregion Callbacks

        #region Security Tokens

        public string CreateToken(bool canRead, bool canWrite, String channel, String eventName, String source, bool isPrivate)
        {
            SecurityToken token = new SecurityToken
            {
                Channel = channel,
                Event = eventName,
                Source = source,
            };
            if (!canRead || !canWrite)
            {
                if (canRead)
                {
                    token.Action = "read";
                }
                else
                {
                    token.Action = "write";
                }
            }

            DataContractJsonSerializer ser = new DataContractJsonSerializer(typeof(SecurityToken));

            MemoryStream ms = new MemoryStream();
            ser.WriteObject(ms, token);
            string stringData = Encoding.Default.GetString(ms.ToArray());

            return PostData(string.Format(s_tokenFormatString, m_appKey), stringData);
        }

        public void DeleteSecurityToken(string token)
        {
            Delete(string.Format(s_tokenFormatString, m_appKey) + "/" + token);
        }

        public IEnumerable<SecurityToken> SecurityTokens
        {
            get
            {
                return GetSecurityTokens();
            }
        }

        /// <summary>
        /// Returns all the security tokens that are active for the account
        /// </summary>
        public IEnumerable<SecurityToken> GetSecurityTokens()
        {
            return GetSecurityTokens(null, null, null);
        }

        /// <summary>
        /// Returns all the security tokens that are active for the account that
        /// match the specified characteristics
        /// </summary>
        /// <param name="channel">If specified, function will only return tokens that have this value for their channel</param>
        /// <param name="eventName">If specified, function will only return tokens that have this value for their eventName</param>
        /// <param name="source">If specified, function will only return tokens that have this value for their source</param>
        public IEnumerable<SecurityToken> GetSecurityTokens(string channel, string eventName, string source)
        {
            string queryParams = "";

            if (!string.IsNullOrEmpty(channel))
            {
                queryParams = "channel=" + channel;
            }

            if (!string.IsNullOrEmpty(eventName))
            {
                queryParams = "&event=" + eventName;
            }

            if (!string.IsNullOrEmpty(source))
            {
                queryParams = "&source=" + source;
            }

            if (!string.IsNullOrEmpty(queryParams))
            {
                queryParams = "?" + queryParams;
            }

            return GetWithWrapper<SecurityToken>(string.Format(s_tokenFormatString, m_appKey) + queryParams);
        }

        #endregion Security Tokens

        #region Twitter
        public void SetTwitterStream(TwitterStream streamDefinition, TwitterPermissions permissions)
        {
            string nonce = Path.GetRandomFileName();
            string timeStamp = ((int)(DateTime.UtcNow - s_epoch).TotalSeconds).ToString();
            string track = HttpUtility.UrlEncode(streamDefinition.TrackParameter);

            string sigBase = "POST&https%3A%2F%2Fstream.twitter.com%2F1%2Fstatuses%2Ffilter.json&"
                             + "oauth_consumer_key%3D" + permissions.ConsumerKey
                             + "%26oauth_nonce%3D" + nonce
                             + "%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D" + timeStamp
                             + "%26oauth_token%3D" + permissions.AccessToken
                             + "%26oauth_version%3D1.0"
                             + "%26track%3D" + HttpUtility.UrlEncode(track);

            string key = permissions.ConsumerSecret + "&" + HttpUtility.UrlEncode(permissions.AccessTokenSecret);

            HMACSHA1 hmacsha1 = new HMACSHA1();
            hmacsha1.Key = Encoding.ASCII.GetBytes(key);

            byte[] dataBuffer = Encoding.ASCII.GetBytes(sigBase);
            byte[] hashBytes = hmacsha1.ComputeHash(dataBuffer);

            string signature = HttpUtility.UrlEncode(Convert.ToBase64String(hashBytes));

            string bodyParameters =
                string.Format(
                    "oauth_signature={0}&oauth_token={1}&track={2}&oauth_consumer_key={3}&oauth_signature_method=HMAC-SHA1&oauth_version=1.0&oauth_nonce={4}&oauth_timestamp={5}",
                    signature, permissions.AccessToken, track, permissions.ConsumerKey, nonce, timeStamp);
            streamDefinition.RequestData = bodyParameters;
            SetTwitterStream(streamDefinition);

        }

        /// <summary>
        /// Setup a new twitter stream.
        /// </summary>
        /// <param name="streamDefinition">The definition of what Twitter data to stream and to where</param>
        public string SetTwitterStream(TwitterStream streamDefinition)
        {
            if (string.IsNullOrEmpty(streamDefinition.RequestData))
            {
                throw new ArgumentNullException("streamDefinition.RequestData");
            }

            if (string.IsNullOrEmpty(streamDefinition.Channel))
            {
                throw new ArgumentNullException("streamDefinition.Channel");
            }

            DataContractJsonSerializer ser = new DataContractJsonSerializer(typeof(TwitterStream));

            MemoryStream ms = new MemoryStream();
            ser.WriteObject(ms, streamDefinition);
            string stringData = Encoding.Default.GetString(ms.ToArray());

            return PostData(string.Format(s_twitterStreamFormatString, m_appKey), stringData);
        }

        /// <summary>
        /// Stops a Twitter stream
        /// </summary>
        /// <param name="index">The key of the twitter stream</param>
        public void RemoveTwitterStream(int index)
        {
            Delete(string.Format(s_twitterStreamFormatString, m_appKey) + "/" + index);
        }

        /// <summary>
        /// Returns a collection of all currently active Twitter streams
        /// </summary>
        public IEnumerable<TwitterStream> TwitterStreams
        {
            get
            {
                return GetWithWrapper<TwitterStream>(string.Format(s_twitterStreamFormatString, m_appKey));
            }
        }
        #endregion Twitter

        #region Usage

        /// <summary>
        /// Gets the historical message usage by day
        /// </summary>
        public IEnumerable<UsageData> GetUsage()
        {
            return GetWithWrapper<UsageData>(string.Format(s_usageFormatString, m_appKey));
        }


        #endregion Usage

        #region GenricMethods

        private string PostData(string url, string data)
        {
            HttpWebRequest myRequest = GetAuthenticatedRequest(url);

            byte[] byteData = Encoding.ASCII.GetBytes(data);
            myRequest.Method = "POST";
            myRequest.ContentType = "application/json";
            myRequest.ContentLength = byteData.Length;
            Stream requestStream = myRequest.GetRequestStream();
            requestStream.Write(byteData, 0, byteData.Length);
            requestStream.Close();

            return ExecuteRequest(myRequest);
        }

        private void Delete(string url)
        {
            HttpWebRequest myRequest = GetAuthenticatedRequest(url);

            myRequest.Method = "DELETE";
            ExecuteRequest(myRequest);
        }

        private IEnumerable<T> GetWithWrapper<T>(string url)
        {
            Wrapper<T> wrapper = Get<Wrapper<T>>(url);
            if (null != wrapper.Sessions)
            {
                return wrapper.Sessions;
            }
            else if (null != wrapper.Items)
            {
                return wrapper.Items;
            }
            else
            {
                return new T[0];
            }
        }

        private T Get<T>(string url)
        {
            HttpWebRequest myRequest = GetAuthenticatedRequest(url);

            myRequest.Method = "GET";

            string data = ExecuteRequest(myRequest);

            DataContractJsonSerializer ser = new DataContractJsonSerializer(typeof(T));

            byte[] byteArray = Encoding.ASCII.GetBytes(data);
            using (MemoryStream stream = new MemoryStream(byteArray))
            {
                return(T)ser.ReadObject(stream);
            }
        }

        private string ExecuteRequest(WebRequest request)
        {
            HttpWebResponse response = null;
            try
            {
                response = (HttpWebResponse)request.GetResponse();
                String resposneText;
                using (StreamReader sr = new StreamReader(response.GetResponseStream()))
                {
                    resposneText = sr.ReadToEnd();
                }

                if (response.StatusCode == HttpStatusCode.Redirect || response.StatusCode == HttpStatusCode.Unauthorized)
                {
                    throw new AuthenticationException("Authentication to x-stream.ly failed, please check your credentials");
                }

                if (response.StatusCode != HttpStatusCode.OK && response.StatusCode != HttpStatusCode.Accepted)
                {
                    throw new Exception("Server did not accept your request: " + resposneText);
                }

                return resposneText;
            }
            catch (WebException wex)
            {
                response = (HttpWebResponse)wex.Response;
                using (TextReader textReader = new StreamReader(response.GetResponseStream()))
                {
                    throw new Exception("Problem setting callback: " + textReader.ReadToEnd());
                }
            }
            finally
            {
                if (null != response)
                {
                    response.Close();
                }
            }
        }

        #endregion GenricMethods

        private HttpWebRequest GetAuthenticatedRequest(string path)
        {
            HttpWebRequest myRequest = (HttpWebRequest)WebRequest.Create(s_xstreamlyHost + path);
            string authInfo = m_emailAddress + ":" + m_password;
            authInfo = Convert.ToBase64String(Encoding.Default.GetBytes(authInfo));
            myRequest.Headers["Authorization"] = "Basic " + authInfo;
            myRequest.AllowAutoRedirect = false;
            return myRequest;
        }

        [DataContract]
        private class Wrapper<T>
        {
            [DataMember(Name = "items")]
            public T[] Items { get; set; }

            [DataMember(Name = "sessions")]
            public T[] Sessions { get; set; }
        }
    }
}

