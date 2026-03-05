/*

  RS485_HalfDuplex.pde - example using ModbusMaster library to communicate
  with EPSolar LS2024B controller using a half-duplex RS485 transceiver.

  This example is tested against an EPSolar LS2024B solar charge controller.
  See here for protocol specs:
  http://www.solar-elektro.cz/data/dokumenty/1733_modbus_protocol.pdf

  Library:: ModbusMaster
  Author:: Marius Kintel <marius at kintel dot net>

  Copyright:: 2009-2016 Doc Walker

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

*/

#include <ModbusMaster.h>
#include <SoftwareSerial.h> // Serial library

/*!
  We're using a MAX485-compatible RS485 Transceiver.
  Rx/Tx is hooked up to the hardware serial port at 'Serial'.
  The Data Enable and Receiver Enable pins are hooked up as follows:
*/
#define MAX485_DE  A4
// define  RS485 Serial port
SoftwareSerial RS485Serial(9, 12); // RX, TX for Leonardo

// instantiate ModbusMaster object
ModbusMaster node;

#define REG_NUMBER 20

bool start = true;

void preTransmission()
{
  digitalWrite(MAX485_DE, HIGH);
}

void postTransmission()
{
  digitalWrite(MAX485_DE, LOW);
}

void setup()
{
  pinMode(MAX485_DE, OUTPUT);
  // Init in receive mode
  digitalWrite(MAX485_DE, 0);

  // Console runs at 9600 baud
  Serial.begin(9600);

  // Open serial communication for Modbus RTU IntesisBox com
  RS485Serial.begin(19200);
  RS485Serial.listen();
  // Modbus slave ID 1
  node.begin(1, RS485Serial);
  
  // Callbacks allow us to configure the RS485 transceiver correctly
  node.preTransmission(preTransmission);
  node.postTransmission(postTransmission);
}


void loop()
{
  uint8_t result;
  uint16_t data[100];
  
  // Read 16 registers starting at 0x0000)
  result = node.readHoldingRegisters(0x0000, REG_NUMBER);
  if (result == node.ku8MBSuccess)
  {
     for (int j = 0; j < REG_NUMBER; j++)
    {
      data[j] = node.getResponseBuffer(j);
      Serial.print("data["); Serial.print(j); Serial.print("]="); 
      Serial.println(data[j]);
    }
    if (start == true) {
      result = node.writeSingleRegister(0x0000, 1);
      start = false;
    }
  }
  else
    Serial.println("Modbus Error");

  delay(1000);
}

