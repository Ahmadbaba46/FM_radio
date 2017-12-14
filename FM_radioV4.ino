/******************************************************************
 Project     : FM radio
 Date        : 31.05.2016 20:40:16
 Libraries   :
 Author      : Florin Nica
 Description : FM radio cu bluetooth
******************************************************************/
/// Arduino port | SI4703 signal | RDA5807M signal
/// :----------: | :-----------: | :-------------:
///          GND | GND           | GND   
///         3.3V | VCC           | -
///           5V | -             | VCC
///           A5 | SCLK          | SCLK
///           A4 | SDIO          | SDIO
///           D2 | RST           | -




#include <Arduino.h>
#include <avr/eeprom.h>
#include <Wire.h>
#include <LiquidCrystal.h>
#include <radio.h>
#include <SI4703.h>
#include <RDSParser.h>
#include <stdio.h>
#include <DS1302.h>

// Default values
#define DEFAULT_CHANNEL 8870
#define DEFAULT_VOLUME    15  // max vol

// LCD messages
#define MSG_WELCOME         "-Welcome to FM-"
#define MSG_MUTE_ON         "Mute On"
#define MSG_MUTE_OFF        "Mute Off"
#define MSG_VOLUME_UP       "Volume Up"
#define MSG_VOLUME_DOWN     "Volume Down"
#define MSG_SEEK_UP         "Seek Up"
#define MSG_SEEK_DOWN       "Seek Down"
#define MSG_PRESET_KEY      "Preset Key 1..9"
#define MSG_PRESET_SELECTED "Preset Selected"

#define oneMinute   60000
#define tenMinute   600000
#define oneHour     3600000
#define twoHours    7200000 
#define threeHours  10800000
#define fourHours   14400000


// LCD control pins
int LCD_BACKLIGHT_PIN =  3;    // Pin controlling LCD backlight
int LCD_RS_PIN        =  8;
int LCD_E_PIN         =  9;
int LCD_D0_PIN        = 10;
int LCD_D1_PIN        = 11;
int LCD_D2_PIN        = 12;
int LCD_D3_PIN        = 13;

const int CePin       = 7;  // Chip Enable (Reset)
const int IoPin       = 6;  // Input/Output
const int SclkPin     = 5;  // Serial Clock

boolean alarmActive = false;
boolean mut = false;
boolean radioOn;
//byte volume;
int cod;
//char buf1[25];
//char buf2[25];
//char buf3[25];
char buf1[20];
char buf2[20];
char buf3[20];
char buf4[10];
char inputBuf[12];

long startTime; // the value returned from millis 
long duration;  // variable to store the selected time
long timeElapsed;
long timeRemaining;
int hours, minutes, seconds, sec;
int oraAlarma;
int minutAlarma;
int vol;
int alarmVol;
int post;
/************************************************************************/
/*********************** LCD Definitions and Functions ******************/
/************************************************************************/

// Declare instance of Liquid Crystal driver
LiquidCrystal lcd(LCD_RS_PIN, LCD_E_PIN, LCD_D0_PIN, LCD_D1_PIN, LCD_D2_PIN, LCD_D3_PIN);

// 89.40 MHz as 8940

//RDA5807M radio;    ///< Create an instance of a RDA5807 chip radio
SI4703  radio;    ///< Create an instance of a SI4703 chip radio.
/// get a RDS parser
RDSParser rds;


/************************************************************************/
/*********************** RTC Definitions and Functions ******************/
/************************************************************************/

  // Create a DS1302 object.
  DS1302 rtc(CePin, IoPin, SclkPin);

  String dayAsString(const Time::Day day) 
  {
    switch (day) 
    {
      case Time::kSunday: return "Dum";
      case Time::kMonday: return "Lun";
      case Time::kTuesday: return "Mar";
      case Time::kWednesday: return "Mie";
      case Time::kThursday: return "Joi";
      case Time::kFriday: return "Vin";
      case Time::kSaturday: return "Sam";
    }
    return "(unknown day)";
  }

  void printTime() 
  {
    
    // Get the current time and date from the chip.
    Time t = rtc.time();
    // Name the day of the week.
    const String day = dayAsString(t.day);

    // Format the time and date and insert into the temporary buffer.
    
    sprintf(buf1, " %s %02d-%02d-%04d",    // ziua, data
             day.c_str(),
             t.date, t.mon, t.yr);
    sprintf(buf2, "%02d:%02d:%02d",    // ora, min, sec
             t.hr, t.min, t.sec);   
          
      Serial.print(" Clock: ");
      Serial.print(buf2);
      Serial.print("\r");
  }


// - - - - - - - - - - - - - - - - - - - - - - - - - -

/// Update the ServiceName text on the LCD display.
void DisplayServiceName(char *name)
{
  //Serial.print("RDS:");
  //Serial.println(name);
  displayRow1Message(name);
  if (radioOn) 
 { 
   Serial.print(" RDS: ");
   Serial.print(name);
   Serial.print("\r");
 }
} // DisplayServiceName()


// - - - - - - - - - - - - - - - - - - - - - - - - - -

// Clear specified row of LCD
void clearLCDRow(byte row) 
{
    lcd.setCursor(0, row);
    lcd.print("                ");    
}

/************************************************************************/
/********************** EEPROM read/write Functions *********************/
/************************************************************************/

// Read uint8_t from the eeprom @ address
byte eepromReadUint8_t(int address) 
{
  byte value;
  eeprom_read_block(&value, (void *) address, sizeof(byte));
  return value;
}

// Write uint8_t to the eeprom @ address
void eepromWriteUint8_t(int address, byte value) 
{
  eeprom_write_block(&value, (void *) address, sizeof(byte));
}

// Read uint16_t from the eeprom @ address
unsigned int eepromReadUint16_t(int address) 
{
  unsigned int value;
  eeprom_read_block(&value, (void *) address, sizeof(unsigned int));
  return value;
}

// Write uint16_t to the eeprom @ address
void eepromWriteUint16_t(int address, unsigned int value) 
{
  eeprom_write_block(&value, (void *) address, sizeof(unsigned int));
}


void RDS_process(uint16_t block1, uint16_t block2, uint16_t block3, uint16_t block4) 
{
  rds.processData(block1, block2, block3, block4);
}

// Display the currently tuned channel centered on row 0 of LCD
void displayChannel()                                                             // displayChannel function
{  
    // Buffers for string formatting
    char buffer1[10];
    char buffer2[16];

    // Clear row 0
    clearLCDRow(0);
    
    // Get channel
    unsigned short int channel = radio.getFrequency()/10;
    
    // Convert channel to string
    dtostrf(channel / 10.0, 4, 1, buffer1);
    
    // Build complete string
    strcpy(buffer2, "FM - ");
    strcat(buffer2, buffer1);
    strcat(buffer2, " MHz");
    
    byte length = strlen(buffer2);
    byte offset = (16 - length ) / 2;
    
    // Position cursor to center channel string
    lcd.setCursor(offset, 0);
    
    // Print the channel string
    lcd.print(buffer2);
    if (radioOn) 
    {
      Serial.print(" Freq: ");
      Serial.print(buffer1);
      Serial.print(" Mhz");
      Serial.print("\r");
      
      Serial.print(" RDS: ");
      Serial.print("   ");
      Serial.print("\r");
    }
}

// Display row 1 message
void displayRow1Message(char *message) 
{ 
    // Clear row 1
    clearLCDRow(1);
    byte length = strlen(message);
    byte offset = (16 - length ) / 2; 
    // Position cursor to center message
    lcd.setCursor(offset, 1);  
    // Print the message
    lcd.print(message);
}

// Retrieve a preset channel. Presets are numbered 1 .. 9
// Preset 10 stores the channel selected when the radio was turned off
// so it can be restored when the radio is turned back on
unsigned int retrievePreset(byte presetNumber) 
{ 
    return eepromReadUint16_t(presetNumber * 2);        
}

// Store a preset channel
void storePreset(byte presetNumber, unsigned int channel) 
{
    eepromWriteUint16_t(presetNumber * 2, channel);
}
void displayChannelStored(int ch)
{
   Serial.print(" Info: ");
   Serial.print("Channel stored on: ");
   Serial.print(ch);
   Serial.print("\r");
}

// Process set preset
void processSetPreset(unsigned int channel)                                      // processSetPreset(unsigned int channel)   
{   
    int cod;  
    while (true) 
    {
      while(!Serial.available());  //wait for a choice
      cod = Serial.read();      
          

         switch (cod) 
        {
            case 'P':                        // Preset 1
                storePreset(1, channel);
                displayRow1Message(MSG_PRESET_SELECTED);
                displayChannelStored(1);
                return;                     
            case 'Q':                        // Preset 2
                storePreset(2, channel);
                displayRow1Message(MSG_PRESET_SELECTED);
                displayChannelStored(2);
                return;                       
            case 'A':                        // Preset 3
                 storePreset(3, channel);
                 displayRow1Message(MSG_PRESET_SELECTED);
                 displayChannelStored(3);
                 return;                    
            case 'S':                        // Preset 4
                 storePreset(4, channel);
                 displayRow1Message(MSG_PRESET_SELECTED);
                 displayChannelStored(4);
                 return;                  
            case 'T':                        // Preset 5
                 storePreset(5, channel);
                 displayRow1Message(MSG_PRESET_SELECTED);
                 displayChannelStored(5);
                 return;                  
            case 'U':                        // Preset 6
                 storePreset(6, channel);
                 displayRow1Message(MSG_PRESET_SELECTED);
                 displayChannelStored(6);
                 return;                  
            case 'V':                        // Preset 7
                 storePreset(7, channel);
                 displayRow1Message(MSG_PRESET_SELECTED);
                 displayChannelStored(7);
                 return;                   
            case 'W':                        // Preset 8
                 storePreset(8, channel);
                 displayRow1Message(MSG_PRESET_SELECTED);
                 displayChannelStored(8);
                 return;                  
            case 'X':                        // Preset 9
                 storePreset(9, channel);
                 displayRow1Message(MSG_PRESET_SELECTED);
                 displayChannelStored(9);
                 return;
            default:
                Serial.print(" Info: ");
                Serial.print("ERROR");
                Serial.print("\r");
                return;
        }
        
    }
}

void changeMuteState()
{
    // toggle mute mode
    radio.setMute(! radio.getMute());
}

void setStation(unsigned int canal)
{
   if (radioOn) 
   {
     unsigned int channel = retrievePreset(canal);
     if (channel != 0) 
     {
       radio.setFrequency(channel*10);                          
       displayChannel();
       Serial.print("    ");
       Serial.print("\r");
       displayRow1Message(MSG_PRESET_SELECTED);
                            
     }
   }
   cod = 'u';

}

void turnOnRadio(int v, int p)
{
    startTime = millis(); 
    lcd.clear();
    // Turning radio on
    digitalWrite(LCD_BACKLIGHT_PIN, HIGH);   // Backlight on
    //volume = DEFAULT_VOLUME;
    radio.setVolume(v);                     // Volume on
    radio.setMute(false);                    // Unmute radio
    lcd.display();                           // Display on
    unsigned int channel = retrievePreset(p);   
    if (channel != 0) 
    {
      radio.setFrequency(channel*10);
    }
                    
   displayRow1Message(MSG_WELCOME);         // Display welcome message
   radioOn = true;
   Serial.print(" Info: ");
   Serial.print("Radio is ON");
   Serial.print("\r");
   delay(500);  
   displayChannel();                        // Display the tuned channel
}

void turnOffRadio()
{
  // Turning radio off
  radioOn = false;                        // Indicate radio is off
  //storePreset(10, radio.getFrequency());          // Store the current channel
  radio.setVolume(0);                     // Volume to zero
  lcd.clear();                            // Clear the display
  //lcd.noDisplay();                        // Display off
  digitalWrite(LCD_BACKLIGHT_PIN, LOW);   // Backlight off
  startTime = millis();
  duration = fourHours;
  Serial.print(" RDS: ");
  Serial.print("   ");
  Serial.print("\r");
  Serial.print(" Info: ");  
  Serial.print("Radio is OFF");
  Serial.print("\r");
  //delay(500);
}

void receiveTimeAlarm()     // ora si minutul la care va porni radio-ul
{
  Serial.readBytes(inputBuf, 10);   // primeste ora, minutul, volumul si postul 
  long int a;                    // de ex: 1234108 
                                   // ora 12, min 34, vol 10, postul 8
  unsigned int interm1;
  unsigned int interm2;
  //Serial.println(inputBuf);
  a = atol(inputBuf);
  //Serial.println(oraMinut,DEC);
  //oraAlarma = oraMinut / 100;
  //minutAlarma = oraMinut % 100;
  oraAlarma = a / 100000;            // oraAlarma = 12
  interm1 = a % 100000;              // interm1 = 34108
  minutAlarma = interm1 / 1000;      // minutAlarma = 34
  interm2 = interm1 % 1000;          // interm2 = 108
  vol = interm2 / 10;                // vol = 10
  post = interm2 % 10;               // post = 8
  
  //if(alarmActive) 
  //{
  //  Serial.print(" POWER ON at: ");
  //  Serial.print(oraAlarma,DEC);
  //  Serial.print(':');
  //  Serial.println(minutAlarma,DEC);
  //  Serial.print("\r");
  //}
  
}
void saveAlarm()  
{
  eepromWriteUint8_t(22, oraAlarma);
  eepromWriteUint8_t(23, minutAlarma);
  eepromWriteUint8_t(24, vol);
  eepromWriteUint8_t(25, post);
}
void saveAlarmFlag()
{
  byte alarmFlag;
  if(alarmActive == false) alarmFlag = 0;
  if(alarmActive == true) alarmFlag = 1;
  eepromWriteUint8_t(21, alarmFlag); 
}
byte readAlarmFlag()
{
  byte alarmFlag = eepromReadUint8_t(21); 
  if(alarmFlag == 0) alarmActive = false;
  if(alarmFlag == 1) alarmActive = true;
  return alarmFlag;
}

void readAlarm()     // read EEPROM 
{
  oraAlarma = eepromReadUint8_t(22);
  minutAlarma = eepromReadUint8_t(23);
  alarmVol = eepromReadUint8_t(24);
  post = eepromReadUint8_t(25);
}

void checkAlarm()
{
  // Get the current time and date from the chip.
  Time t = rtc.time();
  if((oraAlarma == t.hr)&&(minutAlarma == t.min))
  {
    radioOn = true;
    turnOnRadio(alarmVol, post);
    alarmActive = false;  //dezactiveaza alarma
    saveAlarmFlag();
   
  }
  
}

void displayChSelected(byte ch)
{
 Serial.print(" Info: ");
 Serial.print("Channel ");
 Serial.print(ch);
 Serial.print("\r");
}

void listenSerial()       // from bluetooth HC-09 module                                      listen SERIAL function
{  
   
   if(Serial.available() > 0)
   {  
      int data = Serial.peek();  // ce este primul caracter receptionat?
      if (isDigit(data))
      {
        receiveTimeAlarm();
      }
      
      if (isAscii(data)) 
      {
        cod = Serial.read();
      }
      
      
      //cod = Serial.read();
   }  
                  
            if (cod  == 'm')     // Mute the radio
            {
                if (radioOn) 
                {
                   changeMuteState();

                  if (radio.getMute() == true)
                  {
                    displayRow1Message(MSG_MUTE_ON);
                    Serial.print(" Info: ");
                    Serial.print("Mute ON");
                    Serial.print("\r");
                    delay(300);
                    //Serial.print("    ");
                    //Serial.print("\r");
                  }
                  else 
                    {
                      displayRow1Message(MSG_MUTE_OFF);
                      Serial.print(" Info: ");
                      Serial.print("Mute OFF");
                      Serial.print("\r");
                      delay(300);
                      //Serial.print("    ");
                      //Serial.print("\r");
                    }  
                }
                cod = 'u';
            }
                

            else if (cod  == 'p')                          // Radio on/off
            {
                if (radioOn) 
                {
                  turnOffRadio();
                  cod = 'u';
                }    
                else    
                {
                  turnOnRadio(4,1);  // volum, post
                  cod = 'u';
                }
                
            }
                
                else if (cod  == 'a')                                // Volume up
                {
                    if (radioOn) 
                    {
                        vol = radio.getVolume();
                        vol += 1;                         // Increment volume 
                        if (vol > 15) 
                        {                   // Do range check
                            vol = 15;
                        }
                        radio.setVolume(vol);             // Set new volume
                        Serial.print(" Info: ");
                        Serial.print("Volume: ");
                        Serial.print(vol);
                        Serial.print("\r");
                        displayRow1Message(MSG_VOLUME_UP);
                        delay(200);
                        //Serial.print("    ");
                        //Serial.print("\r");
                        clearLCDRow(1);
                    }
                    cod = 'u';
                }
                    
                else if (cod  == 'b')                              // Volume down
                {
                    if (radioOn) 
                    {
                        vol = radio.getVolume();
                        vol -= 1;                         // Decrement volume 
                        if (vol < 1) 
                        {                    // Do range check
                            vol = 1;
                        }
                        radio.setVolume(vol);             // Set new volume
                        Serial.print(" Info: ");
                        Serial.print("Volume: ");
                        Serial.print(vol);
                        Serial.print("\r");
                        displayRow1Message(MSG_VOLUME_DOWN);
                        delay(200);
                        //Serial.print("    ");
                        //Serial.print("\r");
                        clearLCDRow(1);
                    }
                    cod = 'u';
                }
                    
                else if (cod  == 'c')                               // Seek down
                {
                    if (radioOn) 
                    {
                        radio.seekDown();
                        displayChannel();
                        Serial.print(" Info: ");
                        Serial.print("SEEK DOWN");
                        Serial.print("\r");

                        displayRow1Message(MSG_SEEK_DOWN);
                        delay(500);
                        Serial.print(" Info: ");
                        Serial.print("   ");
                        Serial.print("\r");
                        clearLCDRow(1);
                    }
                    
                    cod = 'u';
                }
                    
               else if (cod  == 'd')                          // Seek up
               {
                    if (radioOn) 
                    {
                        radio.seekUp();
                        displayChannel();
                        Serial.print(" Info: ");
                        Serial.print("SEEK UP");
                        Serial.print("\r");
                        
                        displayRow1Message(MSG_SEEK_UP);
                        delay(500);
                        Serial.print(" Info: ");
                        Serial.print("   ");
                        Serial.print("\r");
                        clearLCDRow(1);
                    }
                    
                    cod = 'u';
               }
                    
                else if (cod  == 'e')                            // Set up for setting preset
                {
                    if (radioOn) 
                    {
                        displayRow1Message(MSG_PRESET_KEY);    // Setup message
                        unsigned int channel = radio.getFrequency()/10;  // Get current channel
                        processSetPreset(channel);             // Process preset
                        delay(1000);
                        Serial.print(" Info: ");
                        Serial.print("   ");
                        Serial.print("\r");
                    }
                    cod = 'u';
                }
                    
               else if (cod  == 'P')             // Preset 1
               {
                 setStation(1);
                 displayChSelected(1);
               }
                        
               else if (cod  == 'Q')             // Preset 2
               {
                 setStation(2);
                 displayChSelected(2);
               }
                        
               else if (cod  == 'A')             // Preset 3
               {
                 setStation(3);
                 displayChSelected(3);
               }
                        
               else if (cod  == 'S')             // Preset 4
               {
                 setStation(4);
                 displayChSelected(4);
               }
                        
               else if (cod  == 'T')             // Preset 5
               {
                 setStation(5);
                 displayChSelected(5);
               }
                        
               else if (cod  == 'U')             // Preset 6
               {
                 setStation(6);
                 displayChSelected(6);
               }
                        
               else if (cod  == 'V')             // Preset 7
               {
                 setStation(7);
                 displayChSelected(7);
               }
                        
               else if (cod  == 'W')             // Preset 8
               {
                 setStation(8);
                 displayChSelected(8);
               }
                        
               else if (cod  == 'X')             // Preset 9
               {
                 setStation(9);
                 displayChSelected(9);
               }
               
               else if (cod  == 'v')             // 1 min
               {
                    if (radioOn) 
                    {
                      duration = oneMinute; 
                      startTime = millis();              
                    }
                    cod = 'u';
               }
               else if (cod  == 'w')             // 10 min
               {
                    if (radioOn) 
                    {
                      duration = tenMinute;  
                      startTime = millis();
                                    
                    }
                    cod = 'u';
               }
               else if (cod  == 'x')             // 1 h
               {
                    if (radioOn) 
                    {
                      duration = oneHour; 
                      startTime = millis();              
                    }
                    cod = 'u';
               }
               else if (cod  == 'y')             // 2 h
               {
                    if (radioOn) 
                    {
                      duration = twoHours;  
                      startTime = millis();
                                    
                    }
                    cod = 'u';
               }
               else if (cod  == 'z')             // 3 h
               {
                    if (radioOn) 
                    {
                      duration = threeHours;  
                      startTime = millis();             
                    }
                    cod = 'u';
               }
               else if (cod  == 'i')             // 4 h
               {
                    if (radioOn) 
                    {
                      duration = fourHours;  
                      startTime = millis();             
                    }
                    cod = 'u';
               }
               else if (cod  == 'r')  // toggle alarm ON/OFF           
               {
                    static byte toggleAlarm;
                    toggleAlarm++;
                    if (toggleAlarm % 2 == 0)      // alarm OFF
                    {
                      alarmActive = false;
                      saveAlarmFlag();
                      Serial.print(" POWER ON at: NOT ACTIVE");
                      Serial.print("\r"); 
                    }
                    else                  // alarm ON
                    {
                      alarmActive = true;
                      saveAlarmFlag();
                      sprintf(buf4, "%02d:%02d vol:%02d Ch:%01d", oraAlarma, minutAlarma, alarmVol, post);   
                      Serial.print(" POWER ON at: ");
                      Serial.print(buf4);             
                      Serial.print("\r"); 
                    }
                    cod = 'u';
               }
               else if (cod  == 'l')     // toggle light ON/OFF             
               {
                static byte toggleLight;
                toggleLight++;
                if (toggleLight % 2 == 0)
                {
                    digitalWrite(LCD_BACKLIGHT_PIN, HIGH);   // Backlight ON
                    Serial.print(" Info: ");
                    Serial.print("Light ON");
                    Serial.print("\r");
                }
                 else
                {
                    digitalWrite(LCD_BACKLIGHT_PIN, LOW);   // Backlight off
                    Serial.print(" Info: ");
                    Serial.print("Light OFF");
                    Serial.print("\r");
                }
                        
                    cod = 'u';
               }
               else if (cod  == 't')             // save alarm value in EEPROM 
               {
                    if (radioOn) 
                    {
                      saveAlarm(); 
                      readAlarm();
                    }
                    cod = 'u';
               }
    
}


void setup() 
{
 
  // open the Serial port
  Serial.begin(9600);
  Serial.print(" Radio...");
  Serial.print("\r");
  delay(500);
  pinMode(LCD_BACKLIGHT_PIN, OUTPUT);
  lcd.begin(16, 2);
    
  // Blank display and clear it
  //lcd.noDisplay();
  lcd.clear();

  // Initialize the Radio 
  radio.init();
  delay(200);
  radio.setBandFrequency(RADIO_BAND_FM, DEFAULT_CHANNEL);   // RADIO_BAND_FMWORLD -> 76--108   88-108 -> RADIO_BAND_FM
  turnOffRadio();
  startTime = 0;
  // setup the information chain for RDS data.
  radio.attachReceiveRDS(RDS_process);
  rds.attachServicenNameCallback(DisplayServiceName);
  
  

      // Determine if eeprom has been initialized by looking for eeprom signature
    byte sig0 = eepromReadUint8_t(0);
    byte sig1 = eepromReadUint8_t(1);
      
    if ((sig0 != 0xAA) | (sig1 != 0x55)) 
    {
        // EEPROM is uninitialized, so initialize it once
        // First write signature
        eepromWriteUint8_t(0, 0xAA);
        eepromWriteUint8_t(1, 0x55);
        // Then set all presets to 0 meaning not set
        for (int i = 1; i <= 10; i++) 
        {
            storePreset(i, 0);
        }
    }
    readAlarm();
    byte alarmFlag = readAlarmFlag();
    //Serial.print(" alarmFlag:");
    //Serial.print(alarmFlag);
    //Serial.print("\r");
    
    //Serial.print(" oraAlarma:");
    //Serial.print(oraAlarma);
    //Serial.print("\r");
    
    //Serial.print(" minutAlarma:");
    //Serial.print(minutAlarma);
    //Serial.print("\r");

} // Setup


void loop() 
{
 if(alarmActive) 
  {
    sprintf(buf4, "%02d:%02d vol:%02d Ch:%01d", oraAlarma, minutAlarma, alarmVol, post);    
    Serial.print(" POWER ON at: ");
    Serial.print(buf4);             
    Serial.print("\r");
    checkAlarm();  // daca radioul este deja pornit la ora alarmei se comuta pe postul si volumul setat
  }
  else
  {
    Serial.print(" POWER ON at: NOT ACTIVE");
    Serial.print("\r"); 
  }
  
  listenSerial();    // check for incoming serial codes         
  if(radioOn)
  {
    int s = seconds;
    timeElapsed = millis() - startTime;
    timeRemaining = (duration - timeElapsed)/1000;  // timpul ramas in secunde - final countdown :)
    hours = timeRemaining / 3600;
    int secondsRemain = timeRemaining % 3600;
    minutes = secondsRemain / 60; 
    seconds = secondsRemain % 60;
    sprintf(buf3, " %02d:%02d:%02d",    // ore, minute, secunde ramase pana la oprire
             hours, minutes, seconds);
    if(s != seconds)
    {
      Serial.print(" POWER OFF after:");
      Serial.print(buf3);
      Serial.print("\r");
    }
    
    
    if (timeElapsed >= duration)
    {
      turnOffRadio();                
    }
    radio.checkRDS(); 
  }

  if(radioOn == false) 
  {
    printTime(); 
    lcd.setCursor(0, 0);  
    lcd.print(buf1);      // data

    lcd.setCursor(4, 1);  
    lcd.print(buf2);     // ora, min, sec
    //if(alarmActive) checkAlarm();

  }
  
 
} // loop

// End.


