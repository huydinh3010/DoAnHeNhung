/* rawSend.ino Example sketch for IRLib2
 *  Illustrates how to send a code Using raw timings which were captured
 *  from the "rawRecv.ino" sample sketch.  Load that sketch and
 *  capture the values. They will print in the serial monitor. Then you
 *  cut and paste that output into the appropriate section below.
 */
#include <IRLibSendBase.h>    //We need the base code
#include <IRLib_HashRaw.h>    //Only use raw sender

IRsendRaw mySender;
const int switchPin = 7;
int buttonState = 0;
void setup() {
  Serial.begin(9600);
  delay(2000); while (!Serial); //delay for Leonardo
  Serial.println(F("Every time you press a key is a serial monitor we will send."));
}
/* Cut and paste the output from "rawRecv.ino" below here. It will 
 * consist of a #define RAW_DATA_LEN statement and an array definition
 * beginning with "uint16_t rawData[RAW_DATA_LEN]= {…" and concludes
 * with "…,1000};"
 */

#define RAW_DATA_LEN 132
uint16_t rawData[RAW_DATA_LEN]={
  3406, 1786, 386, 486, 386, 1354, 386, 486, 
  382, 486, 386, 486, 382, 486, 386, 486, 
  386, 482, 386, 486, 386, 482, 386, 486, 
  386, 482, 386, 486, 386, 1354, 386, 486, 
  402, 466, 386, 486, 382, 486, 386, 486, 
  382, 486, 386, 486, 402, 1338, 402, 1338, 
  406, 1334, 386, 486, 406, 462, 386, 1354, 
  386, 486, 406, 462, 386, 486, 382, 486, 
  386, 486, 406, 466, 382, 486, 386, 486, 
  382, 486, 406, 466, 382, 486, 386, 486, 
  382, 486, 386, 486, 382, 486, 406, 466, 
  386, 486, 382, 486, 386, 486, 382, 486, 
  386, 486, 382, 486, 386, 486, 382, 486, 
  386, 486, 386, 486, 382, 486, 386, 486, 
  382, 486, 386, 486, 382, 1358, 382, 1358, 
  382, 490, 382, 486, 386, 482, 386, 486, 
  386, 486, 386, 1000};




/*
 * Cut-and-paste into the area above.
 */
   
void loop() {
  
    //send a code every time a character is received from the 
    // serial port. You could modify this sketch to send when you
    // push a button connected to an digital input pin.
    mySender.send(rawData,RAW_DATA_LEN,38);//Pass the buffer,length, optionally frequency
    Serial.println(F("Sent signal."));
    delay(5000);
}

