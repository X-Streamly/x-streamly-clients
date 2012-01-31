using System;
using System.Linq;
using System.Runtime.Serialization;

namespace XStreamly.Client
{
    [DataContract]
    public class UsageData
    {
        [DataMember(Name = "user")]
        public string  User { get; set;}

        [DataMember(Name = "data")]
        private object[][] m_rawData { 
            get
            {
                throw new NotImplementedException();
            }
            set
            {
                Data = value.Select(p => new UsageDataPoint
                                      {
                                          Date = DateTime.Parse(p[0].ToString()),
                                          Amount = (int) p[1]
                                      }).ToArray();
            }
        }

        public UsageDataPoint[] Data {get;private set;}
    }
}
