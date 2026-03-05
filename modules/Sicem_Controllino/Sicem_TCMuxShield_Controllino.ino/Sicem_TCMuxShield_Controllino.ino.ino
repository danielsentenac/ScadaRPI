/*
  Modbus Server
 A Sicem Module with TCMuxshield and Controllino board
 */
#include <Wire.h>        // I2C library
#include <Controllino.h> // Controllino library
#include <CRC32.h>       // Checksum library
#include <SoftwareSerial.h>

// Conversion tool

typedef union {
    float fvalue;
    uint32_t i32value;
  } FloatUint32;
  
/*
 * I2C ADDRESS (SLAVE)
 */ 
#define I2C_SLAVE_ADDR 0x09
/* 
 * I2C BUFFER SIZE
 */
#define I2C_BUFFER 32
/*
 * Variables used in interrupted handlers
 */
volatile uint32_t i2c_buffer = 0; 
volatile byte data_array[9]; // contains i2c_buffer and Sicem new setpoint position and value
volatile byte crc_array[4];
volatile boolean updateIOFromI2CBool = false;
volatile FloatUint32 sensorTemp[9]; // The TCMuxShield temperature array (8 sensor tempertures + internal temperature)
volatile FloatUint32 ModuleTemp[12]; // Sicem Arrays to store module temperature values
volatile FloatUint32 ModuleSetpoint[12]; // Sicem Arrays to store module temperature values
volatile int posF = -1; // init with no update
volatile int chunkNumber = 0;  // The data chunck number to release in requestEvent

#define SICEMSerial  Serial2 // Serial2 is hardware serial for Controllino MAXI (RXD2, TXD2)
SoftwareSerial RS232Serial(CONTROLLINO_D10, CONTROLLINO_PIN_HEADER_DIGITAL_OUT_14); // RX, TX for Controllino (Raspberry Com)


/*
 * The time for Check and Reset actions
 */
unsigned long CHANNEL_1time = 0;
unsigned long CHANNEL_2time = 0;
unsigned long looptime = 0;

/*
 *  These RESET are used to reset switches (Command outputs)
 */
boolean CHANNEL_1_RESET = false;
boolean CHANNEL_2_RESET = false;

/*
 *  The waiting time before ressetting switches command
 */
long reset_wait = 2000;

/*
*  These CHECK are used to check switches status (Status inputs)
*/
boolean CHANNEL_1_CHECK = false;
boolean CHANNEL_2_CHECK = false;

/*
 *  The waiting time before checking switches status
 */
long check_wait = 10000;

// relay pins
#define CHANNEL_1_ON_CMD  CONTROLLINO_R0  // Channel 1 SWITCH ON COMMAND
#define CHANNEL_1_OFF_CMD CONTROLLINO_R1  // Channel 1 SWITCH OFF COMMAND
#define CHANNEL_2_ON_CMD  CONTROLLINO_R2  // Channel 2 SWITCH ON COMMAND
#define CHANNEL_2_OFF_CMD CONTROLLINO_R3  // Channel 2 SWITCH OFF COMMAND

// digital pins (INPUTS)

#define CHANNEL_1_ON_STATUS   CONTROLLINO_A0     // Channel 1 ON  STATUS
#define CHANNEL_1_OFF_STATUS  CONTROLLINO_A1     // Channel 1 OFF STATUS
#define CHANNEL_2_ON_STATUS   CONTROLLINO_A2     // Channel 2 ON  STATUS
#define CHANNEL_2_OFF_STATUS  CONTROLLINO_A3     // Channel 3 OFF STATUS
#define SICEM_ALRM_STATUS     CONTROLLINO_A4   // SICEM GLOBAL ALARM STATUS

// TCMuxShield local pin definition

#define PINEN 8   //Mux Enable pin
#define PINA0 9  //Mux Address 0 pin
#define PINA1 10  //Mux Address 1 pin
#define PINA2 11  //Mux Address 2 pin
#define PINSO 13  //TCAmp Slave Out pin (MISO)
#define PINSC 42  //TCAmp Serial Clock (SCK)
#define PINCS 43  //TCAmp Chip Select Change this to match the position of the Chip Select Link

/*
 *  I2C BIT ASSIGNATION (I2C_BUFFER; MAX = 256) 
 */
#define CHANNEL_1_ON_STATUS_BIT       0                         // Channel 1 On Status bit
#define CHANNEL_1_OFF_STATUS_BIT      1                         // Channel 1 Off Status bit
#define CHANNEL_2_ON_STATUS_BIT       2                         // Channel 2 On Status bit
#define CHANNEL_2_OFF_STATUS_BIT      3                         // Channel 2 Off Status bit
#define CHANNEL_1_ON_CMD_BIT          4                         // Channel 1 On CMD bit
#define CHANNEL_1_OFF_CMD_BIT         5                         // Channel 1 Off CMD bit
#define CHANNEL_2_ON_CMD_BIT          6                         // Channel 2 On CMD bit
#define CHANNEL_2_OFF_CMD_BIT         7                         // Channel 2 Off CMD bit
#define SICEM_ALRM_STATUS_BIT         8                         // SICEM GLOBAL ALARM STATUS bit
#define SICEM_COMERR_BIT              9                         // SICEM COMM ERR STATUS
#define MUXSHIELD_COMERR_BIT          10                        // MUXSHIELD COMM ERR STATUS
#define ARD_RESET_BIT                 11                        // Controllino Reset Bit

void(*resetArd) (void) = 0; //declare reset function @ address 0

// Buffer for Serial purposes
size_t length = 64;
char *buffer = (char*)malloc(sizeof(char) * length); 

// TCMuxShieldMux variables
int rawTemp[8], sensorFail[8];
char failMode[8];
int internalTemp;
unsigned int mask;
//char data[16];
int numSensors = 8, updateDelay = 500;    
unsigned long TCMuxtime;
unsigned int cnt;
// Normalization offset & Temperature 
#define tempOffset 3
#define tempDelta 3


void setup() {

    // Open Serial communication for Console port.
   Serial.begin(9600);

   Serial.println("START SETUP");
   
   InitializeIO();
   InitializeI2C();

   // Configure TCMuxShield pins and init variables
  TCMuxtime = millis();
  cnt = -1;   
  pinMode(PINEN, OUTPUT);     
  pinMode(PINA0, OUTPUT);    
  pinMode(PINA1, OUTPUT);    
  pinMode(PINA2, OUTPUT);    
  pinMode(PINSO, INPUT);    
  pinMode(PINCS, OUTPUT);    
  pinMode(PINSC, OUTPUT);    

  // Init TCMuxShield pins
  
  digitalWrite(PINEN, HIGH);   // enable on
  digitalWrite(PINA0, LOW); // low, low, low = channel 1
  digitalWrite(PINA1, LOW); 
  digitalWrite(PINA2, LOW); 
  digitalWrite(PINSC, LOW); //put clock in low

  //Initialize TCMuxShield Temperature array values
  
  for (int i = 0; i < 9 ; i++) 
    sensorTemp[i].fvalue = -99;

  for (int i = 0; i < 12 ; i++) 
    ModuleTemp[i].fvalue = -99;

  for (int i = 0; i < 12 ; i++) 
    ModuleSetpoint[i].fvalue = -99;

  // Init Wire
  Wire.begin(I2C_SLAVE_ADDR);
  Wire.setTimeout(1000);

  // Open serial communication for SICEM
  SICEMSerial.begin(4800);
  SICEMSerial.setTimeout(1000);

  // Open serial communication for Raspberry
  RS232Serial.begin(9600);
  RS232Serial.setTimeout(1000);

}
void InitializeI2C()
{
  Serial.println("InitializeI2C...");
  
  Wire.onReceive(receiveEvent); // register receive event (i2c_buffer sent by master)  
  Wire.onRequest(requestEvent); // register request event (i2c_buffer sent by slave)  
  
  Serial.println("Done.");
}

void InitializeIO()
{    
  Serial.println("InitializeIO...");
  
  // Digital OUTPUTS assignation & initialization
  digitalWrite(CHANNEL_1_ON_CMD,LOW);        // Set CHANNEL_1_ON_CMD LOW
  digitalWrite(CHANNEL_1_OFF_CMD,HIGH);      // Set CHANNEL_1_OFF_CMD HIGH
  pinMode(CHANNEL_1_ON_CMD, OUTPUT);         // Set the digital pin as output for Channel 1 ON
  pinMode(CHANNEL_1_OFF_CMD, OUTPUT);        // Set the digital pin as output for Channel 1 OFF
  I2CsetBit(CHANNEL_1_ON_CMD_BIT,0x00);      // Set CHANNEL_1_ON_CMD_BIT LOW
  I2CsetBit(CHANNEL_1_OFF_CMD_BIT,0x01);     // Set CHANNEL_1_OFF_CMD_BIT HIGH
  digitalWrite(CHANNEL_2_ON_CMD,LOW);        // Set CHANNEL_2_ON_CMD LOW
  digitalWrite(CHANNEL_2_OFF_CMD,HIGH);      // Set CHANNEL_2_OFF_CMD HIGH
  pinMode(CHANNEL_2_ON_CMD, OUTPUT);         // Set the digital pin as output for Channel 2 ON
  pinMode(CHANNEL_2_OFF_CMD, OUTPUT);        // Set the digital pin as output for Channel 2 OFF
  I2CsetBit(CHANNEL_2_ON_CMD_BIT,0x00);      // Set CHANNEL_2_ON_CMD_BIT LOW
  I2CsetBit(CHANNEL_2_OFF_CMD_BIT,0x01);     // Set CHANNEL_2_OFF_CMD_BIT HIGH
  
  // Digital INPUTS assignation
  pinMode(CHANNEL_1_ON_STATUS, INPUT);     // sets the digital pin as input for Channel 1 ON STATUS
  pinMode(CHANNEL_1_OFF_STATUS, INPUT);    // sets the digital pin as input for Channel 1 OFF STATUS
  pinMode(CHANNEL_2_ON_STATUS, INPUT);     // sets the digital pin as input for Channel 2 ON STATUS
  pinMode(CHANNEL_2_OFF_STATUS, INPUT);    // sets the digital pin as input for Channel 2 OFF STATUS
  pinMode(SICEM_ALRM_STATUS, INPUT);       // sets the digital pin as input for Sicem Alarm STATUS

  Serial.println("Done.");
}
void ListenToRaspberry() {
      //Serial.println("ListenToRaspberry..");
      RS232Serial.listen();
      String request = RS232Serial.readString();
      //Serial.println(request);
      if (request.substring(0,7).compareTo("getdata") == 0) {
         //Serial.print("Sending data Sicem...");
         // Sicem Temp
         String data = "";
         for ( int i = 1; i <= 12 ; i++) {
            data+=String(ModuleTemp[i-1].fvalue,2);
            data+=";";
         }
         // Sicem Setpoint
         for ( int i = 1; i <= 12 ; i++) {
            data+=String(ModuleSetpoint[i-1].fvalue,2);
            data+=";";
         }
         // TCMux Temp
         for ( int i = 1; i <= 9 ; i++) {
            data+=String(sensorTemp[i-1].fvalue,2);
            data+=";";
         }
         // Write data to the wire
         //Serial.println(data);
         RS232Serial.print(data);
      }
      else if ( request.substring(0,4).compareTo("SETP") == 0 ) {
        Serial.print("Update Setpoint: pos=");
        Serial.print(request.substring(4,6).toInt());
        Serial.print("; value=");
        Serial.println(request.substring(7,10).toInt());
        // SET POINT change requested
        // Extract position and value
        int pos = request.substring(4,6).toInt();
        float value = request.substring(7,10).toFloat();
        // Set new setpoint
        ModuleSetpoint[pos-1].fvalue = value;
        // Update Setpoint
        updateSetpoint(pos);
      }
}
void UpdateI2C() {

  if (updateIOFromI2CBool == true) {
    Serial.print("Received command; updated i2c_buffer =");
    Serial.println(i2c_buffer,BIN);
    updateIOFromI2CBool = false;
    // Update I/O from updated i2c_buffer
    UpdateIOFromI2C();
  }
  
  // Update i2c_buffer from I/O
  UpdateI2CFromIO();
  
  ResetAndCheck();
  
}
void loop() {
  //Serial.println("loop started");
  // For debug purpose
  if ( millis() - looptime > reset_wait) {
     Serial.print("i2c_buffer=");
     Serial.println(i2c_buffer,BIN);
     looptime = millis();
  }

    /***********************************************************************************************************/
    // Get TCMuxShield sensor Temperature array
    /***********************************************************************************************************/
    getTCMuxShieldTemp();

   /***********************************************************************************************************/
  /*  Serial SICEM data / Update Modbus register values */
  /**********************************************************************************************************/
  const char * temp[] = { "s11","s12","s13","s14","s15","s16","s17","s18","s19","s1A","s1B","s1C"};
  const char * setp[] = { "i11","i12","i13","i14","i15","i16","i17","i18","i19","i1A","i1B","i1C"};

  boolean err_com = true;
  for (int i = 1 ; i <= 24 ; i++) {
    // First get message from Raspberry
    ListenToRaspberry();
    if ( millis() - looptime > reset_wait) {
     Serial.print("i2c_buffer=");
     Serial.println(i2c_buffer,BIN);
     looptime = millis();
  }
   // treat external I2C commands
    UpdateI2C();
    String serData3,serData4 = "";
    unsigned char data[64];
    data[0] = 0x02; // STX
    int csum = data[0];
    int pos = 0;
    if ( i <= 12 ) {
     pos = i;
     for (int k = 0 ; k < 3 ; k++) {
      data[k+1] = temp[pos-1][k]; // TEMP
      csum+=data[k+1];
     }
    }
    else {      
      pos = i - 12;
      for (int k = 0 ; k < 3 ; k++) {
      data[k+1] = setp[pos-1][k]; // SETPOINT
      csum+=data[k+1];
     }
    }
    data[4] = 0x03; //ETX
    csum+=data[4];
    //Serial.print("csum=");Serial.println(csum);
    data[5] = ((csum & 255) | 128);
    serData3 = (char*)data;
    
#if defined(SICEMSerialIsSoftware)
     SICEMSerial.();
#endif
    /*for (int l = 0 ; l < 7;l++){
       Serial.print(data[l]);
       Serial.print(",");
    }
    Serial.println("");*/
    SICEMSerial.print(serData3);
    memset(buffer,0,length);
    SICEMSerial.readBytes(buffer,length);
    serData4 = buffer;
    SICEMSerial.flush();
    //Serial.print("Interogate Sicem for module ");Serial.println(pos);
    //Serial.print("Sicem resp=");Serial.print(serData4);Serial.println("END");
    if (serData4 == "") {// Com Status Error: treat only as if module is absent!
      //Serial.print("No answer from Sicem for module ");Serial.println(pos);
      if ( i <= 12 ) ModuleTemp[pos-1].fvalue = -99;
      else if ( i > 12 ) ModuleSetpoint[pos-1].fvalue = -99; // reset setpoint locally
      continue;
    }
    else { // Data OK
      err_com = false;
      //Serial.print("Data OK: Sicem resp=");Serial.print(serData4);Serial.println("END");
      csum = 0; // Calculate checksum
      for (int k = 0 ; k < serData4.length() - 1; k++)
        csum+= serData4[k];
      if ( ((serData4[serData4.length() -1] & 255) | 128) == ((csum & 255) | 128) ) { // checksum OK
        if ( i <= 12 & serData4.length() > 11) { // Temperature case
           String val = serData4.substring(4,10);
           int32_t decvalue;
           decvalue = strtol(val.c_str(),NULL,16);
           float temp = float(decvalue) / 4096.;
           ModuleTemp[pos-1].fvalue = temp;
           //Serial.print("TEMP[");Serial.print(pos);Serial.print("]=");
           //Serial.println(ModuleTemp[pos-1].fvalue);
        }
        else if ( i > 12 & serData4.length() > 8 ){ // Setpoint case
           String val = serData4.substring(4,7);
           int32_t setpoint = strtol(val.c_str(),NULL,16);
           ModuleSetpoint[pos-1].fvalue = setpoint; // Save setpoint locally
           //Serial.print("SETPOINT[");Serial.print(pos);Serial.print("]=");
           //Serial.println(ModuleSetpoint[pos-1].fvalue);
        }
      } 
    }
  }
  if ( err_com == false )
     I2CsetBit(SICEM_COMERR_BIT,0x00); // UPDATE SICEM COMM ERROR BIT
  else
      I2CsetBit(SICEM_COMERR_BIT,0x01); // UPDATE SICEM COMM ERROR BIT
     
  Serial.println("loop finished");
}
void updateSetpoint(int pos) {
   /***********************************************************************************************************/
   /* Update SICEM SETPOINT */
   /***********************************************************************************************************/
   int32_t setpoint = (int)(ModuleSetpoint[pos-1].fvalue);
    
   Serial.print("Update ModuleSetPoint[");Serial.print(pos-1);Serial.print("]=");Serial.println(ModuleSetpoint[pos-1].fvalue);
   String takecmd,changesetpoint,releasecmd,answer;
   // Take command priority
   String modulename = String(pos,HEX);
   modulename.toUpperCase();
   takecmd = "h1"+ modulename + "01";
   SendSicemRequest(takecmd,&answer);
   // Do the setpoint change
   String hexSetpointVal = String((int)setpoint,HEX); // New setpoint is an int
   switch ((int)hexSetpointVal.length()) {
      case 1:
        hexSetpointVal = "00" + hexSetpointVal;
        break;
      case 2:
         hexSetpointVal = "0" + hexSetpointVal;
         break;
      default:
         break;
   }
   String modulenamevalue = String(pos,HEX) + hexSetpointVal;
   modulenamevalue.toUpperCase();
   changesetpoint = "i1" + modulenamevalue + "2";
   SendSicemRequest(changesetpoint,&answer);
        
   // Release command priority
   releasecmd = "h1"+ modulename + "00";
   SendSicemRequest(releasecmd,&answer);
          
}

int SendSicemRequest(String request,String *answer) {
   unsigned char data[64];
   String unformattedData="";;
   String msg="";
   data[0] = 0x02; // STX
   int csum = data[0];
   for (int i = 0; i < (int) request.length(); i++) {
      data[i+1] = request[i];
      csum+=data[i+1];
   }
   data[(int)request.length()+1] = 0x03; //ETX
   csum+=3;
   data[(int)request.length()+2] = ((csum & 255) | 128);
   data[(int)request.length()+3] = '\0';
   msg = (char*)data;
#if defined(SICEMSerialIsSoftware)
     SICEMSerial.listen();
#endif
   //Serial.println(msg);    
   SICEMSerial.print(msg);
   memset(buffer,0,length);
   SICEMSerial.readBytes(buffer,length);
   unformattedData = buffer;
   SICEMSerial.flush();
   //Serial.print("Sicem Request resp=");Serial.print(unformattedData);Serial.println("END");
   csum = 0;
   for (int i = 0; i < (int)unformattedData.length() - 1 ; i++)  {
     csum+=unformattedData[i];
   }
   if (((unformattedData[unformattedData.length() -1] & 255) | 128) != ((csum & 255) | 128))  {
     //Serial.println("Wrong Checksum");
     return (-1); 
    }
   *answer = unformattedData; 
   return 0;
}
void requestEvent() {
  
  // Send i2c_buffer to master (create 8 bytes array, including checksum)
  byte i2c_array[8];
  
  i2c_array[0] = (i2c_buffer >> 24) & 0xFF;
  i2c_array[1] = (i2c_buffer >> 16) & 0xFF;
  i2c_array[2] = (i2c_buffer >> 8) & 0xFF;
  i2c_array[3] = i2c_buffer & 0xFF;
     
  // Calculate Checksum
  uint32_t crc = CRC32::calculate(i2c_array, 4);

  i2c_array[4] = (crc >> 24) & 0xFF;
  i2c_array[5] = (crc >> 16) & 0xFF;
  i2c_array[6] = (crc >> 8) & 0xFF;
  i2c_array[7] = crc & 0xFF;
  
  Wire.write(i2c_array, 8);

}
void receiveEvent(int numbyte) {

  // Update i2c_buffer from master (13 bytes)
  while (Wire.available()) {
    if (numbyte == 8) { // Expect 8 bytes of data (including CRC32), update i2c_buffer
      // i2c_buffer
      data_array[0] = Wire.read();
      data_array[1] = Wire.read();
      data_array[2] = Wire.read();
      data_array[3] = Wire.read();
    
      // last 4 bytes of data corresponds to CRC32
      crc_array[0] = Wire.read();
      crc_array[1] = Wire.read();
      crc_array[2] = Wire.read();
      crc_array[3] = Wire.read();

      uint32_t crc;
      crc = crc_array[0];
      crc = (crc << 8) | crc_array[1];
      crc = (crc << 8) | crc_array[2];
      crc = (crc << 8) | crc_array[3];

      // calculate CRC32
      uint32_t crcdata = CRC32::calculate(data_array, 4);

      //Serial.print(" ReceiveEvent:Checksum:");Serial.print(crc);Serial.print(" <--> ");Serial.println(crcdata);
      //Serial.print(" ReceiveEvent:pos:");Serial.println((int)data_array[4]);
      if ( crc == crcdata ) {
        //  Serial.print(" ReceiveEvent:Checksum good:");Serial.println(crc);
        //  Serial.print(" ReceiveEvent:pos:");Serial.println(data_array[4]);
        // Update i2c_buffer in the interrupted section
        i2c_buffer = data_array[0];
        i2c_buffer = (i2c_buffer << 8) | data_array[1];
        i2c_buffer = (i2c_buffer << 8) | data_array[2];
        i2c_buffer = (i2c_buffer << 8) | data_array[3];

        //Serial.print(" ReceiveEvent::data 5-6-7-8 = ");Serial.print(data_array[5]);Serial.print(" ");
        //Serial.print(data_array[6]);Serial.print(" ");Serial.print(data_array[7]);Serial.print(" ");
        //Serial.print(data_array[8]);

       
        // UpdateIOFromI2C in the loop
        updateIOFromI2CBool = true;   
      }
    }
    else {// Flush the wrong Wire buffer
      while(Wire.available()) Wire.read();
    }
  }
}

void I2CsetBit(int bit, int value) {
  if (bit < I2C_BUFFER) {
    if (value == 1)
      bitSet(i2c_buffer,bit);
    else if (value == 0)
      bitClear(i2c_buffer,bit);
  }
}

void ResetAndCheck() {
  /*
   *  CHANNEL_1 Switch ON/OFF Case
   */
  // Reset CHANNEL_1_OFF_CMD
  if (digitalRead(CHANNEL_1_OFF_CMD) == LOW && CHANNEL_1_RESET == true) {
    if ( millis() - CHANNEL_1time > reset_wait) {
       digitalWrite(CHANNEL_1_OFF_CMD,HIGH);  // RESET CHANNEL 1 OFF CMD
       I2CsetBit(CHANNEL_1_OFF_CMD_BIT,0x01); // RESET CHANNEL 1 OFF CMD BIT
       CHANNEL_1_RESET = false;  
    }
  }
  // Reset CHANNEL_1_ON_CMD
  if (digitalRead(CHANNEL_1_ON_CMD) == HIGH && CHANNEL_1_RESET == true) {
    if ( millis() - CHANNEL_1time > reset_wait) {
       digitalWrite(CHANNEL_1_ON_CMD,LOW);   // RESET CHANNEL 1 ON CMD
       I2CsetBit(CHANNEL_1_ON_CMD_BIT,0x00); // RESET CHANNEL 1 ON CMD BIT
       CHANNEL_1_RESET = false;
    }
  }
  /*
   *  CHANNEL_2 Switch ON/OFF Case
   */
  // Reset CHANNEL_2_OFF_CMD
  if (digitalRead(CHANNEL_2_OFF_CMD) == LOW && CHANNEL_2_RESET == true) {
    if ( millis() - CHANNEL_2time > reset_wait) {
       digitalWrite(CHANNEL_2_OFF_CMD,HIGH);  // RESET CHANNEL 2 OFF CMD
       I2CsetBit(CHANNEL_2_OFF_CMD_BIT,0x01); // RESET CHANNEL 2 OFF CMD BIT
       CHANNEL_2_RESET = false;  
    }
  }
  // Reset CHANNEL_2_ON_CMD
  if (digitalRead(CHANNEL_2_ON_CMD) == HIGH && CHANNEL_2_RESET == true) {
    if ( millis() - CHANNEL_2time > reset_wait) {
       digitalWrite(CHANNEL_2_ON_CMD,LOW);   // RESET CHANNEL 2 ON CMD
       I2CsetBit(CHANNEL_2_ON_CMD_BIT,0x00); // RESET CHANNEL 2 ON CMD BIT
       CHANNEL_2_RESET = false;
    }
  }
  
  /***********************************************************************************************************/
  // Check Controllino Reset Status
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,ARD_RESET_BIT) == 0x01)
    resetArd();
  /***********************************************************************************************************/
}

void UpdateIOFromI2C()
{    
  /***********************************************************************************************************/
  /* Update Channel 1 position (On/Off) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,CHANNEL_1_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer,CHANNEL_1_ON_CMD_BIT) == 0x00 && CHANNEL_1_RESET == false) {
     digitalWrite(CHANNEL_1_OFF_CMD,LOW);   // SWITCH OFF CHANNEL 1
     CHANNEL_1time = millis();
     CHANNEL_1_RESET = true;
  }
  else if (bitRead(i2c_buffer,CHANNEL_1_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,CHANNEL_1_OFF_CMD_BIT) == 0x01 && CHANNEL_1_RESET == false) {
     digitalWrite(CHANNEL_1_ON_CMD,HIGH);   // SWITCH ON CHANNEL 1
     CHANNEL_1time = millis();
     CHANNEL_1_RESET = true;
  }

  /***********************************************************************************************************/
  /* Update Channel 2 position (On/Off) */
  /***********************************************************************************************************/
  if (bitRead(i2c_buffer,CHANNEL_2_OFF_CMD_BIT) == 0x00 && bitRead(i2c_buffer,CHANNEL_2_ON_CMD_BIT) == 0x00 && CHANNEL_2_RESET == false) {
     digitalWrite(CHANNEL_2_OFF_CMD,LOW);   // SWITCH OFF CHANNEL 2
     CHANNEL_2time = millis();
     CHANNEL_2_RESET = true;
  }
  else if (bitRead(i2c_buffer,CHANNEL_2_ON_CMD_BIT) == 0x01 && bitRead(i2c_buffer,CHANNEL_2_OFF_CMD_BIT) == 0x01 && CHANNEL_2_RESET == false) {
     digitalWrite(CHANNEL_2_ON_CMD,HIGH);   // SWITCH ON CHANNEL 2
     CHANNEL_2time = millis();
     CHANNEL_2_RESET = true;
  }

}

void UpdateI2CFromIO()
{ 
  /***********************************************************************************************************/
  /* Update CHANNEL 1 position STATUS bit (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(CHANNEL_1_ON_STATUS) == HIGH && digitalRead(CHANNEL_1_OFF_STATUS) == LOW) { // CHANNEL 1 ON STATUS
     I2CsetBit(CHANNEL_1_ON_STATUS_BIT,0x01);   // UPDATE CHANNEL 1 ON  BIT
     I2CsetBit(CHANNEL_1_OFF_STATUS_BIT,0x00);   // UPDATE CHANNEL 1 OFF  BIT
  }
  else if (digitalRead(CHANNEL_1_OFF_STATUS) == HIGH && digitalRead(CHANNEL_1_ON_STATUS) == LOW) { // CHANNEL 1 OFF STATUS
     I2CsetBit(CHANNEL_1_ON_STATUS_BIT,0x00);   // UPDATE CHANNEL 1 ON BIT
     I2CsetBit(CHANNEL_1_OFF_STATUS_BIT,0x01);  // UPDATE CHANNEL 1 OFF BIT
  }
  else {
     // ERROR STATE
     I2CsetBit(CHANNEL_1_ON_STATUS_BIT,0x00);   // UPDATE CHANNEL 1 ON BIT
     I2CsetBit(CHANNEL_1_OFF_STATUS_BIT,0x00);  // UPDATE CHANNEL 1 OFF BIT
  }
  /***********************************************************************************************************/
  /* Update CHANNEL 2 position STATUS bit (ON/OFF) */
  /***********************************************************************************************************/
  if (digitalRead(CHANNEL_2_ON_STATUS) == HIGH && digitalRead(CHANNEL_2_OFF_STATUS) == LOW) { // CHANNEL 2 ON STATUS
     I2CsetBit(CHANNEL_2_ON_STATUS_BIT,0x01);   // UPDATE CHANNEL 2 ON  BIT
     I2CsetBit(CHANNEL_2_OFF_STATUS_BIT,0x00);   // UPDATE CHANNEL 2 OFF  BIT
  }
  else if (digitalRead(CHANNEL_2_OFF_STATUS) == HIGH && digitalRead(CHANNEL_2_ON_STATUS) == LOW) { // CHANNEL 2 OFF STATUS
     I2CsetBit(CHANNEL_2_ON_STATUS_BIT,0x00);   // UPDATE CHANNEL 2 ON BIT
     I2CsetBit(CHANNEL_2_OFF_STATUS_BIT,0x01);  // UPDATE CHANNEL 2 OFF BIT
  }
  else {
     // ERROR STATE
     I2CsetBit(CHANNEL_2_ON_STATUS_BIT,0x00);   // UPDATE CHANNEL 2 ON BIT
     I2CsetBit(CHANNEL_2_OFF_STATUS_BIT,0x00);  // UPDATE CHANNEL 2 OFF BIT
  }
  
  /***********************************************************************************************************/
  /* Update SICEM Error status bit (OK/KO) */
  /***********************************************************************************************************/
  if (digitalRead(SICEM_ALRM_STATUS) == HIGH) {     // SICEM ALRM KO STATUS
     I2CsetBit(SICEM_ALRM_STATUS_BIT,0x01);         // UPDATE SICEM_ALRM_STATUS KO BIT
  }
  else if (digitalRead(SICEM_ALRM_STATUS) == LOW) {  // SICEM ALRM OK STATUS
     I2CsetBit(SICEM_ALRM_STATUS_BIT,0x00);          // SICEM ALRM OK BIT
  }
 
}
void getTCMuxShieldTemp() 
{
  if (millis() > (TCMuxtime + ((unsigned int)updateDelay)) || cnt == -1)
  {
    cnt = 0;
    TCMuxtime = millis();
    for(int j = 0 ; j < numSensors ; j++)
    {
      // First get message from Raspberry
      //ListenToRaspberry();
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
      
      rawTemp[j] = 0;
      failMode[j] = 0;
      sensorFail[j] = 0;
      internalTemp = 0;

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
          // these 14 bits are the thermocouple sensorTemperature data
          // bit 31 sign
          // bit 30 MSB = 2^10
          // bit 18 LSB = 2^-2 (0.25 degC)
          
          mask = 1<<(i-18);
          if (digitalRead(PINSO)==1)
          {
            if (i == 31)
            {
              rawTemp[j] += (0b11<<14);//pad the sensorTemp with the bit 31 value so we can read negative values correctly
            }
            rawTemp[j] += mask;
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
          sensorFail[j] = digitalRead(PINSO);
        }
        
        if ((i<=15) && (i>=4))
        {
          //these 12 bits are the internal sensorTemp of the chip
          //bit 15 sign
          //bit 14 MSB = 2^6
          //bit 4 LSB = 2^-4 (0.0625 degC)
          mask = 1<<(i-4);
          if (digitalRead(PINSO)==1)
          {
            if (i == 15)
            {
              internalTemp += (0b1111<<12);//pad the sensorTemp with the bit 31 value so we can read negative values correctly
            }
            
            internalTemp += mask;//should probably pad the sensorTemp with the bit 15 value so we can read negative values correctly
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
      //Serial.println(rawTemp[j],BIN);
      //Serial.print("#");
      //Serial.print(j+1,DEC);
      //Serial.print(": ");
      if (sensorFail[j] == 1)
      {
        //Serial.print("FAIL");
        if ((failMode[j] & 0b0100) == 0b0100)
        {
          //Serial.println(" SHORT TO VCC");
        }
        if ((failMode[j] & 0b0010) == 0b0010)
        {
          //Serial.println(" SHORT TO GND");
        }
        if ((failMode[j] & 0b0001) == 0b0001)
        {
          //Serial.println(" OPEN CIRCUIT");
        }
      }
      else
      {
        double newTemp = (float)rawTemp[j] * 0.25;
        newTemp-=tempOffset;
        if ( (abs(newTemp - sensorTemp[j].fvalue) > tempDelta) || sensorTemp[j].fvalue == -99)
           // Update sensorTemperature value
           sensorTemp[j].fvalue = newTemp;
        Serial.print(sensorTemp[j].fvalue,2);
        Serial.println(" degC");
        Serial.println("");
      }
      //Pause between 2 measures
      delay(10);
    }
    //end reading sensors
    // Treating internal Temperature
    Serial.print("#9: (Int) ");
    double newTemp = (float)internalTemp * 0.0625;
    newTemp-=tempOffset;
    if ( (abs(newTemp - sensorTemp[8].fvalue) > tempDelta) || sensorTemp[8].fvalue == -99)
       // Update internal Temperature value
       sensorTemp[8].fvalue = newTemp;
    Serial.print(sensorTemp[8].fvalue,2);
    Serial.println(" degC");
    Serial.println("");
    I2CsetBit(MUXSHIELD_COMERR_BIT,0x00); // UPDATE SICEM COMM ERROR BIT
  }
}
