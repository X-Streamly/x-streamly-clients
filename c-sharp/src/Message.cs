using System.Runtime.Serialization;
using System.Web.Script.Serialization;

namespace XStreamly.Client
{
    [DataContract]
    public class Message
    {
        //we need to to it this way beucase we want to remap the property names
        //to lower case using DataContractJsonSerializer, but we want to do
        //dyname serialziation of contents using JavaScriptSerializer
        //we could have overriden the behavior of either to get the combinded
        //behavior but this seemed cleaner (it is by far the shortest)
        private static JavaScriptSerializer s_serializer = new JavaScriptSerializer();

        [DataMember(Name = "channel")]
        public string Channel { get; set; }

        [DataMember(Name = "event")]
        public string Event { get; set;}

        [DataMember(Name = "persisted")]
        public bool Persisted { get; set; }


        private object m_contents;
        public object Contents
        {
            get { return m_contents; }
            set
            {
                m_contents = value;
                ContentsAsString = s_serializer.Serialize(value);
            }
        }

        [DataMember(Name = "message")]
        private string ContentsAsString { get; set; }
    }
}
