using System;
using XStreamly.Client;

namespace XStreamlyExamples
{
    class Program
    {
        static void Main(string[] args)
        {
            try
            {
                Client client = new Client("10bc1643-c9f5-4210-9814-cae3203af316", "bwillard@x-stream.ly", "qwer1234");

                /*client.Send("MyChannel", "MyEvent", new { Name = "Brian", Age = 28 });

                client.SendBatch(new[]{
                    new Message
                       {
                           Channel = "Channel1",
                           Event = "MyEvent",
                           Persisted = false,
                           Contents = new {Data="goes here"}
                       },
                       new Message
                       {
                           Channel = "Channel2",
                           Event = "MyEvent",
                           Contents = "this could be a string too"
                       },
                });

                foreach(UsageDataPoint point in client.GetConnectionUsage())
                {
                    Console.WriteLine("Connection: "+point);
                }

                foreach (UsageDataPoint point in client.GetMessageUsage())
                {
                    Console.WriteLine("Message: " + point);
                }*/

                /*String callbackKey = client.SetCallback(new Callback
                {
                    Channel = "MyOtherChannel",
                    Endpoint = "http://my.end.point",
                    EventName = "EventName",
                    Secret = "No one should know this"
                });

                foreach (Callback c in client.Callbacks)
                {
                    Console.WriteLine(c);
                }

                client.RemoveCallback(callbackKey);*/

                String presenceCallbackKey = client.SetPresenceCallback(new Callback
                {
                    Channel = "MyOtherChannel",
                    Endpoint = "http://my.end.point",
                    EventName = "EventName",
                    Secret = "No one should know this"
                });

                foreach (Callback c in client.PresenceCallbacks)
                {
                    Console.WriteLine(c);
                }

                client.RemovePresenceCallback(presenceCallbackKey);


                /*String securityToken = client.CreateToken(true, true, "MyChannel", null, null, false);

                Console.WriteLine("new security token: " + securityToken);
                
                foreach (SecurityToken token in client.SecurityTokens)
                {
                    Console.WriteLine("all:"+token);
                }

                foreach (SecurityToken token in client.GetSecurityTokens(null, "MyEvent",null))
                {
                    Console.WriteLine("filtered:" + token);
                }*/

                //client.DeleteSecurityToken(securityToken);
            }
            catch (Exception ex)
            {
                Console.WriteLine("Unhanded exception: "+ex);
            }

        }
    }
}