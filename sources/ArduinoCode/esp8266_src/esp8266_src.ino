#include <ESP8266WiFi.h>
#include <FirebaseArduino.h>
#include <IRsend.h>
#define FIREBASE_HOST "irremote-4f614.firebaseio.com"
#define FIREBASE_AUTH "KPv43AaJnuulZW8YvMNwKIZkvZj9wOie365KjzPg"
#define WIFI_SSID "VNPT-Binh"
#define WIFI_PASSWORD "0123456789"

#define MAXLEN 500
#define RXPINIR 5
#define IRLEDPIN 13
#define LEDPIN 15
#define WFLED 0


volatile unsigned int last = 0;
volatile unsigned int irBuffer[MAXLEN];
volatile unsigned int len = 0;
volatile int val = 0;
String data = "";
uint16_t rawLen = 0;
byte mode = 0;


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

void readIR(){
  if(len > 0){ // tín hiệu đã được đọc vào mảng
    if(len < 20){
      len = 0;
      return;
    }
    detachInterrupt(digitalPinToInterrupt(RXPINIR));
    rawLen = len - 1;
    len = 0; // đặt lại biến chạy
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
    Firebase.setString("device/ircode", data); // cập nhật mã tín hiệu đã đọc được
    Firebase.setFloat("device/mode", 0);
  }
}

void sendIR(){ // phát tín hiệu hồng ngoại
  if(data.length() < 2) return;
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
      if(i > data.length() - 2) return;
      c = (byte)data[i++] - 40;
      b0 = (byte)data[i++];
      if(b0 == 34){
        if(i > data.length() - 2) return;
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
      if(i > data.length() - 2) return;
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
//  Serial.println(rawLen);
//  for(int i = 0; i < rawLen; i++){
//    Serial.print(irBuffer[i]); Serial.print(" ");
//  }
//  Serial.println();
  
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
  Firebase.setFloat("device/mode", 0); // cập nhật chế độ lên firebase
}

void setMode(byte _m){ // chuyển chế độ (mode)
  if(_m == 1){
    mode = 1;
    len = 0;
    digitalWrite(LEDPIN, HIGH);
    attachInterrupt(digitalPinToInterrupt(RXPINIR), rxIR_Interrupt_Handler, CHANGE);
  } else if(_m == 2){
    mode = 2;
    digitalWrite(LEDPIN, LOW);
    detachInterrupt(digitalPinToInterrupt(RXPINIR));
    sendIR();
    delay(500);
  } else {
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
  digitalWrite(WFLED, LOW);
  Firebase.begin(FIREBASE_HOST, FIREBASE_AUTH);
  Firebase.stream("/device");
  //attachInterrupt(digitalPinToInterrupt(RXPINIR), rxIR_Interrupt_Handler, CHANGE);
}

void loop() {
//  delay(5000);
//  sendIR();
  if(mode == 1 && ir_available()){
    readIR();
  }
  if (Firebase.available()) { // bắt sự kiện từ firebase
     FirebaseObject event = Firebase.readEvent();
     String eventType = event.getString("type");
     eventType.toLowerCase();
     if(eventType == "put"){
        String path = event.getString("path");
        if(path == "/"){
           JsonVariant payload = event.getJsonVariant("data");
           int _m = payload["mode"];
           setMode((byte)_m);
           String s = payload["scode"];
           data = s;
           Serial.println(data);
        }
        else if(path == "/mode"){
           int _m = event.getInt("data");
           setMode((byte)_m);
        }
        else if(path == "/scode"){
          data = event.getString("data");
          Serial.println(data);
        }
     }
  }   
}
