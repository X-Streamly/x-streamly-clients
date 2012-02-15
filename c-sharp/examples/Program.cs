using System;
using XStreamly.Client;

namespace XStreamlyExamples
{
    class Program
    {
        static void Main()
        {
            try
            {
                Client client = new Client("appkey", "email", "password");

                client.Send("MyChannel", "MyEvent", new { Name = "Brian", Age = 28 });


                foreach(UsageData point in client.GetUsage())
                {
                    Console.WriteLine("Usage: "+point);
                }


                String callbackKey = client.SetCallback(new Callback
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

                Console.WriteLine("deleting callbackKey: " + callbackKey);
                client.RemoveCallback(callbackKey);

                
                String securityToken = client.CreateToken(true, true, "MyChannel", null, null, false);

                Console.WriteLine("new security token: " + securityToken);
                
                foreach (SecurityToken token in client.SecurityTokens)
                {
                    Console.WriteLine("all:"+token);
                }

                foreach (SecurityToken token in client.GetSecurityTokens(null, "MyEvent",null))
                {
                    Console.WriteLine("filtered:" + token);
                }

                client.DeleteSecurityToken(securityToken);
            }
            catch (Exception ex)
            {
                Console.WriteLine("Unhanded exception: "+ex);
            }

        }
    }
}