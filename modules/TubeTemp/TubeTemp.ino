// TCMux shield by Ocean Controls (same as MuxModbus2)

/*********************************************************************************************************/
// This is to work with Leonardo ETH V2 (Compliant model)
#define ETHCS 10 //W5500 CS
#define ETHRST 11 //W5500 RST
#define SDCS 4 //SD CS pin
/*********************************************************************************************************/

#include <ModbusTCPSlave.h> // Modbus2 library

/*uint8_t mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5C, 0xDC };
IPAddress ip( 192, 168, 224, 146 ); // Arduino-10 has this IP

uint8_t mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x60, 0x42 };
IPAddress ip( 192, 168, 224, 151 ); // Arduino-11 has this IP

uint8_t mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5F, 0xD3 };
IPAddress ip( 192, 168, 224, 153 ); // Arduino-12 has this IP

uint8_t mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5F, 0xD2 };
IPAddress ip( 192, 168, 224, 173 ); // Arduino-13 has this IP

uint8_t mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5F, 0xFF };
IPAddress ip( 192, 168, 224, 174 ); // Arduino-14 has this IP

uint8_t mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x5C, 0xED };
IPAddress ip( 192, 168, 224, 175 ); // Arduino-15 has this IP
*/
uint8_t mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x60, 0x4D };
IPAddress ip( 192, 168, 224, 177 ); // Arduino-17 has this IP
/*
uint8_t mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x60, 0x2C };
IPAddress ip( 192, 168, 224, 178 ); // Arduino-18 has this IP
has this IP

uint8_t mac[] = { 0x90, 0xA2, 0xDA, 0x10, 0x27, 0x39 };
IPAddress ip(192, 168, 224, 137); // Arduino-1 has this IP (Temperature Sensor Mux)
*/

// Define the ModbusTCPSlave object with port = 502
ModbusTCPSlave modbus(502);

//#define SHOWMEYOURBITS // Display the raw 32bit binary data from the MAX31855
// Modbus registers

#define NB_HOLDING_REGISTERS 20
#define TEMP_1_ADDR 0
#define TEMP_2_ADDR 2
#define TEMP_3_ADDR 4
#define TEMP_4_ADDR 6
#define TEMP_5_ADDR 8
#define TEMP_6_ADDR 10
#define TEMP_7_ADDR 12
#define TEMP_8_ADDR 14
#define TEMP_INT_ADDR 16
#define ARD_RESET_ADDR 18

uint16_t holdingRegisters[NB_HOLDING_REGISTERS];

// Normalization factors & Temperature 
#define tempFactor 3
#define tempDelta 3
double temp[9] = {-99,-99,-99,-99,-99,-99,-99,-99,-99};

// Conversion tool
typedef union {
       float fvalue;
       uint16_t value[2];
  } FloatUint16;

// Reset function
void(*resetArd) (void) = 0; //declare reset function @ address 0

// Local Pin definition
#define PINEN 7 //Mux Enable pin
#define PINA0 4 //Mux Address 0 pin
#define PINA1 5 //Mux Address 1 pin
#define PINA2 6 //Mux Address 2 pin
#define PINSO 12 //TCAmp Slave Out pin (MISO)
#define PINSC 13 //TCAmp Serial Clock (SCK)
#define PINCS 9  //TCAmp Chip Select Change this to match the position of the Chip Select Link

// Internal variables
int Temp[8], SensorFail[8];
char failMode[8];
int internalTemp;
unsigned int Mask;
//char data[16];
int NumSensors = 8, UpdateDelay = 500;    
unsigned long time;
unsigned int cnt;

void setup()   
{     
  /*********************************************************************************************************/
  // This is to work with Leonardo ETH V2 (Compliant model)
  pinMode(ETHCS, OUTPUT);
  pinMode(ETHRST, OUTPUT);
  pinMode(SDCS, OUTPUT);
  digitalWrite(ETHRST, HIGH);
  digitalWrite(ETHCS, HIGH);
  digitalWrite(SDCS, LOW);
  /*********************************************************************************************************/


  // Start console
  Serial.begin(9600); 

  time = millis();
  cnt = -1;
  // Configure pins
  pinMode(PINEN, OUTPUT);     
  pinMode(PINA0, OUTPUT);    
  pinMode(PINA1, OUTPUT);    
  pinMode(PINA2, OUTPUT);    
  pinMode(PINSO, INPUT);    
  pinMode(PINCS, OUTPUT);    
  pinMode(PINSC, OUTPUT);    

  // Init pins
  digitalWrite(PINEN, HIGH);   // enable on
  digitalWrite(PINA0, LOW); // low, low, low = channel 1
  digitalWrite(PINA1, LOW); 
  digitalWrite(PINA2, LOW); 
  digitalWrite(PINSC, LOW); //put clock in low
  
  // start the Modbus server
  StartModbusServer();

}
void StartModbusServer()
{    
  Ethernet.begin(mac);
  // Init ModbusTCPSlave object
  modbus.begin();

  // Configure registers
  modbus.setHoldingRegisters(holdingRegisters, NB_HOLDING_REGISTERS);
  
  // Init registers
  for (int i = 0 ; i < NB_HOLDING_REGISTERS ; i++) 
     holdingRegisters[i] = 0x00;
}

void loop()                     
{
  /***********************************************************************************************************/
  // check DHCP lease
  Ethernet.maintain(); 
  /***********************************************************************************************************/
  // Check Reset Status
  /***********************************************************************************************************/
  if (holdingRegisters[ARD_RESET_ADDR] == 0x01)
    resetArd();
  /***********************************************************************************************************/
  if (millis() > (time + ((unsigned int)UpdateDelay)) || cnt == -1)
  {
    cnt = 0;
    time = millis();
    for(int j = 0 ; j < NumSensors ; j++)
    {
      switch (j) //select channel
      {
        case 0:
          digitalWrite(PINA0, LOW); 
          digitalWrite(PINA1, LOW); 
          digitalWrite(PINA2, LOW);
        break;
        case 1:
          digitalWrite(PINA0, HIGH); 
          digitalWrite(PINA1, LOW); 
          digitalWrite(PINA2, LOW);
        break;
        case 2:
          digitalWrite(PINA0, LOW); 
          digitalWrite(PINA1, HIGH); 
          digitalWrite(PINA2, LOW);
        break;
        case 3:
          digitalWrite(PINA0, HIGH); 
          digitalWrite(PINA1, HIGH); 
          digitalWrite(PINA2, LOW);
        break;
        case 4:
          digitalWrite(PINA0, LOW); 
          digitalWrite(PINA1, LOW); 
          digitalWrite(PINA2, HIGH);
        break;
        case 5:
          digitalWrite(PINA0, HIGH); 
          digitalWrite(PINA1, LOW); 
          digitalWrite(PINA2, HIGH);
        break;
        case 6:
          digitalWrite(PINA0, LOW); 
          digitalWrite(PINA1, HIGH); 
          digitalWrite(PINA2, HIGH);
        break;
        case 7:
          digitalWrite(PINA0, HIGH); 
          digitalWrite(PINA1, HIGH); 
          digitalWrite(PINA2, HIGH);
        break;
      }
      
      delay(10);
      digitalWrite(PINCS, LOW); //stop conversion
      delay(10);
      digitalWrite(PINCS, HIGH); //begin conversion
      delay(150);  //wait 100 ms for conversion to complete
      digitalWrite(PINCS, LOW); //stop conversion, start serial interface
      delay(10);
      
      Temp[j] = 0;
      failMode[j] = 0;
      SensorFail[j] = 0;
      internalTemp = 0;
      FloatUint16 mbValue;
      for (int i = 31 ; i >= 0 ; i--)
      {
          digitalWrite(PINSC, HIGH);
          delay(1);
          
           //print out bits
         #ifdef SHOWMEYOURBITS
         if (digitalRead(PINSO)==1)
          {
            Serial.print("1");
          }
          else
          {
            Serial.print("0");
          }
          #endif
          
        if ((i<=31) && (i>=18))
        {
          // these 14 bits are the thermocouple temperature data
          // bit 31 sign
          // bit 30 MSB = 2^10
          // bit 18 LSB = 2^-2 (0.25 degC)
          
          Mask = 1<<(i-18);
          if (digitalRead(PINSO)==1)
          {
            if (i == 31)
            {
              Temp[j] += (0b11<<14);//pad the temp with the bit 31 value so we can read negative values correctly
            }
            Temp[j] += Mask;
            //Serial.print("1");
          }
          else
          {
           // Serial.print("0");
          }
        }
        //bit 17 is reserved
        //bit 16 is sensor fault
        if (i==16)
        {
          SensorFail[j] = digitalRead(PINSO);
        }
        
        if ((i<=15) && (i>=4))
        {
          //these 12 bits are the internal temp of the chip
          //bit 15 sign
          //bit 14 MSB = 2^6
          //bit 4 LSB = 2^-4 (0.0625 degC)
          Mask = 1<<(i-4);
          if (digitalRead(PINSO)==1)
          {
            if (i == 15)
            {
              internalTemp += (0b1111<<12);//pad the temp with the bit 31 value so we can read negative values correctly
            }
            
            internalTemp += Mask;//should probably pad the temp with the bit 15 value so we can read negative values correctly
            //Serial.print("1");
          }
          else
          {
           // Serial.print("0");
          }
          
        }
        //bit 3 is reserved
        if (i==2)
        {
          failMode[j] += digitalRead(PINSO)<<2;//bit 2 is set if shorted to VCC
        }
        if (i==1)
        {
          failMode[j] += digitalRead(PINSO)<<1;//bit 1 is set if shorted to GND
        }
        if (i==0)
        {
          failMode[j] += digitalRead(PINSO)<<0;//bit 0 is set if open circuit
        }
        
        
        digitalWrite(PINSC, LOW);
        delay(1);
      }
      //Serial.println();
    
      //Serial.println(Temp,BIN);
      Serial.print("#");
      Serial.print(j+1,DEC);
      Serial.print(": ");
      if (SensorFail[j] == 1)
      {
        Serial.print("FAIL");
        if ((failMode[j] & 0b0100) == 0b0100)
        {
          Serial.println(" SHORT TO VCC");
        }
        if ((failMode[j] & 0b0010) == 0b0010)
        {
          Serial.println(" SHORT TO GND");
        }
        if ((failMode[j] & 0b0001) == 0b0001)
        {
          Serial.println(" OPEN CIRCUIT");
        }
        // Write to modbus regs
        mbValue.fvalue = temp[j]; // keep old temp value
      }
      else
      {
        double newTemp = (float)Temp[j] * 0.25;
        newTemp-=tempFactor;
        if ( (abs(newTemp - temp[j]) > tempDelta) && temp[j] != -99)
           mbValue.fvalue = temp[j]; // keep old temp
        else {
           mbValue.fvalue = newTemp;
           temp[j] = newTemp;
        }
        Serial.print(mbValue.fvalue,2);
        Serial.println(" degC");
     
      }
      int index = j*2;
      holdingRegisters[index] = mbValue.value[0];
      Serial.print(" register ");Serial.print(index);Serial.print(" = ");Serial.println(mbValue.value[0]);
      holdingRegisters[++index] = mbValue.value[1];
      Serial.print(" register ");Serial.print(index);Serial.print(" = ");Serial.println(mbValue.value[1]);
      //Pause between 2 measures
      delay(10);
      // Process modbus requests
      //modbus.update();
    }//end reading sensors
    FloatUint16 mbValue;
    //Serial.println("");
    Serial.print("#9: (Int) ");
    double newTemp = (float)internalTemp * 0.0625;
    newTemp-=tempFactor;
    if ( (abs(newTemp - temp[8]) > tempDelta) && temp[8] != -99)
       mbValue.fvalue = temp[8]; // keep old temp
    else {
       mbValue.fvalue = newTemp;
       temp[8] = newTemp;
    }
    Serial.print(mbValue.fvalue,4);
    Serial.print(" degC");
    Serial.println("");
    
    holdingRegisters[TEMP_INT_ADDR] = mbValue.value[0];
    Serial.print(" register ");Serial.print(TEMP_INT_ADDR);Serial.print(" = ");Serial.println(mbValue.value[0]);
    holdingRegisters[TEMP_INT_ADDR+1] = mbValue.value[1];
    Serial.print(" register ");Serial.print(TEMP_INT_ADDR+1);Serial.print(" = ");Serial.println(mbValue.value[1]);
    // Process modbus requests
    modbus.update();
  }//end time
  else {
      Serial.print("waiting..");Serial.println(++cnt);
      delay(1000);
      // Process modbus requests
      modbus.update();
  }
}
