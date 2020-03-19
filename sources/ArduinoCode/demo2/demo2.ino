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
}

// đọc và lưu tín hiệu thu được
void decodeIR(){
  delay(3000); // chờ khoảng thời gian đọc tín hiệu (3-5s)
  if(len > 0){ // tín hiệu đã được đọc vào mảng
    detachInterrupt(digitalPinToInterrupt(RXPINIR)); // bỏ qua ngắt
    rawLen = len - 1;
    len = 0; // đặt lại biến chạy
    attachInterrupt(digitalPinToInterrupt(RXPINIR), rxIR_Interrupt_Handler, CHANGE); // bật chế độ ngắt
  }
  digitalWrite(LEDPIN, LOW);
  delay(500);
  digitalWrite(LEDPIN, HIGH);
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

void loop() {
  if(Serial.available() > 0){
    int data = Serial.read();
    if(data == '0'){
      mode = !mode;
      len = 0;
      if(mode == 0){ // doi che do
        detachInterrupt(digitalPinToInterrupt(RXPINIR));
        Serial.print('0');
      } else{
        attachInterrupt(digitalPinToInterrupt(RXPINIR), rxIR_Interrupt_Handler, CHANGE);
        Serial.print('1');
      }
      
      delay(1000);
    }
    else if(data == '1'){
      if(mode == 0){
        sendIRCode();
        delay(500);
      }
    }
    else if(data == '2'){
      if(mode == 0){
        Serial.print('0');
      }
      else {
        Serial.print('1');
      }
    }
  }
  
  if(mode == 0){
    digitalWrite(LEDPIN, LOW);
  } else {
    digitalWrite(LEDPIN, HIGH);
    decodeIR();
  }
}

// ct xử lý ngắt trên chân số 2
void rxIR_Interrupt_Handler() {
  if (len > MAXLEN) return;
  irBuffer[len] = micros();
  if(len > 0) irBuffer[len-1] = irBuffer[len] - irBuffer[len-1]; // tính khoảng thời gian của trạng thái bật/tắt
  len++;
}
