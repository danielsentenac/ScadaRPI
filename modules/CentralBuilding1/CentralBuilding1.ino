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

#include <SPI.h>
#include <Ethernet.h>  // Ethernet library
#include <OneWire.h> // OneWire library
#include <DS2438.h> 
#include <currentLoop.h> // 4-20mA board library

// Enter a MAC address and IP address for your controller below.
// The IP address will be dependent on your local network:
//byte mac[] = {  
//  0x90, 0xA2, 0xDA, 0x10, 0x2C, 0x1D };

byte mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x78, 0xAB };
IPAddress ip( 192, 168, 224, 181 ); // Controllino-22 has this IP

// Initialize the Ethernet server library
// with the IP address and port you want to use
// (port 80 is default for HTTP):
EthernetServer server(80);
//IPAddress ip(192, 168, 229, 101);

// 4-20mA Board Sensor channels
#define CH_CUR1 CHANNEL1
#define CH_CUR2 CHANNEL2
#define CH_CUR3 CHANNEL3
#define CH_CUR4 CHANNEL4

// Working Analog channels with 4-20mA mezzanine
#define CH_VOLT1 A5
#define CH_VOLT2 A4
#define CH_VOLT3 A3
#define CH_VOLT4 A2


// Mean calculation count
#define CH_VOLT_CNT 10

// Calibration factors
#define A_CH_CUR1 1
#define B_CH_CUR1 0

#define A_CH_CUR2 1
#define B_CH_CUR2 0

#define A_CH_CUR3 1
#define B_CH_CUR3 0

#define A_CH_CUR4 1
#define B_CH_CUR4 0

#define A_CH_VOLT1 10
#define B_CH_VOLT1 -1000

#define A_CH_VOLT2 10
#define B_CH_VOLT2 -1000

#define A_CH_VOLT3 10
#define B_CH_VOLT3 -1000

#define A_CH_VOLT4 10
#define B_CH_VOLT4 -1000

#define DS24B38 0x26      // 1-Wire DS2438 address (DS2438 + HIH5030 sensor)
#define DIGITAL_ONEWIRE 9 // digital address used for bus 1-Wire
#define BUS_SENSOR_MAX 5  // Max number of sensors

OneWire ds7(DIGITAL_ONEWIRE); // OneWire object

void getAddress(String &id, byte addr[8]) {
  for (uint8_t i = 1; i < 7; i++) {
    if (addr[i] < 0x10) id += String("0");;
    id += String(addr[i], HEX);
  }
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
  DigitalIn = (float)(rh / (1.0546 - 0.00216 * temperature));
  if (DigitalIn < 0.0)
     DigitalIn = 0.0;
  if (DigitalIn > 100.0)
     DigitalIn = 100.0;

  DigitalInBisStr = String(temperature);
  DigitalInStr = String(DigitalIn);

}
void setup() {
  
  // Switch OFF the 24V DC-DC converter
  //sensorBoard.OFF();
  delay(100);

  // 1-wire bus reset
  //ds7.reset();             
  
  // Open serial communications and wait for port to open:
  Serial.begin(115200);
  delay(100);
  Serial.println("Arduino 4-20mA board switched OFF...");
 
  // start the Ethernet connection:
  while (Ethernet.begin(mac) == 0) {
   delay(100);
   Serial.println("Failed to configure Ethernet using DHCP");
  }
  Serial.print("server is connected...");
  server.begin();
  Serial.print("server is at ");
  Serial.println(Ethernet.localIP());
  
}


void loop() {

  // Get Channels
  float CurrentIn1 = 0;
  const char* error_C1;
  float CurrentIn2 = 0;
  const char* error_C2;
  float CurrentIn3 = 0;
  const char* error_C3;
  float CurrentIn4 = 0;
  const char* error_C4;
  float AnalogIn1 = 0;
  float AnalogIn2 = 0;
  float AnalogIn3 = 0;
  float AnalogIn4 = 0;
  String DigitalIn[BUS_SENSOR_MAX];
  String DigitalInBis[BUS_SENSOR_MAX];
  int   busCnt = 0;
  byte  addr[8] ={0,0,0,0,0,0,0,0};
  String address[BUS_SENSOR_MAX];


  // check DHCP lease
  Ethernet.maintain(); 
  
  while (ds7.search(addr) == 1 && busCnt < BUS_SENSOR_MAX) { // Scan 1-Wire module
    getAddress(address[busCnt],addr);
    getDigitalIn(DigitalIn[busCnt],DigitalInBis[busCnt],addr);
    delay(100);
    busCnt++;
  }
 
  if (sensorBoard.isConnected(CH_CUR1)) {
     CurrentIn1 = sensorBoard.readCurrent(CH_CUR1);
     CurrentIn1 = CurrentIn1 * A_CH_CUR1 + B_CH_CUR1;
     error_C1 = "OK";
  }
  else
     error_C1 = "not connected";

  if (sensorBoard.isConnected(CH_CUR2)) {
     CurrentIn2 = sensorBoard.readCurrent(CH_CUR2);
     CurrentIn2 = CurrentIn2 * A_CH_CUR2 + B_CH_CUR2;
     error_C2 = "OK";
  }
  else
     error_C2 = "not connected";

  if (sensorBoard.isConnected(CH_CUR3)) {
     CurrentIn3 = sensorBoard.readCurrent(CH_CUR3);
     CurrentIn3 = CurrentIn3 * A_CH_CUR2 + B_CH_CUR2;
     error_C3 = "OK";
  }
  else
     error_C3 = "not connected";

  if (sensorBoard.isConnected(CH_CUR4)) {
     CurrentIn4 = sensorBoard.readCurrent(CH_CUR4);
     CurrentIn4 = CurrentIn4 * A_CH_CUR4 + B_CH_CUR4;
     error_C4 = "OK";
  }
  else
     error_C4 = "not connected";

  // Average AnalogIn values
  for (int i = 0 ; i < CH_VOLT_CNT; i++) {
   AnalogIn1 += analogRead(CH_VOLT1);
   AnalogIn2 += analogRead(CH_VOLT2);
   AnalogIn3 += analogRead(CH_VOLT3);
   AnalogIn4 += analogRead(CH_VOLT4);
   delay(10); // sensor max response is 6ms
  }
  AnalogIn1/=CH_VOLT_CNT;
  AnalogIn2/=CH_VOLT_CNT;
  AnalogIn3/=CH_VOLT_CNT;
  AnalogIn4/=CH_VOLT_CNT;
  
  AnalogIn1 = (AnalogIn1 * A_CH_VOLT1 + B_CH_VOLT1) * 1e-3;
  AnalogIn2 = (AnalogIn2 * A_CH_VOLT2 + B_CH_VOLT2) * 1e-3;
  AnalogIn3 = (AnalogIn3 * A_CH_VOLT3 + B_CH_VOLT3) * 1e-3;
  AnalogIn4 = (AnalogIn4 * A_CH_VOLT4 + B_CH_VOLT4) * 1e-3;
  
  // listen for incoming Internet clients
  EthernetClient client = server.available();
  if (client) {
    // an http request ends with a blank line
    boolean currentLineIsBlank = true;
    while (client.connected()) {
      if (client.available()) {
        char c = client.read();
        // if you've gotten to the end of the line (received a newline
        // character) and the line is blank, the http request has ended,
        // so you can send a reply
        if (c == '\n' && currentLineIsBlank) {
          // send a standard http response header
          client.println("HTTP/1.1 200 OK");
          client.println("Content-Type: text/html");
          client.println("Connection: close");  // the connection will be closed after completion of the response
          client.println("Refresh: 5");  // refresh the page automatically every 5 sec
          client.println();
          client.println("<html>");
          client.println("<head>");
          client.println("<title> Central Building Sensor Page </title>");
          client.println("</head>");
          client.println("<body>");
          client.print("CurrentIn1:");
          client.print(CurrentIn1);
//          client.print("&nbsp;Bar (");
          client.print(" (");
          client.print(error_C1);
          client.println(")</br>");
          client.print("CurrentIn2:");
          client.print(CurrentIn2);
//          client.print("&nbsp;m&sup3; (");
          client.print(" (");
          client.print(error_C2);
          client.println(")</br>");
          client.print("CurrentIn3:");
          client.print(CurrentIn3);
//          client.print("&nbsp;m&sup3; (");
          client.print(" (");
          client.print(error_C3);
          client.println(")</br>");
          client.print("CurrentIn4:");
          client.print(CurrentIn4);
//          client.print("&nbsp;m&sup3; (");
          client.print(" (");
          client.print(error_C4);
          client.println(")</br>");
          client.print("AnalogIn1:");
          client.print(AnalogIn1);
//          client.println("&nbsp;L/m");
          client.println("</br>");
          client.print("AnalogIn2:");
          client.print(AnalogIn2);
//          client.println("&nbsp;L/m");
          client.println("</br>");
          client.print("AnalogIn3:");
          client.print(AnalogIn3);
//          client.println("&nbsp;L/m");
          client.println("</br>");
          client.print("AnalogIn4:");
          client.print(AnalogIn4);
//          client.println("&nbsp;L/m");
          client.println("</br>");
          for (int i = 0; i < BUS_SENSOR_MAX;i++) {
           client.print("DigitalIn (");
           client.print(address[i]);
           client.print("):");
           client.print(DigitalIn[i]);
//           client.print("&nbsp;%RH ");
           client.println("</br>");
          }
          for (int i = 0; i < BUS_SENSOR_MAX;i++) {
           client.print("DigitalInBis (");
           client.print(address[i]);
           client.print("):");
           client.print(DigitalInBis[i]);
//           client.print("&nbsp;C ");
           client.println("</br>");
          }
          client.println("</center>");
          client.println("</body>");
          client.println("</html>");
          break;
        }
        if (c == '\n') {
          // you're starting a new line
          currentLineIsBlank = true;
        }
        else if (c != '\r') {
          // you've gotten a character on the current line
          currentLineIsBlank = false;
        }
      }
    }
    // give the web browser time to receive the data
    delay(1000);
    // close the connection:
    client.stop();
  }
}

