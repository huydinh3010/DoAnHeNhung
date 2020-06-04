#include <ESP8266WiFi.h>
#include <FirebaseArduino.h>
#include <IRsend.h>
#include <NTPClient.h>
#include <Ticker.h>
#include <WiFiUdp.h>
#include <ESP8266WebServer.h>
#include <WiFiClient.h>
#include <EEPROM.h>


#define FIREBASE_HOST "irremote-4f614.firebaseio.com"
#define FIREBASE_AUTH "KPv43AaJnuulZW8YvMNwKIZkvZj9wOie365KjzPg"


#define MAXLEN 500
#define RXPINIR 5
#define IRLEDPIN 13
#define LEDPIN 15
#define WFLED 4
#define FLASHPIN 0

String WIFI_SSID = "";
String WIFI_PASSWORD = "";

bool sendIR(String);

WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "asia.pool.ntp.org", 0, 3600000L);
ESP8266WebServer server(80);

volatile unsigned int last = 0;
volatile unsigned int irBuffer[MAXLEN];
volatile unsigned int len = 0;
volatile int val = 0;
String data = "";
uint16_t rawLen = 0;
byte mode = 0;
bool hasWifiInfo = false;
bool flashPressed = false;

class SenderSchedule{
  public:
    SenderSchedule(){};
    ~SenderSchedule(){};
    int pos;
    unsigned int duration;
    String data;
    int loop;
    bool st = false;
    Ticker ticker;
    
    void set(){
      ticker.once(duration, std::bind(&SenderSchedule::doSend, this));  
    }
    
    void stop(){
      ticker.detach();
    }

    void next(){
      if(loop != 0){
        Firebase.setString("device/sstatus/s" + String(pos), "DONE");
        delay(200);
        Firebase.setInt("device/sduration/s" + String(pos), duration); 
        Firebase.setString("device/sstatus/s" + String(pos), "SET");
      } else {
        Firebase.setString("device/sstatus/s" + String(pos), "DONE");
      }
      st = false;
    }
    
    void doSend(){
      duration = (unsigned int)timeClient.getEpochTime() + loop * 3600;
      sendIR(this->data);
      st = true;
    };
} schedule[10];

void ICACHE_RAM_ATTR flash_Interrupt_Handler() { // hàm xử lý ngắt
  flashPressed = true;
}

void ICACHE_RAM_ATTR rxIR_Interrupt_Handler() { // hàm xử lý ngắt
  val = digitalRead(RXPINIR);
  if (len > MAXLEN) return;
  last = micros();
  irBuffer[len] = last;
  if(len > 0) irBuffer[len-1] = (irBuffer[len] - irBuffer[len-1] + 25) / 50; // làm tròn theo bội của 50
  len++;
}

bool ir_available(){ // trả về true nếu đã thu được tín hiệu hồng ngoại
  delay(10);
  if(len == 0 || val == 0) return false;
  unsigned long delta_t = micros() - last;  
  if(delta_t < 200000L) return false;
  return true;
}

bool validateIRcode(String data){
  if(data.length() < 2) return false;
  byte b0;
  int i = 0;
  byte b1 = (byte)data[i++] - 60;
  byte b2 = (byte)data[i++] - 60;
  int len = b1 * 64 + b2;
  if(len < 0) return false;
  while(i < data.length()){
    b0 = (byte)data[i++];
    if(b0 == 33){
      if(i > data.length() - 2) return false;
      i++;
      b0 = (byte)data[i++];
      if(b0 == 34){
        if(i > data.length() - 2) return false;
        b1 = (byte)data[i++] - 60;
        b2 = (byte)data[i++] - 60;
        if(b1 > 32) return false;
      }
    } else if(b0 == 34){
      if(i > data.length() - 2) return false;
      b1 = (byte)data[i++] - 60;
      b2 = (byte)data[i++] - 60;
      if(b1 > 32) return false;
    }
  }
  return true;
}

bool readIR(){
    if(len < 20){
      len = 0;
      return false;
    }
    detachInterrupt(digitalPinToInterrupt(RXPINIR));
    rawLen = len - 1;
    len = 0; // đặt lại biến chạy
    for(int i = 0; i < rawLen; i++){
      if(irBuffer[i] > 2100){
        attachInterrupt(digitalPinToInterrupt(RXPINIR), rxIR_Interrupt_Handler, CHANGE);
        return false;
      }
    }
    data = "";
    byte b1 = (byte)((rawLen & 0xFFC0) >> 6); // độ dài chuỗi
    byte b2 = (byte)(rawLen & 0x003F);
    data += (char)(b1 + 60);
    data += (char)(b2 + 60);
    int i = 0;
    unsigned int t = 0;
    byte c = 0;
    while(i < rawLen){ // nén giá trị ở vị trí chẵn
      t = irBuffer[i];
      do{
        i += 2;
        c++;
      } while(i < rawLen && irBuffer[i] == t && c < 85); 
      if(c > 1){ // c là số lượng giá trị giống nhau, nếu có nhiều hơn 1
        data += (char)33;
        data += (char)(c + 40);
        if(t > 63){
          b1 = (byte)((t & 0xFFC0) >> 6);
          b2 = (byte)(t & 0x003F);
          data += (char)34;
          data += (char)(b1 + 60);
          data += (char)(b2 + 60);
        } else{
          data += (char)(t + 60);
        }
      } else{ // chỉ có 1 giá trị
        if(t > 63){
          b1 = (byte)((t & 0xFFC0) >> 6);
          b2 = (byte)(t & 0x003F);
          data += (char)34;
          data += (char)(b1 + 60);
          data += (char)(b2 + 60);
        } else{
          data += (char)(t + 60);
        }
      }
      c = 0;
    }
    data += (char)35; // kí tự phân cách '#'
    i = 1;
    while(i < rawLen){ // nén giá trị ở vị trí lẻ
      t = irBuffer[i];
      do{
        i += 2;
        c++;
      } while(i < rawLen && irBuffer[i] == t && c < 85);
      if(c > 1){
        data += (char)33;
        data += (char)(c + 40);
        if(t > 63){
          b1 = (byte)((t & 0xFFC0) >> 6);
          b2 = (byte)(t & 0x003F);
          data += (char)34;
          data += (char)(b1 + 60);
          data += (char)(b2 + 60);
        } else{
          data += (char)(t + 60);
        }
      } else{
        if(t > 63){
          b1 = (byte)((t & 0xFFC0) >> 6);
          b2 = (byte)(t & 0x003F);
          data += (char)34;
          data += (char)(b1 + 60);
          data += (char)(b2 + 60);
        } else{
          data += (char)(t + 60);
        }
      }
      c = 0;
    }
    Serial.println(data);
    mode = 0;
    return true;
}

bool sendIR(String data){ // phát tín hiệu hồng ngoại
  if(data.length() < 2 || !validateIRcode(data)) return false;
  detachInterrupt(digitalPinToInterrupt(RXPINIR));
  byte b0;
  int i = 0;
  byte b1 = (byte)data[i++] - 60;
  byte b2 = (byte)data[i++] - 60;
  rawLen = b1 * 64 + b2;
  int j = 0;
  byte c = 0;
  unsigned int t = 0;
  while(i < data.length()){ // giải nén từ chuỗi để lấy giá trị
    b0 = (byte)data[i++];
    if(b0 == 35){
      j = 1;
    } else if(b0 == 33){
      c = (byte)data[i++] - 40;
      b0 = (byte)data[i++];
      if(b0 == 34){
        b1 = (byte)data[i++] - 60;
        b2 = (byte)data[i++] - 60;
        t = b1 * 64 + b2;
      } else{
        t = b0 - 60;
      }
      for(c; c > 0; c--){
        irBuffer[j] = t;
        j += 2;
      }
    } else if(b0 == 34){
      b1 = (byte)data[i++] - 60;
      b2 = (byte)data[i++] - 60;
      t = b1 * 64 + b2;
      irBuffer[j] = t;
      j += 2;
    } else {
      irBuffer[j] = b0 - 60;
      j += 2;
    }
  }
  
  // phát tín hiệu hồng ngoại
  IRsend irsend(IRLEDPIN);
  irsend.enableIROut(38);
  for (int i = 0; i < rawLen; i++){
    if(i & 1){
      irsend.space(irBuffer[i] * 50);
    } else{
      irsend.mark(irBuffer[i] * 50);
    }
  }
  digitalWrite(IRLEDPIN, LOW);
  mode = 0;
  return true;
}

void doCmd(String cmd){
  if(cmd == "READ"){
    Firebase.setString("device/cmd", "WAIT");
    mode = 1;
    len = 0;
    digitalWrite(LEDPIN, HIGH);
    attachInterrupt(digitalPinToInterrupt(RXPINIR), rxIR_Interrupt_Handler, CHANGE);
  } else if(cmd == "SEND"){
    data = Firebase.getString("device/scode");
    Firebase.setString("device/cmd", "WAIT");
    mode = 2;
    digitalWrite(LEDPIN, LOW);
    if(sendIR(data)){
      Firebase.setString("device/cmd", "SEND_OK");
    }else {
      Firebase.setString("device/cmd", "SEND_ERROR");
    }
    delay(500);
  } else if(cmd != "WAIT"){
    mode = 0;
    digitalWrite(LEDPIN, LOW);
    detachInterrupt(digitalPinToInterrupt(RXPINIR));
  }
}

bool lockupWifiInfo(){
  EEPROM.begin(512);
  int w_a = EEPROM.read(0);
  if(w_a){
    int i = 1;
    int _b = -1;
    WIFI_SSID = "";
    WIFI_PASSWORD = "";
    while(_b != 0 && i < 512){
      _b = EEPROM.read(i++);
      if(_b != 0) WIFI_SSID += (char)_b;
    }
    _b = -1;
    while(_b != 0 && i < 512){
      _b = EEPROM.read(i++);
      if(_b != 0) WIFI_PASSWORD += (char)_b;
    }
    EEPROM.end();
    return true;
  } else{
    return false;
  }
}

void writeWifiInfoToEEPROM(String wf_name, String wf_pass){
  EEPROM.begin(512);
  EEPROM.write(0, 1);
  delay(5);
  int e_in = 1;
  for(int i = 0; i < wf_name.length(); i++){
    EEPROM.write(e_in++, (int)wf_name[i]);
    delay(5);
  }
  EEPROM.write(e_in++, 0);
  delay(5);
  for(int i = 0; i < wf_pass.length(); i++){
    EEPROM.write(e_in++, (int)wf_pass[i]);
    delay(5);
  }
  EEPROM.write(e_in++, 0);
  delay(100);
  EEPROM.commit();
  EEPROM.end();
}

void handleWSRoot(){
  if(server.hasArg("name") && server.hasArg("pass")){
    String wf_name = server.arg("name");
    String wf_pass = server.arg("pass");
    if(wf_name.length() != 0 && wf_pass.length() >= 8 && (wf_name != WIFI_SSID || wf_pass != WIFI_PASSWORD)){
      WIFI_SSID = wf_name;
      WIFI_PASSWORD = wf_pass;
      Serial.println("WifiInfo:");
      Serial.println(WIFI_SSID);
      Serial.println(WIFI_PASSWORD);
      writeWifiInfoToEEPROM(WIFI_SSID, WIFI_PASSWORD);
    }
  }
  String html_content = "";
  html_content += "<html>";
  html_content += "<head>";
  html_content += "<meta charset='UTF-8'>";
  html_content += "<h2>Enter wifi information to connect the device</h2>";
  html_content += "</head>";
  html_content += "<body>";
  html_content += "<form method='post'>WiFiName: <input type='text' name='name' value='" + WIFI_SSID + "' pattern='.{1,}' required title='1 characters minimum'/> <br />WiFiPass: <input type='text' name='pass' placeholder='Minimum of 8 characters' value='" + WIFI_PASSWORD + "' pattern='.{8,}' required title='8 characters minimum'/> <br /><input class='button' type='submit' value='Save'/></form>";
  html_content += "</body>";
  html_content += "</html>";
  server.send(200, "text/html", html_content);
}

void setup() {
  pinMode(IRLEDPIN, OUTPUT);
  pinMode(WFLED, OUTPUT);
  pinMode(RXPINIR, INPUT);
  pinMode(LEDPIN, OUTPUT);
  pinMode(FLASHPIN, INPUT_PULLUP);
  digitalWrite(LEDPIN, LOW);
  digitalWrite(IRLEDPIN, LOW);
  digitalWrite(WFLED, HIGH);
  
  
  int wf_led = HIGH;
  Serial.begin(9600);
  //
  hasWifiInfo = lockupWifiInfo();
  if(hasWifiInfo){
    Serial.println("WifiInfo:");
    Serial.println(WIFI_SSID);
    Serial.println(WIFI_PASSWORD);
    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    attachInterrupt(digitalPinToInterrupt(FLASHPIN), flash_Interrupt_Handler, FALLING);
    while (WiFi.status() != WL_CONNECTED) {
      delay(500);
      wf_led = !wf_led;
      digitalWrite(WFLED, wf_led);
      if(flashPressed) clearEEPROM();
    }
    Serial.println("Wifi Connected!");

    timeClient.begin();
    for (int i = 0; i < 10; i++) schedule[i].pos = i;
    digitalWrite(WFLED, HIGH);
    Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
    Firebase.stream("/device");
    delay(500);
    timeClient.update();
    Firebase.setString("device/cmd", "RESET");
    unsigned long _ct = timeClient.getEpochTime();
    for(int i = 0; i < 10; i++){
      String _s = Firebase.getString("device/schedule/s" + String(i));
      int f = Firebase.getInt("device/sduration/s" + String(i));
      int lp = Firebase.getInt("device/sloop/s" + String(i));
      String ss = Firebase.getString("device/sstatus/s" + String(i));
      if(ss == "DONE" || ss == "" || ss == "OK" || ss == "CANCEL"){
       schedule[i].stop();
       continue;
      }     
      if(f < _ct){
       Firebase.setString("device/sstatus/s" + String(i), "TIME_ERROR");
       continue; 
      }
      if(!validateIRcode(_s)){
       Firebase.setString("device/sstatus/s" + String(i), "INVALID_IRCODE");
       continue;
      }
      if(lp < 0 || lp > 24){
       Firebase.setString("device/sstatus/s" + String(i), "INVALID_LOOP");
       continue;
      }
      schedule[i].data = _s;
      schedule[i].duration = (unsigned long)f - _ct;
      schedule[i].set();
      Firebase.setString("device/sstatus/s" + String(i), "WAIT");
    }
    digitalWrite(WFLED, LOW);
    flashPressed = false;
  } else{
    Serial.println("There is no WifiInfo!");
    Serial.print("Configuring access point...");
    WiFi.mode(WIFI_AP);
    WiFi.softAP("ESP8266", "12345678");
    delay(100);
    IPAddress ip(192, 168, 1, 1);
    IPAddress n_mask(255, 255, 255, 0);
    WiFi.softAPConfig(ip, ip, n_mask);
    IPAddress myIP = WiFi.softAPIP();
    Serial.print("AP IP address: ");
    Serial.println(myIP);
    server.on("/", handleWSRoot);
    server.begin();
    Serial.println("HTTP server started");
    flashPressed = false;
    attachInterrupt(digitalPinToInterrupt(FLASHPIN), flash_Interrupt_Handler, FALLING);
  }
  //
}

void mainOp(){
  timeClient.update();
  for(int i = 0;i < 10; i++){
    if(schedule[i].st) schedule[i].next();
  }
  if(mode == 1 && ir_available() && readIR()){
    Firebase.setString("device/ircode", data);
    Firebase.setString("device/cmd", "READ_OK");
  }
  if (Firebase.available()) { // bắt sự kiện từ firebase
     FirebaseObject event = Firebase.readEvent();
     String eventType = event.getString("type");
     eventType.toLowerCase();
     if(eventType == "put"){
        String path = event.getString("path");
        Serial.println(path);
        if(path == "/cmd"){
           String cmd = Firebase.getString("device/cmd");
           doCmd(cmd);
        } else {
          if(path.substring(0, 10) == "/sduration" || path.substring(0,8) == "/sstatus"){
            int pos = -1;
            if(path.substring(0, 10) == "/sduration") pos = path.substring(12, 13).toInt();
            else{
              pos = path.substring(10, 11).toInt();
              String ss = Firebase.getString("device/sstatus/s" + String(pos));
              if(ss == ""){
                schedule[pos].stop();
                return;
              }
            }
            String ss = Firebase.getString("device/sstatus/s" + String(pos));
            if(ss != "SET") return;
            unsigned long _t = Firebase.getInt("device/sduration/s" + String(pos));
            unsigned long _ct = timeClient.getEpochTime();
            if(_t < _ct){
              Firebase.setString("device/sstatus/s" + String(pos), "TIME_ERROR");
              return; 
            }
            String sd = Firebase.getString("device/schedule/s" + String(pos));
            if(!validateIRcode(sd)){
              Firebase.setString("device/sstatus/s" + String(pos), "INVALID_IRCODE");
              return; 
            }
            int lp = Firebase.getInt("device/sloop/s" + String(pos));
            if(lp < 0 || lp > 24){
              Firebase.setString("device/sstatus/s" + String(pos), "INVALID_LOOP");
              return;
            }
            schedule[pos].duration = _t - _ct;
            schedule[pos].data = sd;
            schedule[pos].loop = lp;
            schedule[pos].set();
            Firebase.setString("device/sstatus/s" + String(pos), "WAIT");
          } 
        }
     }
  }   
}

void clearEEPROM(){
  Serial.println("Flash button pressed!");
  digitalWrite(LEDPIN, HIGH);
  EEPROM.begin(512);
  int w_a = EEPROM.read(0);
  delay(200);
  if(w_a){
    EEPROM.write(0, 0);
    delay(5);
    Serial.println("Cleared WifiInfo");
  }
  EEPROM.end();
  WIFI_SSID = "";
  WIFI_PASSWORD = "";
  flashPressed = false;
  digitalWrite(LEDPIN, LOW);
}

void loop() {
  if(flashPressed){
    clearEEPROM();
  }
  if(hasWifiInfo){
    mainOp();
  } else{
    server.handleClient();
  }
}
