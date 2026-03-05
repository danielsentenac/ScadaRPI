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
byte mac[] = {  
  0x90, 0xA2, 0xDA, 0x10, 0x60, 0x00 };

// Initialize the Ethernet server library
// with the IP address and port you want to use
// (port 80 is default for HTTP):
EthernetServer server(80);
IPAddress ip(192, 168, 224, 137);

// digital relay pin
#define PIN_RELAY 7


void setup() {
  
  // Open serial communications for MaxiGauge and wait for port to open:
  Serial.begin(9600);
  //Serial.begin(9600,SERIAL_8N1);
  delay(100);
  //Serial.setTimeout(2000);
  //Serial.println("Arduino Serial started");
  
   // sets the digital pin as output for RELAY
  pinMode(PIN_RELAY, OUTPUT);     
  
  // start the Ethernet connection:
  while (Ethernet.begin(mac) == 0) {
   delay(100);
  }
  server.begin();
  
}


void loop() {
  
  // check DHCP lease
  Ethernet.maintain(); 
  
  // listen for incoming Internet clients
  EthernetClient client = server.available();
  
  if (client) {
    // an http request ends with a blank line
    boolean currentLineIsBlank = true;
    String client_req;
    while (client.connected()) {
      if (client.available()) {
        char c = client.read();
        client_req+=c;
        // if you've gotten to the end of the line (received a newline
        // character) and the line is blank, the http request has ended,
        // so you can send a reply
        if (c == '\n' && currentLineIsBlank) {
          // Serial MaxiGauge data
          String serData1[6] = {"PR1\r","PR2\r","PR3\r","PR4\r","PR5\r","PR6\r"};
          String serData2[6] = {"","","","","",""};
          // Serial MaxiGauge Get Sensor data
          for (int i = 0; i < 6; i++) {
            Serial.print(serData1[i]); // MaxiGauge sensor number i
            delay(20);
            serData2[i] = Serial.readString();
            delay(20);
            Serial.print("\x05");
            delay(20);
            serData2[i] = Serial.readString();
          }
          // Clean client request
          client_req.replace("GET /","");
          client_req.replace("?","");
          client_req = client_req.substring(0,client_req.indexOf(" HTTP"));

          // Treat RELAY command form client_req
          if (client_req == "RELAY=ON") 
             digitalWrite(PIN_RELAY,HIGH); // Switch RELAY ON
          else if (client_req == "RELAY=OFF") 
             digitalWrite(PIN_RELAY,LOW); // Swith RELAY OFF
             
          // send a standard http response header
          client.println("HTTP/1.1 200 OK");
          client.println("Content-Type: text/html");
          client.println("Connection: close");  // the connection will be closed after completion of the response
          client.println("Refresh: 5");  // refresh the page automatically every 5 sec
          client.println();
          client.println ("<html>");
          client.println("<head>");
          client.println("<title> Sensor Page </title>");
          client.println("</head>");
          client.println("<body>");
          client.print("Client Request:");
          client.print(client_req);
          client.println("</br>");
          // Print Digital Relay value
          client.print("DigitalInRELAY (");
          client.print(PIN_RELAY);
          client.print("):");
          client.print(digitalRead(PIN_RELAY));
          client.println("</br>");
          // Print MaxiGauge sensor channels value
          for (int i = 0 ; i < 6 ; i++) {
           client.print("MaxiGauge sensor [");
           client.print(i+1);
           client.print("]:");
           client.print(serData2[i]);
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
    delay(100);
    // close the connection:
    client.stop();
  }
}

