using System;
using System.Runtime.Serialization;

namespace XStreamly.Client
{
    [DataContract]
    public class UsageDataPoint
    {
        [DataMember(Name = "time")]
        public DateTime  Date { get; set;}

        [DataMember(Name = "amount")]
        public int Amount { get; set;}

        public override string ToString()
        {
            return Date + " " + Amount;
        }
    }
}
