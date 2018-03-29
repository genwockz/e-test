/*
This code was developed by the_3d6 from Ultimate Robotics (http://ultimaterobotics.com.ua). 
License: you can use it for any purpose as long as you don't claim that you are its author and
you don't alter License terms and formulations (lines 1-10 of this file). 
You can share modifications of the code if you have properly tested their functionality, 
including confirming correct sensor response on CO concentrations of 0-30ppm, 100-300ppm and 1000-10000ppm.

If you can improve this code, please do so!
You can contact me at aka.3d6@gmail.com
*/


//WARNING! Each sensor is different!
//You MUST calibrate sensor manually and
//set proper sensor_reading_clean_air value before using
//it for any practical purpose!
int flag = 0;
const int relaypin=2;
int ihapX=0;
 int aveLPG= 0;
 int aveCO= 0;
 int aveSMOKE= 0;
int ihapY=0;
int time_scale = 8; //time scale: we altered main system timer, so now all functions like millis(), delay() etc 
//will think that time moves 8 times faster than it really is
//to correct that, we use time_scale variable:
//in order to make delay for 1 second, now
//we call delay(1000*time_scale)

void setTimer0PWM(byte chA, byte chB) //pins D5 and D6
{
  TCCR0A = 0b10110011; //OCA normal,OCB inverted, fast pwm
  TCCR0B = 0b010; //8 prescaler - instead of system's default 64 prescaler - thus time moves 8 times faster
  OCR0A = chA; //0..255
  OCR0B = chB;
}


void setTimer2PWM(byte chA, byte chB) //pins D11 and D3 MEGA 10 and 9
{
  TCCR2A = 0b10100011; //OCA,OCB, fast pwm
  TCCR2B = 0b001; //no prescaler
  OCR2A = chA; //0..255
  OCR2B = chB;
}

void setTimer1PWM(int chA, int chB) //pins D9 and D10
{
  TCCR1A = 0b10100011; //OCA,OCB, fast pwm
  TCCR1B = 0b1001; //no prescaler
  OCR1A = chA; //0..1023
  OCR1B = chB;
}

float opt_voltage = 0;
byte opt_width = 240; //default reasonable value

void pwm_adjust()
//this function tries various PWM cycle widths and prints resulting
//voltage for each attempt, then selects best fitting one and
//this value is used in the program later
{
    digitalWrite(relaypin, HIGH);
  float previous_v = 5.0; //voltage at previous attempt
  float raw2v = 5.0 / 1024.0;//coefficient to convert Arduino's 
  //analogRead result into voltage in volts
  for(int w =0; w < 250; w++)
  {
    setTimer2PWM(0, w);
    float avg_v = 0;
    for(int x = 0; x < 100; x ++) //measure over about 100ms to ensure stable result
    {
      avg_v += analogRead(A1);
      delay(time_scale);
    }
    avg_v *= 0.01;
    avg_v *= raw2v;
    Serial.print("adjusting PWM=");
    Serial.print(w);
    Serial.print(", V=");
    Serial.println(avg_v);
    if(avg_v < 3.6 && previous_v > 3.6) //we found optimal width
    {
      float dnew = 3.6 - avg_v; //now we need to find if current one
      float dprev = previous_v - 3.6;//or previous one is better
      if(dnew < dprev) //if new one is closer to 1.4 then return it
      {
        opt_voltage = avg_v;
        opt_width = w;
        return;
      }
      else //else return previous one
      {
        opt_voltage = previous_v;
        opt_width = w-1;
        return;
      }
    }
    previous_v = avg_v;
  }
}

float alarm_ppm_threshold = 100; //threshold CO concentration for buzzer alarm to be turned on,
float red_threshold = 40; //threshold when green LED is turned on red turns on
float reference_resistor_kOhm = 10.0; //fill correct resisor value if you are using not 10k reference
//620
float sensor_reading_clean_air = 620; //fill raw sensor value at the end of measurement phase (before heating starts) in clean air here! That is critical for proper calculation
//float sensor_reading_clean_air = 769;
float sensor_reading_100_ppm_CO = -1; //if you can measure it 
//using some CO meter or precisely calculated CO sample, then fill it here
//otherwise leave -1, default values will be used in this case

float sensor_100ppm_CO_resistance_kOhm; //calculated from sensor_reading_100_ppm_CO variable
float sensor_base_resistance_kOhm; //calculated from sensor_reading_clean_air variable

byte phase = 0; //1 - high voltage, 0 - low voltage, we start from measuring
unsigned long prev_ms = 0; //milliseconds in previous cycle
unsigned long sec10 = 0; //this timer is updated 10 times per second,
//when it will overflow, program might freeze or behave incorrectly. 
//It will happen only after ~13 years of operation. Still, 
//if you'll ever use this code in industrial application,
//please take care of overflow problem 
unsigned long high_on = 0; //time when we started high temperature cycle
unsigned long low_on = 0; //time when we started low temperature cycle
unsigned long last_print = 0; //time when we last printed message in serial

float sens_val = 0; //current smoothed sensor value
float last_CO_ppm_measurement = 0; //CO concentration at the end of previous measurement cycle

float raw_value_to_CO_ppm(float value)
{
  if(value < 1) return -1; //wrong input value
  sensor_base_resistance_kOhm = reference_resistor_kOhm * 1023 / sensor_reading_clean_air - reference_resistor_kOhm;
  if(sensor_reading_100_ppm_CO > 0)
  {
    sensor_100ppm_CO_resistance_kOhm = reference_resistor_kOhm * 1023 / sensor_reading_100_ppm_CO - reference_resistor_kOhm;
  }
  else
  {
    sensor_100ppm_CO_resistance_kOhm = sensor_base_resistance_kOhm * 0.5;
//This seems to contradict manufacturer's datasheet, but for my sensor it 
//looks quite real using CO concentration produced by CH4 flame according to
//this paper: http://www.iafss.org/publications/fss/8/1013/view/fss_8-1013.pdf 
//my experiments were very rough though, so I could have overestimated CO concentration significantly
//if you have calibrated sensor to produce reference 100 ppm CO, then
//use it instead    
  }
  float sensor_R_kOhm = reference_resistor_kOhm * 1023 / value - reference_resistor_kOhm;
  float R_relation = sensor_100ppm_CO_resistance_kOhm / sensor_R_kOhm;
  float CO_ppm = 100 * (exp(R_relation) - 1.648);
  if(CO_ppm < 0) CO_ppm = 0;
  return CO_ppm;
}

void startMeasurementPhase()
{
  phase = 0;
  flag = 0;
  low_on = sec10;
  setTimer2PWM(0, opt_width);
  digitalWrite(relaypin, LOW);
}

void startHeatingPhase()
{
  flag = 1;
  phase = 1;
  high_on = sec10;
  setTimer2PWM(0, 255);
    digitalWrite(relaypin, HIGH);
    
}
void setLEDs(int br_green, int br_red)
{
  if(br_red < 0) br_red = 0;
  if(br_red > 100) br_red = 100;
  if(br_green < 0) br_green = 0;
  if(br_green > 100) br_green = 100;

  float br = br_red;
  br *= 0.01;
  br = (exp(br)-1) / 1.7183 * 1023.0;
  float bg = br_green;
  bg *= 0.01;
  bg = (exp(bg)-1) / 1.7183 * 1023.0;
  if(br < 0) br = 0;
  if(br > 1023) br = 1023;
  if(bg < 0) bg = 0;
  if(bg > 1023) bg = 1023;

  setTimer1PWM(1023-br, 1023-bg);
}
void buzz_on()
{
  setTimer0PWM(128, 128);
}
void buzz_off()
{
  setTimer0PWM(255, 255);
}
void buzz_beep()
{
  byte sp = sec10%15;
  if(sp == 0)
    buzz_on();
  if(sp == 1)
    buzz_off();
  if(sp == 2)
    buzz_on();
  if(sp == 3)
    buzz_off();
  if(sp == 4)
    buzz_on();
  if(sp == 5)
    buzz_off();
}

/********************************MQ2 Definitions************/
const int MQ_PIN=A2;                                //define which analog input channel you are going to use
int RL_VALUE=10;                                     //define the load resistance on the board, in kilo ohms
float RO_CLEAN_AIR_FACTOR=9.83;
int CALIBARAION_SAMPLE_TIMES=20;                    //define how many samples you are going to take in the calibration phase
int CALIBRATION_SAMPLE_INTERVAL=500;                //define the time interal(in milisecond) between each samples in the
                                                    //cablibration phase
int READ_SAMPLE_INTERVAL=50;                        //define how many samples you are going to take in normal operation
int READ_SAMPLE_TIMES=5;                            //define the time interal(in milisecond) between each samples in 
#define         GAS_LPG             0   
#define         GAS_CO              1   
#define         GAS_SMOKE           2                                                      //normal operation
float           LPGCurve[3]  =  {2.3,0.21,-0.47};   //two points are taken from the curve. 
                                                    //with these two points, a line is formed which is "approximately equivalent"
                                                    //to the original curve. 
                                                    //data format:{ x, y, slope}; point1: (lg200, 0.21), point2: (lg10000, -0.59) 
float           COCurve[3]  =  {2.3,0.72,-0.34};    //two points are taken from the curve. 
                                                    //with these two points, a line is formed which is "approximately equivalent" 
                                                    //to the original curve.
                                                    //data format:{ x, y, slope}; point1: (lg200, 0.72), point2: (lg10000,  0.15) 
float           SmokeCurve[3] ={2.3,0.53,-0.44};    //two points are taken from the curve. 
                                                    //with these two points, a line is formed which is "approximately equivalent" 
                                                    //to the original curve.
                                                    //data format:{ x, y, slope}; point1: (lg200, 0.53), point2: (lg10000,  -0.22)                                                     
float           Ro           =  10;                 //Ro is initialized to 10 kilo ohms

         //Serial pins



         float MQResistanceCalculation(int raw_adc)
{
  return ( ((float)RL_VALUE*(1023-raw_adc)/raw_adc));
}
 
/***************************** MQCalibration ****************************************
Input:   mq_pin - analog channel
Output:  Ro of the sensor
Remarks: This function assumes that the sensor is in clean air. It use  
         MQResistanceCalculation to calculates the sensor resistance in clean air 
         and then divides it with RO_CLEAN_AIR_FACTOR. RO_CLEAN_AIR_FACTOR is about 
         10, which differs slightly between different sensors.
************************************************************************************/ 
float MQCalibration(int mq_pin)
{
  int i;
  float val=0;

  for (i=0;i<CALIBARAION_SAMPLE_TIMES;i++) { 
    Serial.print("adjusting");
    Serial.print(" ");  
     Serial.print(val);//take multiple samples
    val += MQResistanceCalculation(analogRead(mq_pin));
    delay(CALIBRATION_SAMPLE_INTERVAL);
      Serial.print(" ");
    Serial.println(i);
    
  }
  val = val/CALIBARAION_SAMPLE_TIMES;                   //calculate the average value
  val = val/RO_CLEAN_AIR_FACTOR;                        //divided by RO_CLEAN_AIR_FACTOR yields the Ro                                        
  return val;                                                      //according to the chart in the datasheet 

}
 
/*****************************  MQRead *********************************************
Input:   mq_pin - analog channel
Output:  Rs of the sensor
Remarks: This function use MQResistanceCalculation to caculate the sensor resistenc (Rs).
         The Rs changes as the sensor is in the different consentration of the target
         gas. The sample times and the time interval between samples could be configured
         by changing the definition of the macros.
************************************************************************************/ 
float MQRead(int mq_pin)
{
  int i;
  float rs=0;
 
  for (i=0;i<READ_SAMPLE_TIMES;i++) {
    rs += MQResistanceCalculation(analogRead(mq_pin));
    delay(READ_SAMPLE_INTERVAL);
  }
 
  rs = rs/READ_SAMPLE_TIMES;
 
  return rs;  
}
 
/*****************************  MQGetGasPercentage **********************************
Input:   rs_ro_ratio - Rs divided by Ro
         gas_id      - target gas type
Output:  ppm of the target gas
Remarks: This function passes different curves to the MQGetPercentage function which 
         calculates the ppm (parts per million) of the target gas.
************************************************************************************/ 
long MQGetGasPercentage(float rs_ro_ratio, int gas_id)
{
  if ( gas_id == GAS_LPG ) {
     return MQGetPercentage(rs_ro_ratio,LPGCurve);
  } else if ( gas_id == GAS_CO ) {
     return MQGetPercentage(rs_ro_ratio,COCurve);
  } else if ( gas_id == GAS_SMOKE ) {
     return MQGetPercentage(rs_ro_ratio,SmokeCurve);
  }    
 
  return 0;
}
 
/*****************************  MQGetPercentage **********************************
Input:   rs_ro_ratio - Rs divided by Ro
         pcurve      - pointer to the curve of the target gas
Output:  ppm of the target gas
Remarks: By using the slope and a point of the line. The x(logarithmic value of ppm) 
         of the line could be derived if y(rs_ro_ratio) is provided. As it is a 
         logarithmic coordinate, power of 10 is used to convert the result to non-logarithmic 
         value.
************************************************************************************/ 
long  MQGetPercentage(float rs_ro_ratio, float *pcurve)
{
  return (pow(10,( ((log(rs_ro_ratio)-pcurve[1])/pcurve[2]) + pcurve[0])));
}
/********************************MQ2 Definitions************/

void setup() {

//WARNING! Each sensor is different!
//You MUST calibrate sensor manually and
//set proper sensor_reading_clean_air value before using
//it for any practical purpose!
  pinMode(relaypin, OUTPUT);
  pinMode(5, OUTPUT);
  pinMode(6, OUTPUT);
  pinMode(9, OUTPUT);
  pinMode(11, OUTPUT);
  pinMode(12, OUTPUT);
  pinMode(A0, INPUT);
  pinMode(A1, INPUT);
  
  setTimer1PWM(1023, 0);
  analogReference(DEFAULT);
  Serial.begin(9600);

  pwm_adjust();
  Ro = MQCalibration(MQ_PIN);  
//   Serial.println("done!");                                 //Serial display  
//   Serial.println("Ro= ");
//   Serial.println(Ro);
//   Serial.println("kohm");
//  
//  Serial.print("PWM result: width ");
//  Serial.print(opt_width);
//  Serial.print(", voltage ");
//  Serial.println(opt_voltage);
//  Serial.println("Data output: raw A0 value, heating on/off (0.1 off 1000.1 on), CO ppm from last measurement cycle");
  //beep buzzer in the beginning to indicate that it works
  buzz_on();
  delay(100*time_scale);
  buzz_off();
  delay(100*time_scale);
  buzz_on();
  delay(100*time_scale);
  buzz_off();
  delay(100*time_scale);
  buzz_on();
  delay(100*time_scale);
  buzz_off();
  delay(100*time_scale);

  startMeasurementPhase(); //start from measurement
}

void loop() 
{

//WARNING! Each sensor is different!
//You MUST calibrate sensor manually and
//set proper sensor_reading_clean_air value before using
//it for any practical purpose!

  unsigned long ms = millis();
  int dt = ms - prev_ms;

//this condition runs 10 times per second, even if millis() 
//overflows - which is required for long-term stability
//when millis() overflows, this condition will run after less
//than 0.1 seconds - but that's fine, since it happens only once
//per several days
  if(dt > 100*time_scale || dt < 0) 
  {
    prev_ms = ms; //store previous cycle time
    sec10++; //increase 0.1s counter
    if(sec10%10 == 1) //we want LEDs to blink periodically
    {
      setTimer1PWM(1023, 1023); //blink LEDs once per second
      //use %100 to blink once per 10 seconds, %2 to blink 5 times per second
    }
    else //all other time we calculate LEDs and buzzer state
    {
      int br_red = 0, br_green = 0; //brightness from 1 to 100, setLEDs function handles converting it into timer settings
      if(last_CO_ppm_measurement <= red_threshold) //turn green LED if we are below 30 PPM
      {//the brighter it is, the lower concentration is
        br_red = 0; //turn off red
        br_green = (red_threshold - last_CO_ppm_measurement)*100.0/red_threshold; //the more negative is concentration, the higher is value
        if(br_green < 1) br_green = 1; //don't turn off completely
      }
      else //if we are above threshold, turn on red one
      {
        br_green = 0; //keep green off
        br_red = (last_CO_ppm_measurement-red_threshold)*100.0/red_threshold; //the higher is concentration, the higher is this value
        if(br_red < 1) br_red = 1; //don't turn off completely
      }

      if(last_CO_ppm_measurement > alarm_ppm_threshold) //if at 50 seconds of measurement cycle we are above threshold 
        buzz_beep();
      else
        buzz_off();

      setLEDs(br_green, br_red); //set LEDs brightness
    }
  }
  if(phase == 1 && sec10 - high_on > 600) //60 seconds of heating ended?
    startMeasurementPhase();
  if(phase == 0 && sec10 - low_on > 900) //90 seconds of measurement ended?
  {
    last_CO_ppm_measurement = raw_value_to_CO_ppm(sens_val);
    startHeatingPhase();
  }

  float v = analogRead(A0); //reading value
  sens_val *= 0.999; //applying exponential averaging using formula
  sens_val += 0.001*v; // average = old_average*a + (1-a)*new_reading
 
  
  if(sec10 - last_print > 9) //print measurement result into serial 2 times per second
  {
    last_print = sec10;
    Serial.print(sens_val);
    Serial.print(" ");
     Serial.print(last_CO_ppm_measurement);
    Serial.print(" ");
   Serial.print(0.1 + phase*1000);
   Serial.print(" ");
      if(phase == 0){
       
         ihapY=0;
          ihapX++;
          Serial.print(ihapX);
             Serial.print(" ");
        }else{
 
         ihapX=0;
          ihapY++;
          Serial.print(ihapY);
             Serial.print(" ");
          
          }

  int qwe =  analogRead(A2);
  Serial.print("RAW VALUE: ");
  Serial.print(qwe);
   Serial.print(" ");
  long iPPM_LPG = 0;
  long iPPM_CO = 0;
  long iPPM_Smoke = 0;
  iPPM_LPG = MQGetGasPercentage(MQRead(MQ_PIN)/Ro,GAS_LPG);
  iPPM_CO = MQGetGasPercentage(MQRead(MQ_PIN)/Ro,GAS_CO);
  iPPM_Smoke = MQGetGasPercentage(MQRead(MQ_PIN)/Ro,GAS_SMOKE);
   

   Serial.print("LPG: ");
   Serial.print(iPPM_LPG);
   Serial.print(" ppm ");   
  

   Serial.print("CO: ");
   Serial.print(iPPM_CO);
   Serial.print(" ppm ");    


   Serial.print("Smoke: ");
   Serial.print(iPPM_Smoke);
   Serial.println(" ppm ");   

 
  }


  
}

