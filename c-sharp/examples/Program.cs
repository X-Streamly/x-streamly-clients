using System;
using XStreamly.Client;

namespace XStreamlyExamples
{
    class Program
    {
        static void Main(string[] args)
        {
            Client client = new Client("app key","email","password");

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
        }
    }
}