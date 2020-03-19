#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#ifndef APSSID
#define APSSID "ESPap"
#define APPSK "12345678"
#endif

const char *ssid = APSSID;
const char *password = APPSK;

WiFiServer server(5000);

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  //Serial.println("Configuring access point...");
  WiFi.softAP(ssid, password);
  IPAddress myIP = WiFi.softAPIP();
  //Serial.print("AP IP address: ");
  //Serial.println(myIP);
  server.begin();
}

void loop() {
  WiFiClient client = server.available();
  if(client){
    //Serial.println("\n[Client connected]");
    Serial.print('2');
    while (client.connected())
    {
      if (client.available())
      {
        char data = static_cast<char>(client.read());
        Serial.print(data);
      }
      if(Serial.available()){
        char data = static_cast<char>(Serial.read());
        client.println(data);
      }
    }
  }
}
