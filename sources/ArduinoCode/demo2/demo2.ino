#include <IRremote.h>
#define LEDPIN 13 // đèn báo trạng thái
#define BTNMODE 8 
#define BTNSEND 7

#define MAXLEN 600
#define RXPINIR 2

int mode = 0; // 0: chờ và đọc tín hiệu, 1: gửi tín hiệu

volatile  unsigned int irBuffer[MAXLEN]; // mảng lưu tín hiệu
volatile unsigned int len = 0; // biến trỏ đến phần tử trong mảng irBuffer
int rawLen;
IRsend irsend;

void setup() {
  pinMode(LEDPIN, OUTPUT);
  digitalWrite(LEDPIN, LOW);
  pinMode(BTNMODE, INPUT);
  pinMode(BTNSEND, INPUT);
  Serial.begin(115200);
  Serial.setTimeout(2000);
}

// đọc và lưu tín hiệu thu được
void decodeIR(){
  delay(3000); // chờ khoảng thời gian đọc tín hiệu (3-5s)
  if(len > 0){ // tín hiệu đã được đọc vào mảng
    detachInterrupt(digitalPinToInterrupt(RXPINIR)); // bỏ qua ngắt
    rawLen = len - 1;
    len = 0; // đặt lại biến chạy
    Serial.print("code:");
    Serial.print(rawLen);
    Serial.print(" ");
    for(int i = 0; i < rawLen; i++){
      Serial.print(irBuffer[i]);
      Serial.print(" ");
    }
    Serial.println();
    //attachInterrupt(digitalPinToInterrupt(RXPINIR), rxIR_Interrupt_Handler, CHANGE); // bật chế độ ngắt
  }
  digitalWrite(LEDPIN, LOW);
  delay(500);
  digitalWrite(LEDPIN, HIGH);
  Serial.print("mode:");
  Serial.println('0');
}

// phát tín hiệu hồng ngoại
void sendIRCode(){
  irsend.enableIROut(38); // bật chế độ phát ở tần số 38kHz
  for (int i = 0; i < rawLen; i++){
    if(i & 1){
      irsend.space(irBuffer[i]); // tắt trong khoảng irBuffer[i]
    } else{
      irsend.mark(irBuffer[i]); // bật
    }
  }
  irsend.space(0);
}

void readSerial(){
  if(Serial.available() > 0){
    char cmd[6];
    memset(cmd, 0, sizeof(cmd));
    Serial.readBytes(cmd, 5);
    if(strcmp(cmd,"mode:") == 0){
      byte b = Serial.read();
      mode = b - '0';
      if(mode == 0){ // doi che do
        digitalWrite(LEDPIN, LOW);
        detachInterrupt(digitalPinToInterrupt(RXPINIR));
      } else{
        len = 0;
        digitalWrite(LEDPIN, HIGH);
        attachInterrupt(digitalPinToInterrupt(RXPINIR), rxIR_Interrupt_Handler, CHANGE);
        decodeIR();
      }
    } else if(strcmp(cmd,"code:") == 0){
      int len = Serial.parseInt(); 
      if(len > MAXLEN || len <= 0) return;
      for(int i = 0; i < len; i++){
        int d = Serial.parseInt();
        if (d <= 0) return;
        irBuffer[i] = d;
      }
      if(mode == 0){
        sendIRCode();
        delay(500);
      }
    }
  }
}

void loop() {
  readSerial();
}

// ct xử lý ngắt trên chân số 2
void rxIR_Interrupt_Handler() {
  if (len > MAXLEN) return;
  irBuffer[len] = micros();
  if(len > 0) irBuffer[len-1] = irBuffer[len] - irBuffer[len-1]; // tính khoảng thời gian của trạng thái bật/tắt
  len++;
}
