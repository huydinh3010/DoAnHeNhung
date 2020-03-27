#include <ESP8266WiFi.h>
#include <FirebaseArduino.h>
#define FIREBASE_HOST "irremote-4f614.firebaseio.com"
#define FIREBASE_AUTH "KPv43AaJnuulZW8YvMNwKIZkvZj9wOie365KjzPg"
#define WIFI_SSID "Huy Dinh"
#define WIFI_PASSWORD "0123456789"

void readSerial(){
  if(Serial.available() > 0){
    char cmd[6];
    memset(cmd, 0, sizeof(cmd));
    Serial.readBytes(cmd, 5);
    if(strcmp(cmd,"mode:") == 0){
      byte b = Serial.read();
      Firebase.setFloat("device/mode", b - '0');
    } else if(strcmp(cmd,"code:") == 0){
      String code = Serial.readStringUntil('\n');
      Firebase.setString("device/ircode", code);
    }
  }
}

void setup() {
  Serial.begin(115200);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  //Serial.print("connecting");
  while (WiFi.status() != WL_CONNECTED) {
    //Serial.print(".");
    delay(500);
  }
  //Serial.println();
  //Serial.print("connected: ");
  //Serial.println(WiFi.localIP());
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
  Firebase.stream("/device");
}
void loop() {
  readSerial();
  if (Firebase.available()) {
     FirebaseObject event = Firebase.readEvent();
     String eventType = event.getString("type");
     eventType.toLowerCase();
     //Serial.print("event: ");
     //Serial.println(eventType);
     if(eventType == "put"){
      String path = event.getString("path");
      if(path == "/"){
         JsonVariant payload = event.getJsonVariant("data");
         int mode = payload["mode"];
         String code = payload["scode"];
         Serial.print("mode:");
         Serial.println(mode);
         Serial.print("code:");
         Serial.println(code);
      }
      else if(path == "/mode"){
        int mode = event.getInt("data");
        Serial.print("mode:");
        Serial.println(mode);
      }
      else if(path == "/scode"){
        String code = event.getString("data");
        Serial.print("code:");
        Serial.print(code);
      }
     }
  }   
}
