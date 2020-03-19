#include <IRremote.h>


/*
 * IRremote: IRrecvDump - dump details of IR codes with IRrecv
 * An IR detector/demodulator must be connected to the input RECV_PIN.
 * Version 0.1 July, 2009
 * Copyright 2009 Ken Shirriff
 * http://arcfn.com
 * JVC and Panasonic protocol added by Kristian Lauszus (Thanks to zenwheel and other people at the original blog post)
 * LG added by Darryl Smith (based on the JVC protocol)
 */



int RECV_PIN = 2;
const int switchPin = 7;
int buttonState = 0;

IRrecv irrecv(RECV_PIN);
IRsend irsend;

decode_results results;
int last_value = 0;
int last_bits = 0;
unsigned int  raw[439] = {3460, 1760, 440, 404, 440, 1328, 440, 404, 440, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 428, 440, 428, 444, 424, 444, 1328, 440, 400, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 424, 444, 1324, 444, 1296, 444, 1300, 440, 400, 444, 428, 444, 1324, 440, 400, 444, 428, 444, 424, 444, 428, 444, 428, 440, 428, 444, 424, 444, 428, 444, 428, 440, 428, 444, 428, 440, 428, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 424, 448, 424, 444, 428, 444, 424, 444, 428, 440, 428, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 1324, 440, 1300, 440, 400, 444, 428, 444, 428, 440, 428, 444, 428, 444, 10692, 3512, 1736, 440, 404, 444, 1324, 440, 400, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 428, 440, 428, 444, 428, 444, 1320, 444, 400, 444, 428, 444, 424, 444, 428, 440, 428, 444, 428, 444, 424, 444, 1324, 440, 1300, 444, 1296, 444, 400, 444, 428, 440, 1324, 444, 400, 444, 428, 440, 428, 444, 428, 440, 428, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 424, 444, 1324, 448, 396, 444, 424, 444, 1324, 440, 404, 444, 1324, 440, 404, 440, 428, 444, 428, 444, 424, 444, 1324, 440, 404, 444, 1324, 440, 1300, 440, 400, 444, 428, 444, 428, 440, 428, 444, 428, 440, 428, 444, 428, 444, 424, 444, 428, 444, 1324, 440, 400, 444, 428, 444, 1324, 440, 400, 444, 1324, 440, 1300, 440, 1300, 440, 404, 444, 424, 444, 428, 444, 428, 440, 428, 444, 428, 440, 428, 444, 428, 440, 428, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 1324, 440, 1300, 440, 1300, 440, 404, 440, 428, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 428, 440, 428, 444, 1324, 440, 1300, 440, 1300, 440, 400, 444, 428, 444, 428, 440, 428, 444, 428, 440, 428, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 424, 452, 420, 444, 424, 444, 1324, 440, 404, 444, 424, 448, 424, 444, 424, 444, 428, 444, 428, 440, 1328, 440, 400, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 424, 444, 428, 444, 424, 448, 424, 444, 424, 444, 428, 444, 428, 440, 428, 444, 428, 444, 1324, 440, 1300, 440, 400, 444, 428, 444, 424, 444, 1324, 440, 1300, 440};
int count = 439;

void setup()
{
  Serial.begin(9600);
  //irrecv.enableIRIn(); // Start the receiver
  pinMode(switchPin, INPUT);
}

// Dumps out the decode_results structure.
// Call this after IRrecv::decode()
// void * to work around compiler issue
//void dump(void *v) {
//  decode_results *results = (decode_results *)v
void dump(decode_results *results) {
  count = results->rawlen - 1;
  if (results->decode_type == UNKNOWN) {
    Serial.print("Unknown encoding: ");
  } 
  else if (results->decode_type == NEC) {
    Serial.print("Decoded NEC: ");
  } 
  else if (results->decode_type == SONY) {
    Serial.print("Decoded SONY: ");
  } 
  else if (results->decode_type == RC5) {
    Serial.print("Decoded RC5: ");
  } 
  else if (results->decode_type == RC6) {
    Serial.print("Decoded RC6: ");
  }
  else if (results->decode_type == PANASONIC) {	
    Serial.print("Decoded PANASONIC - Address: ");
    Serial.print(results->panasonicAddress,HEX);
    Serial.print(" Value: ");
  }
  else if (results->decode_type == LG) {
     Serial.print("Decoded LG: ");
  }
  else if (results->decode_type == JVC) {
     Serial.print("Decoded JVC: ");
  }
  last_value = results->value;
  last_bits = results->bits;
  Serial.print(results->value, HEX);
  Serial.print(" (");
  Serial.print(results->bits, DEC);
  Serial.println(" bits)");
  Serial.print("Raw (");
  Serial.print(count, DEC);
  Serial.print("): ");

  for (int i = 0; i < count; i++) {
    Serial.print(results->rawbuf[i+1]*USECPERTICK, DEC);
    raw[i] = results->rawbuf[i+1]*USECPERTICK;
    Serial.print(", ");
  }
  Serial.println("");
//  delay(1000);
//  irsend.sendRaw(raw,count, 38);
//  Serial.println("--------------------------------------------------------");
//  delay(1000);
  //delay(5000);
}



void loop() {
  if (irrecv.decode(&results)) {
    Serial.println(results.value, HEX);
    dump(&results);
    
    
     
    irrecv.resume(); // Receive the next value
    

  }

//  Serial.println("--------------------------------------------------------");
//
//  irsend.sendRaw(raw, 99, 38);// raw là mã thô cần gửi đi. 
//                             // 99 là số phần tử trong mảng
//                            // 38 là tần sử gửi đi, tính theo kHz
//  delay(5000);


//  buttonState = digitalRead(switchPin);
//  if(buttonState == HIGH){
//    irsend.sendRaw(raw, count, 38);
//    //irsend.sendPanasonic(4004,7200000);
//    Serial.print("count: ");
//    Serial.println(count);
//     irrecv.enableIRIn();
//  }
//  delay(200);

  
}
