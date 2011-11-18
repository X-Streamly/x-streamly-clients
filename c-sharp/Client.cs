using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Runtime.Serialization.Json;
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
        private static readonly string s_xstreamlyHost = "https://secure.x-stream.ly";
        private static readonly DateTime s_epoch = new DateTime(1970, 1, 1);
        private static readonly string s_subscriptionFormatString = "/api/v1.1/{0}/feeds/out/custom";
        private static readonly string s_twitterStreamFormatString = "/api/v1.1/{0}/feeds/in/twitter";

        private readonly string m_appKey;
        private readonly string m_emailAddress;
        private readonly string m_password;

        /// <summary>
        /// Create a new client object to interact with X-Stream.ly
        /// </summary>
        /// <param name="appKey">Your application key</param>
        /// <param name="emailAddress">The e-mail address you use to sign into X-Stream.ly with</param>
        /// <param name="password">The password you use to sign into X-Stream.ly with</param>
        public Client(string appKey,string emailAddress,string password)
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
        public void Send(string channel,string eventName, object data)
        {
            HttpWebRequest myRequest = GetAuthenticatedRequest("/api/v1.1/" + m_appKey + "/channels/" + channel + "/events/" + eventName);

            JavaScriptSerializer ser = new JavaScriptSerializer();
            string stringData = ser.Serialize(data);

            byte[] byteData = Encoding.ASCII.GetBytes(stringData);
            myRequest.Method = "POST";

            myRequest.ContentType = "application/json";
            myRequest.ContentLength = byteData.Length;
            Stream requestStream = myRequest.GetRequestStream();
            requestStream.Write(byteData, 0, byteData.Length);
            requestStream.Close();
            HttpWebResponse response = (HttpWebResponse)myRequest.GetResponse();

            using(StreamReader sr = new StreamReader(response.GetResponseStream()))
            {
                string responseData = sr.ReadToEnd();
                Console.WriteLine(responseData);
            }

            response.Close();
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
        public void SetCallback(Callback callback)
        {
            HttpWebRequest myRequest = GetAuthenticatedRequest(string.Format(s_subscriptionFormatString, m_appKey));

            DataContractJsonSerializer ser = new DataContractJsonSerializer(typeof(Callback));


            MemoryStream ms = new MemoryStream();
            ser.WriteObject(ms, callback);
            string stringData = Encoding.Default.GetString(ms.ToArray());

            byte[] byteData = Encoding.ASCII.GetBytes(stringData);
            myRequest.Method = "POST";
            myRequest.ContentType = "application/json";
            myRequest.ContentLength = byteData.Length;
            Stream requestStream = myRequest.GetRequestStream();
            requestStream.Write(byteData, 0, byteData.Length);
            requestStream.Close();
            HttpWebResponse response=null;
            try
            {
                response = (HttpWebResponse)myRequest.GetResponse();
            }
            catch (WebException wex)
            {
                response = (HttpWebResponse)wex.Response;
                using (TextReader textReader = new StreamReader(response.GetResponseStream()))
                {
                    throw new Exception("Problem setting callback: "+textReader.ReadToEnd());
                }

            }
            finally
            {
                if(null!=response)
                {
                    response.Close();
                }
            }

        }

        /// <summary>
        /// Removes an existing callback from X-Stream.ly
        /// </summary>
        /// <param name="index"></param>
        public void RemoveCallback(string index)
        {
            HttpWebRequest myRequest = GetAuthenticatedRequest(string.Format(s_subscriptionFormatString,m_appKey)+"/"+index);

            myRequest.Method = "DELETE";
            HttpWebResponse response = (HttpWebResponse)myRequest.GetResponse();
            response.Close();
        }

        /// <summary>
        /// Returns a collection of all currently active callbacks
        /// </summary>
        public IEnumerable<Callback> Callbacks
        {
            get
            {
                HttpWebRequest myRequest = GetAuthenticatedRequest(string.Format(s_subscriptionFormatString, m_appKey));

                myRequest.Method = "GET";
                HttpWebResponse response = (HttpWebResponse)myRequest.GetResponse();
                CallbackWrapper callbacks;

                DataContractJsonSerializer ser = new DataContractJsonSerializer(typeof(CallbackWrapper));

                callbacks = (CallbackWrapper)ser.ReadObject(response.GetResponseStream());
                
                response.Close();

                return callbacks.Items;
            }
        }
        #endregion Callbacks

        #region Twitter
        public void SetTwitterStream(TwitterStream streamDefinition,TwitterPermissions permissions)
        {
            string nonce = Path.GetRandomFileName();
            string timeStamp = ((int)(DateTime.UtcNow - s_epoch).TotalSeconds).ToString();
            string track = HttpUtility.UrlEncode(streamDefinition.TrackParameter);

            string sigBase = "POST&https%3A%2F%2Fstream.twitter.com%2F1%2Fstatuses%2Ffilter.json&"
                             + "oauth_consumer_key%3D" + permissions.ConsumerKey
                             + "%26oauth_nonce%3D"+nonce
                             + "%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D" + timeStamp
                             + "%26oauth_token%3D" + permissions.AccessToken
                             + "%26oauth_version%3D1.0"
                             + "%26track%3D" +track;

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
        public void SetTwitterStream(TwitterStream streamDefinition)
        {
            if(string.IsNullOrEmpty(streamDefinition.RequestData))
            {
                throw new ArgumentNullException("streamDefinition.RequestData");
            }

            if (string.IsNullOrEmpty(streamDefinition.Channel))
            {
                throw new ArgumentNullException("streamDefinition.Channel");
            }

            HttpWebRequest myRequest = GetAuthenticatedRequest(string.Format(s_twitterStreamFormatString, m_appKey));

            DataContractJsonSerializer ser = new DataContractJsonSerializer(typeof(TwitterStream));


            MemoryStream ms = new MemoryStream();
            ser.WriteObject(ms, streamDefinition);
            string stringData = Encoding.Default.GetString(ms.ToArray());

            byte[] byteData = Encoding.ASCII.GetBytes(stringData);
            myRequest.Method = "POST";
            myRequest.ContentType = "application/json";
            myRequest.ContentLength = byteData.Length;
            Stream requestStream = myRequest.GetRequestStream();
            requestStream.Write(byteData, 0, byteData.Length);
            requestStream.Close();
            HttpWebResponse response = null;
            try
            {
                response = (HttpWebResponse)myRequest.GetResponse();
            }
            catch (WebException wex)
            {
                response = (HttpWebResponse)wex.Response;
                using (TextReader textReader = new StreamReader(response.GetResponseStream()))
                {
                    throw new Exception("Problem setting twitter stream: " + textReader.ReadToEnd());
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

        /// <summary>
        /// Stops a Twitter stream
        /// </summary>
        /// <param name="index">The key of the twitter stream</param>
        public void RemoveTwitterStream(int index)
        {
            HttpWebRequest myRequest = GetAuthenticatedRequest(string.Format(s_twitterStreamFormatString, m_appKey)+"/" + index);

            myRequest.Method = "DELETE";
            HttpWebResponse response = (HttpWebResponse)myRequest.GetResponse();
            response.Close();
        }

        /// <summary>
        /// Returns a collection of all currently active Twitter streams
        /// </summary>
        public IEnumerable<TwitterStream> TwitterStreams
        {
            get
            {
                HttpWebRequest myRequest = GetAuthenticatedRequest(string.Format(s_twitterStreamFormatString, m_appKey));

                myRequest.Method = "GET";
                try
                {
                    HttpWebResponse response = (HttpWebResponse)myRequest.GetResponse();
                    TwitterStreamWrapper callbacks;

                    DataContractJsonSerializer ser = new DataContractJsonSerializer(typeof(TwitterStreamWrapper));



                    callbacks = (TwitterStreamWrapper)ser.ReadObject(response.GetResponseStream());

                    response.Close();

                    return callbacks.Items;
                }
                catch (WebException wex)
                {
                    string errorMessage ="";
                    using(StreamReader sr = new StreamReader(wex.Response.GetResponseStream()))
                    {
                        errorMessage = sr.ReadToEnd();
                    }
                    throw new WebException(errorMessage,wex);
                }

            }
        }
        #endregion Twitter

        private HttpWebRequest GetAuthenticatedRequest(string path)
        {
            HttpWebRequest myRequest = (HttpWebRequest) WebRequest.Create(s_xstreamlyHost + path);
            string authInfo = m_emailAddress + ":" + m_password;
            authInfo = Convert.ToBase64String(Encoding.Default.GetBytes(authInfo));
            myRequest.Headers["Authorization"] = "Basic " + authInfo;
            return myRequest;
        }
    }
}

