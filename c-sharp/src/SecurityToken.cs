using System;
using System.Runtime.Serialization;

namespace XStreamly.Client
{
    [DataContract]
    public class SecurityToken
    {
        [DataMember(Name = "key")]
        public String Key { get; set;}

        [DataMember(Name = "lifetime")]
        public int Lifetime { get; set;}

        [DataMember(Name = "secure")]
        public bool Secure { get; set; }

        [DataMember(Name = "email")]
        public string Email { get; set; }

        [DataMember(Name = "action")]
        public string Action { get; set; }

        [DataMember(Name = "channel")]
        public string Channel { get; set; }

        [DataMember(Name = "event")]
        public string Event { get; set; }

        [DataMember(Name = "source")]
        public string Source { get; set; }

        public override string ToString()
        {
            return "SecurityToken: " + Key; 
        }
    }
}
