#include <ESP8266WiFi.h>
#include <FirebaseArduino.h>
#include <IRsend.h>
#include <NTPClient.h>
#include <Ticker.h>
#include <WiFiUdp.h>


#define FIREBASE_HOST "irremote-4f614.firebaseio.com"
#define FIREBASE_AUTH "KPv43AaJnuulZW8YvMNwKIZkvZj9wOie365KjzPg"
#define WIFI_SSID "Huy Dinh"
#define WIFI_PASSWORD "0123456789"

#define MAXLEN 500
#define RXPINIR 5
#define IRLEDPIN 13
#define LEDPIN 15
#define WFLED 0


bool sendIR(String);

WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP, "asia.pool.ntp.org", 0, 3600000L);

volatile unsigned int last = 0;
volatile unsigned int irBuffer[MAXLEN];
volatile unsigned int len = 0;
volatile int val = 0;
String data = "";
uint16_t rawLen = 0;
byte mode = 0;

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

void setup() {
  pinMode(IRLEDPIN, OUTPUT);
  pinMode(WFLED, OUTPUT);
  pinMode(RXPINIR, INPUT);
  pinMode(LEDPIN, OUTPUT);
  digitalWrite(LEDPIN, LOW);
  digitalWrite(IRLEDPIN, LOW);
  digitalWrite(WFLED, HIGH);
  Serial.begin(9600);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }
  timeClient.begin();

  for (int i = 0; i < 10; i++) schedule[i].pos = i;
  digitalWrite(WFLED, LOW);
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
}

void loop() {
//  delay(5000);
//  sendIR();
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
