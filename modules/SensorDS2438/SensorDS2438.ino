/*
  Web Server

 A simple web server that shows the value of the analog input pins.
 using an Arduino Wiznet Ethernet shield.

 Circuit:
 * Ethernet shield attached to pins 10, 11, 12, 13
 * Analog inputs attached to pins A0 through A5 (optional)

 created 18 Dec 2009
 by David A. Mellis
 modified 9 Apr 2012
 by Tom Igoe

 */

#include <OneWire.h> // OneWire library
#include <DS2438.h> 

// Enter a MAC address and IP address for your controller below.
// The IP address will be dependent on your local network:
byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5C, 0xDC };  // arduino-10


#define DS24B38 0x26       // 1-Wire DS2438 address (DS2438 + HIH5030 sensor)
#define DIGITAL_ONEWIRE 9 // digital pin address used for bus 1-Wire
#define BUS_SENSOR_MAX 1   // Max number of sensors

OneWire ds7(DIGITAL_ONEWIRE); // OneWire object

void getAddress(String &id, byte addr[8]) {
  for (uint8_t i = 1; i < 7; i++) {
    if (addr[i] < 0x10) id += String("0");;
    id += String(addr[i], HEX);
  }
  Serial.print("address:");Serial.println(id);
}
void getDigitalIn(String &DigitalInStr, String &DigitalInBisStr, byte addr[8]){
  byte data[9];
  // data : Data from scratchpad
  // addr : Module address 
  float DigitalIn;
  float temperature;
  
  if (OneWire::crc8(addr, 7) != addr[7]) {// Check address integrity
    DigitalInStr = String("address failed");
    return;
  }
      
  else if (addr[0] != DS24B38) {// Check DS24B38 module
    DigitalInStr = String("no DS24B38");
    return;
  }
 
  DS2438 ds2438(&ds7, addr);
  ds2438.begin();
  ds2438.update();
  if (ds2438.isError() || ds2438.getVoltage(DS2438_CHA) == 0.0) {
      DigitalInStr = String("Error reading from DS2438 device");
      return;
    }
  temperature = ds2438.getTemperature();
  float rh = (ds2438.getVoltage(DS2438_CHA) / ds2438.getVoltage(DS2438_CHB) - 0.1515) / 0.00636;
  Serial.print("Voltage A=");Serial.println(ds2438.getVoltage(DS2438_CHA));
  Serial.print("Voltage B=");Serial.println(ds2438.getVoltage(DS2438_CHB));
  DigitalIn = (float)(rh / (1.0546 - 0.00216 * temperature));
  if (DigitalIn < 0.0)
     DigitalIn = 0.0;
  if (DigitalIn > 100.0)
     DigitalIn = 100.0;

  DigitalInBisStr = String(temperature);
  DigitalInStr = String(DigitalIn);
  Serial.println(DigitalInBisStr);
  Serial.println(DigitalInStr);

}
void setup() {
  
  // Switch OFF the 24V DC-DC converter
  //sensorBoard.OFF();

  // 1-wire bus reset
  ds7.reset(); 
  // Set analog pin to digital output            
  //pinMode(A0, OUTPUT);
  // Open serial communications and wait for port to open:
  Serial.begin(9600);
  while (!Serial) {
    ; // wait for serial port to connect. Needed for native USB port only
  }
  Serial.println("Arduino Serial started...");
  
}


void loop() {

  // Get Channels
 
  String DigitalIn[BUS_SENSOR_MAX];
  String DigitalInBis[BUS_SENSOR_MAX];
  int   busCnt = 0;
  byte  addr[8] ={0,0,0,0,0,0,0,0};
  String address[BUS_SENSOR_MAX];
  static int cnt = 0;
  Serial.print("Search device...");Serial.println(++cnt);
  if (ds7.search(addr) == 1 && busCnt < BUS_SENSOR_MAX) { // Scan 1-Wire module
    getAddress(address[busCnt],addr);
    getDigitalIn(DigitalIn[busCnt],DigitalInBis[busCnt],addr);
    delay(100);
    busCnt++;
  }
  Serial.println(address[0]);
  delay(1000);
}

