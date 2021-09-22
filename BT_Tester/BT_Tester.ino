#include <SoftwareSerial.h>

#define BT_BAUD_RATE              115200         //115200 for red SparkFun chip, 9600 for blue chip
#define SERIAL_MONITOR_BAUD_RATE  9600


#define BT_CMD_HEADER "$BT_CMD"
#define BT_CMD_FOOTER "@@@"

#define BT_DELIM ','


SoftwareSerial BT_Module(4,5);

void setup() {
  
  Serial.begin(SERIAL_MONITOR_BAUD_RATE);
  BT_Module.begin(BT_BAUD_RATE);

}

void loop() {
  // put your main code here, to run repeatedly:

  if(BT_Module.available()){
    Serial.write(BT_Module.read());
  }

}
