using System;
using XStreamly.Client;

namespace XStreamlyExamples
{
    class Program
    {
        static void Main(string[] args)
        {
            Client client = new Client("app key","e-mail address","password");

            client.Send("MyChannel","MyEvent",new {Name="Brian", Age=28});

            String callbackKey = client.SetCallback(new Callback
                                   {
                                       Channel ="MyOtherChannel",
                                       Endpoint = "http://my.end.point",
                                       EventName = "EventName",
                                       Secret = "No one should know this"
                                   });

            foreach(Callback c in client.Callbacks)
            {
                Console.WriteLine(c);
            }

            client.RemoveCallback(callbackKey);

            String securityToken = client.CreateToken(true, true, "MyChannel", null, null, false);

            Console.WriteLine("new security token: "+securityToken);

            foreach (SecurityToken token in client.SecurityTokens)
            {
                Console.WriteLine(token);
            }

            client.DeleteSecurityToken(securityToken);
        }
    }
}