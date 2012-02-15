using System;
using System.Runtime.Serialization;

namespace XStreamly.Client
{
    [DataContract]
    public class UsageData
    {
        [DataMember(Name = "maxConcurentConnections")]
        public int MaxConcurentConnections { get; set; }

        [DataMember(Name = "totalConnections")]
        public int TotalConnections { get; set; }

        [DataMember(Name = "messagesSent")]
        public int MessagesSent { get; set; }

        public DateTime Date { get; private set; }

        private string m_myDate;
        [DataMember(Name = "date")]
        private string MyDate
        {
            get
            {
                return m_myDate;
            }

            set
            {
                m_myDate = value;
                Date = DateTime.Parse(value);
            }
        }

        public override string ToString()
        {
            return string.Format("{0}: messages: {1}, total connection: {2}, max concurent conections:{3}", Date,
                                 MessagesSent, TotalConnections, MaxConcurentConnections);
        }
    }
}
