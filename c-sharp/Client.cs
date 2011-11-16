using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Runtime.Serialization.Json;
using System.Text;
using System.Web.Script.Serialization;

namespace XStreamly.Client
{
    public class Client
    {
        private static readonly string s_xstreamlyHost = "https://secure.x-stream.ly";

        private readonly string m_appKey;
        private readonly string m_emailAddress;
        private readonly string m_password;

        public Client(string appKey,string emailAddress,string password)
        {
            m_appKey = appKey;
            m_emailAddress = emailAddress;
            m_password = password;
        }

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

        public void SetCallback(Callback callback)
        {
            HttpWebRequest myRequest = GetAuthenticatedRequest("/api/v1.1/" + m_appKey + "/feeds/out/custom");

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

        public void RemoveCallback(string index)
        {
            HttpWebRequest myRequest = GetAuthenticatedRequest("/api/v1.1/" + m_appKey + "/feeds/out/custom/"+index);

            myRequest.Method = "DELETE";
            HttpWebResponse response = (HttpWebResponse)myRequest.GetResponse();
            response.Close();
        }

        public IEnumerable<Callback> Callbacks
        {
            get
            {
                HttpWebRequest myRequest = GetAuthenticatedRequest("/api/v1.1/" + m_appKey + "/feeds/out/custom");

                myRequest.Method = "GET";
                HttpWebResponse response = (HttpWebResponse)myRequest.GetResponse();
                CallbackWrapper callbacks;

                DataContractJsonSerializer ser = new DataContractJsonSerializer(typeof(CallbackWrapper));

                callbacks = (CallbackWrapper)ser.ReadObject(response.GetResponseStream());
                
                response.Close();

                return callbacks.Items;
            }
        }

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

