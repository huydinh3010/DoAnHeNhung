#include <IRremote.h>
#define LEDPIN 13 // đèn báo trạng thái
#define BTNMODE 8 
#define BTNSEND 7

#define MAXLEN 500
#define RXPINIR 2

int mode = 0; 
String data = "";
volatile int val = 0;
bool sstart = false;
bool ssize = false;
int cmd = -1;
byte * sbuf = new byte[MAXLEN];
unsigned int sbufSize = 0;
unsigned int sbufIn = 0;
byte zc = 0;
byte oc = 0;
volatile unsigned long last = 0;
volatile unsigned int* irBuffer;
volatile unsigned int len = 0; // biến trỏ đến phần tử trong mảng irBuffer
int rawLen = 0;
IRsend irsend;

void serialWriteMode(byte mode){
  Serial.print("SM ");
  Serial.println(mode);
}

void serialWriteCode(String & code){
  Serial.print("SC ");
  Serial.print(code.length()); Serial.print(" ");
  Serial.println(code);
}

void setup() {
  pinMode(LEDPIN, OUTPUT);
  digitalWrite(LEDPIN, LOW);
  pinMode(RXPINIR, INPUT);
  pinMode(BTNMODE, INPUT);
  pinMode(BTNSEND, INPUT);
  Serial.begin(9600);
  Serial.setTimeout(2000);
  serialWriteMode(0);
}

// đọc và lưu tín hiệu thu được
void readIR(){
  if(len > 0){ // tín hiệu đã được đọc vào mảng
    detachInterrupt(digitalPinToInterrupt(RXPINIR)); // bỏ qua ngắt
    rawLen = len - 1;
    len = 0; // đặt lại biến chạy
    data = "";
    byte b1 = (byte)((rawLen & 0xFFC0) >> 6);
    byte b2 = (byte)(rawLen & 0x003F);
    data += (char)(b1 + 60);
    data += (char)(b2 + 60);
    int i = 0;
    unsigned int t = 0;
    byte c = 0;
    while(i < rawLen){
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
    data += (char)35;
    i = 1;
    while(i < rawLen){
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
    serialWriteCode(data);
    serialWriteMode(0);
  }
  digitalWrite(LEDPIN, HIGH);
  delay(500);
  digitalWrite(LEDPIN, LOW);
}

bool ir_avaiable(){
  delay(10);
  if(len == 0 || val == 0) return false;
  unsigned long delta_t = micros() - last;  
  if(delta_t < 1000000L) return false;
  return true;
}

// phát tín hiệu hồng ngoại
void sendIR(){
  if(data.length() < 2) return;
  byte b0;
  int i = 0;
  byte b1 = (byte)data[i++] - 60;
  byte b2 = (byte)data[i++] - 60;
  rawLen = b1 * 64 + b2;
  int j = 0;
  byte c = 0;
  unsigned int t = 0;
  while(i < data.length()){
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
  irsend.enableIROut(38); // bật chế độ phát ở tần số 38kHz
  for (int i = 0; i < rawLen; i++){
    if(i & 1){
      irsend.space(irBuffer[i] * 50); // tắt trong khoảng irBuffer[i]
    } else{
      irsend.mark(irBuffer[i] * 50); // bật
    }
    irsend.space(0);
  }
  serialWriteMode(0);
}
//
byte readByteSerial(){
  byte c = 0;
  byte rb = 255;
  do{
    delay(1);
    if(Serial.available()) rb = Serial.read();
    if(c++ > 200) return 255;
  } while(rb == 255);
  return rb;
}

void readSerial(){
//    if(Serial.available()){
//      delay(1);
//      byte b = Serial.read();
//      Serial.write(b);
//      if(b == 0) zc++; else zc = 0;
//      if(b == 1) oc++; else oc = 0;
//      sbuf[sbufIn] = b;
//      sbufIn++;
//      if(sbufIn >= MAXLEN) sbufIn = 0;
//      if(zc >= 5){
//        sbufIn = 0;
//        data ="";
//        zc = 0;
//      }
//      if(oc >= 5){
////        for(int i = 0; i < sbufIn; i++){
////          Serial.write(sbuf[i]); 
////        }
//        sbufIn = 0;
//        oc = 0;
//      }
//    }
  if(Serial.available() >= 5){
    for(int i = 0; i < 5; i++){
      if(readByteSerial() != 0) return;
    }
    byte cmd = readByteSerial();
    if(cmd == 0){
      byte _m = readByteSerial();
      if(_m == 1){
        mode = 1;
        digitalWrite(LEDPIN, HIGH);
//        len = 0;
//        attachInterrupt(digitalPinToInterrupt(RXPINIR), rxIR_Interrupt_Handler, CHANGE);
      } else if(_m == 2){
        mode = 2;
        digitalWrite(LEDPIN, LOW);
//        detachInterrupt(digitalPinToInterrupt(RXPINIR));
//        sendIR();
//        delay(500);
      } else{
        mode = 0;
        digitalWrite(LEDPIN, LOW);
//        detachInterrupt(digitalPinToInterrupt(RXPINIR));
      }
    } else if(cmd == 1){
      byte b1 = readByteSerial();
      byte b2 = readByteSerial();
      if(b1 == 255 || b2 == 255) return;
      int dataLen = b1 * 256 + b2;
      data = "";
      for(int i = 0; i < dataLen; i++){
        b1 = readByteSerial();
        if (b1 == 255){
          data = "";
          return;
        }
        data += (char)b1;
      }
      Serial.println(data);
//    }
    }
  }
//    String cmd = Serial.readStringUntil(' ');
//    if(cmd.equals("SM")){
//      int _m = Serial.parseInt();
//      if (_m == 1){
//        //mode = 1;
//        len = 0;
//        digitalWrite(LEDPIN, HIGH);
//        //attachInterrupt(digitalPinToInterrupt(RXPINIR), rxIR_Interrupt_Handler, CHANGE);
//      } else if (_m == 2){
//        //mode = 2;
//        //detachInterrupt(digitalPinToInterrupt(RXPINIR));
//        digitalWrite(LEDPIN, LOW);
//        //sendIR();
//        //delay(500);
//      } else {
//        //mode = 0;
//        digitalWrite(LEDPIN, LOW);
//        //detachInterrupt(digitalPinToInterrupt(RXPINIR));
//      }
//    } else if(cmd.equals("SC")){
//      data = "";
//      int dataLen = Serial.parseInt();
//      Serial.read();
//      while(dataLen > 0){
//        String s = Serial.readStringUntil(' ');
//        data += s;
//        dataLen -= s.length();
//      }
//      Serial.println(data);
//    }
//  }
}

void loop() {
  readSerial();
//  if(mode == 1 && ir_avaiable()){
//      readIR();
//  }
}

// ct xử lý ngắt trên chân số 2
void rxIR_Interrupt_Handler() {
  val = digitalRead(RXPINIR);
  if (len > MAXLEN) return;
  last = micros();
  irBuffer[len] = last;
  if(len > 0) irBuffer[len-1] = (irBuffer[len] - irBuffer[len-1] + 25) / 50; // tính khoảng thời gian của trạng thái bật/tắt
  len++;
}
