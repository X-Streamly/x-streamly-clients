using System.Runtime.Serialization;
using System.Text.RegularExpressions;

namespace XStreamly.Client
{
    [DataContract]
    public class TwitterStream
    {
        private static readonly Regex s_trackParamRegex = new Regex("track=(.*?)&");

        private string m_trackParameter;

        [DataMember(Name = "channel")]
        public string Channel { get; set; }

        [DataMember(Name = "event")]
        public string Event { get; set; }

        [DataMember(Name = "requestData")]
        public string RequestData { get; set; }

        [DataMember(Name = "status")]
        public string Status { get; set; }

        [DataMember(Name = "key")]
        public int Key { get; set; }

        public string TrackParameter
        {
            get
            {
                if (string.IsNullOrEmpty(m_trackParameter))
                {
                    if(!string.IsNullOrEmpty(RequestData))
                    {
                        Match m = s_trackParamRegex.Match(RequestData);
                        if(m.Success)
                        {
                            return m.Groups[1].Value;
                        }
                    }
                }

                return m_trackParameter;
                
            }
            set { m_trackParameter = value; }
        }
    }
}

