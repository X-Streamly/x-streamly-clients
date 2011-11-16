using System.Runtime.Serialization;

namespace XStreamly.Client
{
    [DataContract]
    public class Callback
    {
        [DataMember(Name = "channel")]
        public string Channel { get; set;}

        [DataMember(Name = "endpoint")]
        public string Endpoint { get; set;}

        [DataMember(Name = "secret")]
        public string Secret { get; set;}

        [DataMember(Name = "event")]
        public string EventName { get; set; }

        [DataMember(Name = "key")]
        public string Key { get; set; }
    }

    [DataContract]
    public class CallbackWrapper
    {
        [DataMember(Name="items")]
        public Callback[] Items { get; set; }
    }
}

