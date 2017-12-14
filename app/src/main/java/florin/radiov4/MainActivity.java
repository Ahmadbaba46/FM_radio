//
// http://android--examples.blogspot.ro/2015/01/radio-button-onclick-event-in-android.html
// http://www.limbaniandroid.com/2014/05/custom-radio-buttons-example-in-android.html
// http://android--examples.blogspot.ro/2015/04/timepickerdialog-in-android.html
// http://www.java2s.com/Code/Android/UI/UsingTimePickerDialog.htm

package florin.radiov4;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity
{
        UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        ImageButton btnOnOff,btnMuteOn,btnScanDown,btnScanUp,btnStore,btnConnect,
                btn1,btn2,btn3,btn4,btn5,btn6,btn7,btn8,btn9,btnVolPlus,btnVolMin,btnSetAlarm,
                alarmSw,lightToggBut;
        TextView rdsTxtV;
        TextView freqTxtV;
        TextView clockTxtV;
        TextView infoTxtV;
        TextView powerOnTxtV;
        TextView alarmTxtV;
        Spinner timeSpinner;
        Spinner volSpinner;
        Spinner stationSpinner;
        private RadioGroup radioGroup1;
        private RadioGroup radioGroup2;
        private RadioGroup radioGroup3;
        RadioButton rB1, rB2, rB3, rB4, rB5, rB6, rB7, rB8, rB9;
        private ProgressDialog progress;

        private BluetoothAdapter mBluetoothAdapter = null;
        private Set<BluetoothDevice> pairedDevices;
        BluetoothSocket mmSocket = null;
        BluetoothDevice mmDevice = null;
        OutputStream mmOutputStream;
        InputStream mmInputStream;
        Thread workerThread;
        byte[] readBuffer;
        int readBufferPosition;
        String address = "0F:15:20:08:0F:1A";
        volatile boolean stopWorker;
        private boolean isBtConnected = false;
        //final int BLUETOOTH_REQUEST_CODE = 0;
        private static final String TAG = "bluetooth";
        private String[] timeChoose;
        private String[] volumeChoose;
        private String[] stationChoose;
        String alarmTime;
        String station;
        String volume;
        String alarmRes;
        private int pHour;
        private int pMinute;

        // DateFormat fmtDateAndTime = DateFormat.getDateTimeInstance();
        Calendar Time = Calendar.getInstance();
        TimePickerDialog.OnTimeSetListener t = new TimePickerDialog.OnTimeSetListener()
        {
            public void onTimeSet(TimePicker view, int hourOfDay,
                                  int minute)
            {
                pHour = hourOfDay;
                pMinute = minute;
                alarmTime = String.format("%02d:%02d", pHour, pMinute);
                alarmTxtV.setText(alarmTime);

                String a;
                a = alarmTxtV.getText().toString();
                String result = a.substring(0, 2) + a.substring(3);  // remove ":"
                alarmRes = new StringBuilder().append(result).append(volume).append(station).toString();

                //infoTxtV.setText(res);
                Toast.makeText(getBaseContext(), "Set channel and volume first", Toast.LENGTH_SHORT).show();
                //alarmSw.setVisibility(View.VISIBLE);

            }

        };


        @Override
        protected void onCreate(Bundle savedInstanceState)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            timeSpinner = (Spinner)findViewById(R.id.timeChooser);
            volSpinner = (Spinner)findViewById(R.id.volumeSpinner);
            stationSpinner = (Spinner)findViewById(R.id.stationSpinner);

            lightToggBut = (ImageButton) findViewById(R.id.lightButton);
            btnOnOff = (ImageButton)findViewById(R.id.onOffButton);
            btnMuteOn = (ImageButton)findViewById(R.id.muteOnButton);
            btnSetAlarm = (ImageButton)findViewById(R.id.setAlarmButton);
            btnScanDown = (ImageButton)findViewById(R.id.scanDownButton);
            btnScanUp = (ImageButton)findViewById(R.id.scanUpButton);
            btnStore = (ImageButton)findViewById(R.id.storeButton);
            btnConnect = (ImageButton)findViewById(R.id.connectButton);
            btnVolPlus = (ImageButton)findViewById(R.id.volPlusButton);
            btnVolMin = (ImageButton)findViewById(R.id.volMinusButton);
            timeSpinner.setVisibility(View.INVISIBLE);
            rdsTxtV = (TextView)findViewById(R.id.rdsTextView);
            //rdsTxtV.setSelected(true);  // Set focus to the textview
            //rdsTxtV.setMovementMethod(new ScrollingMovementMethod());
            freqTxtV = (TextView)findViewById(R.id.freqTextView);
            clockTxtV = (TextView)findViewById(R.id.clockTextView);
            infoTxtV = (TextView)findViewById(R.id.infoTextView);
            powerOnTxtV = (TextView)findViewById(R.id.powerOnTextView);
            alarmTxtV = (TextView)findViewById(R.id.alarmTextView);
            alarmSw = (ImageButton) findViewById(R.id.alarmSwitch);

            timeChoose = getResources().getStringArray(R.array.Timer);
            volumeChoose = getResources().getStringArray(R.array.VolChooser);
            stationChoose = getResources().getStringArray(R.array.StationChooser);

            ArrayAdapter<String> timerAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, timeChoose);
            ArrayAdapter<String> volumeAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, volumeChoose);
            ArrayAdapter<String> stationAdapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_item, stationChoose);

            timerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            timeSpinner.setAdapter(timerAdapter);
            timerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            volSpinner.setAdapter(volumeAdapter);
            timerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            stationSpinner.setAdapter(stationAdapter);

            timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id)
                {
                    String item = parent.getItemAtPosition(position).toString();
                    switch (item)
                    {
                        case "4 h":
                            try
                            {
                                sendData("i");
                            }catch (Exception e)
                            {
                                //msg("ERROR - No Bluetooth connection");
                            }
                            break;
                        case "3 h":
                            try
                            {
                                sendData("z");
                            }catch (Exception e)
                            {
                                msg("ERROR - No Bluetooth connection");
                            }
                            break;
                        case "2 h":
                            try
                            {
                                sendData("y");
                            }catch (Exception e)
                            {
                                msg("ERROR - No Bluetooth connection");
                            }
                            break;
                        case "1 h":
                            try
                            {
                                sendData("x");
                            }catch (Exception e)
                            {
                                msg("ERROR - No Bluetooth connection");
                            }
                            break;
                        case "10 m":
                            try
                            {
                                sendData("w");
                            }catch (Exception e)
                            {
                                msg("ERROR - No Bluetooth connection");
                            }
                            break;
                        case "1 m":
                            try
                            {
                                sendData("v");
                            }catch (Exception e)
                            {
                                //msg("ERROR - No Bluetooth connection");
                            }
                            break;


                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0)
                {

                }
            });

            volSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id)
                {
                    String item = parent.getItemAtPosition(position).toString();
                    switch (item)
                    {
                        case "Vol 01":
                            volume = "01";
                            break;
                        case "Vol 02":
                            volume = "02";
                            break;
                        case "Vol 03":
                            volume = "03";
                            break;
                        case "Vol 04":
                            volume = "04";
                            break;
                        case "Vol 05":
                            volume = "05";
                            break;
                        case "Vol 06":
                            volume = "06";
                            break;
                        case "Vol 07":
                            volume = "07";
                            break;
                        case "Vol 08":
                            volume = "08";
                            break;
                        case "Vol 09":
                            volume = "09";
                            break;
                        case "Vol 10":
                            volume = "10";
                            break;
                        case "Vol 11":
                            volume = "11";
                            break;
                        case "Vol 12":
                            volume = "12";
                            break;
                        case "Vol 13":
                            volume = "13";
                            break;
                        case "Vol 14":
                            volume = "14";
                            break;
                        case "Vol 15":
                            volume = "15";
                            break;


                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0)
                {

                }
            });

            stationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id)
                {
                    String item = parent.getItemAtPosition(position).toString();
                    switch (item)
                    {
                        case "Ch 1":
                            station = "1";
                            break;
                        case "Ch 2":
                            station = "2";
                            break;
                        case "Ch 3":
                            station = "3";
                            break;
                        case "Ch 4":
                            station = "4";
                            break;
                        case "Ch 5":
                            station = "5";
                            break;
                        case "Ch 6":
                            station = "6";
                            break;
                        case "Ch 7":
                            station = "7";
                            break;
                        case "Ch 8":
                            station = "8";
                            break;
                        case "Ch 9":
                            station = "9";
                            break;


                    }

                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0)
                {

                }
            });

            findBT();



            //alarmSw.setChecked(false);
            //checkAlarmState();
            alarmSw.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                        //infoTxtV.setText(result);
                        //Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
                        try
                        {
                            sendData(alarmRes);
                            sendData("\0");  // null
                            sendData("\0");  // null
                            sendData("\0");  // null
                            sendData("t");   // save alarm value in EEPROM
                            sendData("\0");  // null
                            sendData("r");   // toggle alarm
                            msg("Alarm ON/OFF");
                        }catch (Exception e)
                        {
                            msg("ERROR");
                        }


                }
            });
            lightToggBut.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {

                        try
                        {
                            sendData("l");   // toggle light

                        }catch (Exception e)
                        {
                            msg("ERROR - No Bluetooth connection");
                        }

                }
            });
            btnSetAlarm.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    new TimePickerDialog(MainActivity.this,
                            t,
                            Time.get(Calendar.HOUR_OF_DAY),
                            Time.get(Calendar.MINUTE),
                            true).show();

                    pHour = Time.get(Calendar.HOUR_OF_DAY);
                    pMinute = Time.get(Calendar.MINUTE);
                }


            });
            //alarmTime = String.format("%02d:%02d", dateAndTime.get(Calendar.HOUR_OF_DAY), dateAndTime.get(Calendar.MINUTE));
            //alarmTxtV.setText(alarmTime);

            btnConnect.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {

                    if (mmSocket == null)
                    {
                        new  ConnectBT().execute(); //Call the class to connect
                        //btnConnect.setVisibility(View.INVISIBLE);
                    }

                }
                //Toast.makeText(getBaseContext(), "Turn on/off", Toast.LENGTH_SHORT).show();

            });

            btnOnOff.setOnClickListener(new View.OnClickListener()
            {
                public void onClick(View v)
                {
                    try
                    {
                        sendData("p");
                        freqTxtV.setText(" ");
                    }catch (Exception e)
                    {
                        msg("ERROR - No Bluetooth connection");
                    }

                    //Toast.makeText(getBaseContext(), "Turn on/off", Toast.LENGTH_SHORT).show();
                }
            });

            btnMuteOn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    try
                    {
                        sendData("m");
                    }catch (Exception e)
                    {
                        msg("ERROR - No Bluetooth connection");
                    }
                    //Toast.makeText(getBaseContext(), "Mute On", Toast.LENGTH_SHORT).show();
                }
            });

            btnScanDown.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try
                    {
                        sendData("c");
                    }catch (Exception e)
                    {
                        msg("ERROR - No Bluetooth connection");
                    }
                    //Toast.makeText(getBaseContext(), "Seek Down", Toast.LENGTH_SHORT).show();
                }
            });
            btnScanUp.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try
                    {
                        sendData("d");
                    }catch (Exception e)
                    {
                        msg("ERROR - No Bluetooth connection");
                    }
                    //Toast.makeText(getBaseContext(), "Seek Up", Toast.LENGTH_SHORT).show();
                }
            });
            btnStore.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try
                    {
                        sendData("e");
                    }catch (Exception e)
                    {
                        msg("ERROR - No Bluetooth connection");
                    }
                    //Toast.makeText(getBaseContext(), "Store channel", Toast.LENGTH_SHORT).show();
                }
            });

            btnVolPlus.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try
                    {
                        sendData("a");
                    }catch (Exception e)
                    {
                        msg("ERROR - No Bluetooth connection");
                    }
                    //Toast.makeText(getBaseContext(), "Volume Up", Toast.LENGTH_SHORT).show();
                }
            });
            btnVolMin.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    try
                    {
                        sendData("b");
                    }catch (Exception e)
                    {
                        msg("ERROR - No Bluetooth connection");
                    }
                    //Toast.makeText(getBaseContext(), "Volume Down", Toast.LENGTH_SHORT).show();
                }
            });
        }



        void findBT() // find and enable bluetooth
        {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if(mBluetoothAdapter == null)
            {
                infoTxtV.setText("No bluetooth adapter available");
            }

            else if(!mBluetoothAdapter.isEnabled())
            {
                mBluetoothAdapter.enable();

                //Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //startActivityForResult(enableBluetooth, 1);
            }

            pairedDevices = mBluetoothAdapter.getBondedDevices();
            if(pairedDevices.size() > 0)
            {
                for(BluetoothDevice device : pairedDevices)
                {
                    if(device.getName().equals("HC-09"))
                    {
                        mmDevice = device;

                        break;
                    }


                }
            }

            SystemClock.sleep(1500);
            new  ConnectBT().execute(); //Call the class to connect

        }
        public void disableBT()
        {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter.isEnabled())
            {
                mBluetoothAdapter.disable();
            }
        }


        private void disconnect()
        {
            if (mmSocket!=null) //If the btSocket is busy
            {
                try
                {
                    mmSocket.close(); //close connection
                }
                catch (IOException e)
                { msg("Error");}
            }
            // finish(); //return to the first layout

        }


        void beginListenForData()
        {
            final Handler handler = new Handler();
            // final byte delimiter = 10; //This is the ASCII code for a newline character
            final byte delimiter = 13; //This is the ASCII code for CR character  \r

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];
            workerThread = new Thread(new Runnable()
            {
                public void run()
                {
                    while(!Thread.currentThread().isInterrupted() && !stopWorker)
                    {
                        try
                        {
                            int bytesAvailable = mmInputStream.available();
                            if(bytesAvailable > 0)
                            {
                                byte[] packetBytes = new byte[bytesAvailable];
                                mmInputStream.read(packetBytes);
                                for(int i=0;i<bytesAvailable;i++)
                                {
                                    byte b = packetBytes[i];
                                    if(b == delimiter)
                                    {
                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                        final String data = new String(encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;


                                        handler.post(new Runnable()
                                        {
                                            public void run()
                                            {
                                                final String mhz = " Mhz";
                                                final String clock = " Clock";
                                                final String powerOff = " POWER OFF";
                                                final String info = " Info";
                                                final String powerOn = " POWER ON";
                                                final String notActive = "NOT ACTIVE";
                                                final String rds = " RDS";
                                                final String radioOFF = "Radio is OFF";
                                                final String radioON = "Radio is ON";
                                                if (data.contains(mhz)) freqTxtV.setText(data);
                                                if (data.contains(clock)|| data.contains(powerOff)) clockTxtV.setText(data);
                                                if (data.contains(info)) infoTxtV.setText(data);
                                                if (data.contains(powerOn))
                                                {
                                                    powerOnTxtV.setText(data);
                                                }

                                                if (data.contains(rds)) rdsTxtV.setText(data);
                                                if (data.contains(radioOFF))
                                                {
                                                    freqTxtV.setText(" ");
                                                    rdsTxtV.setText(" ");
                                                    timeSpinner.setVisibility(View.INVISIBLE);
                                                }
                                                if (data.contains(radioON))
                                                {
                                                    timeSpinner.setVisibility(View.VISIBLE);
                                                }
                                                if (data.contains(notActive))
                                                {
                                                    //alarmSw.setVisibility(View.VISIBLE);

                                                    //alarmSw.setChecked(false);

                                                }

                                            }
                                        });
                                    }

                                    else
                                    {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }
                        }
                        catch (IOException ex)
                        {
                            stopWorker = true;
                        }
                    }
                }
            });

            workerThread.start();
        }


        void closeBT() throws IOException
        {
            stopWorker = true;
            mmOutputStream.close();
            mmInputStream.close();
            mmSocket.close();
            msg("Bluetooth connection closed");
        }
        void sendData(String message) throws IOException
        {
            byte[] msgBuffer = message.getBytes();
            mmOutputStream.write(msgBuffer);
        }

        // fast way to call Toast
        private void msg(String s)
        {
            Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
        }


        private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
        {
            private boolean ConnectSuccess = true; //if it's here, it's almost connected

            @Override
            protected void onPreExecute()
            {
                progress = ProgressDialog.show(MainActivity.this, "Connecting...", "Please wait!!!");  //show a progress dialog
            }

            @Override
            protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
            {
                try
                {
                    if (mmSocket == null || !isBtConnected)
                    {
                        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);//connects to the device's address and checks if it's available
                        //btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                        //BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                        //myBluetooth.cancelDiscovery();
                        // btSocket.connect();//start connection

                        try
                        {
                            Method m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
                            mmSocket = (BluetoothSocket) m.invoke(device, 1);
                        } catch (Exception e)
                        {
                            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
                        }

                        // method = device.getClass().getMethod("createRfcommSocket", new Class[] { UUID.class });

                        mBluetoothAdapter.cancelDiscovery();
                        mmSocket.connect();//start connection
                        mmOutputStream = mmSocket.getOutputStream();
                        mmInputStream = mmSocket.getInputStream();

                    }
                }
                catch (Exception e)
                {
                    ConnectSuccess = false;//if the try failed, you can check the exception here
                    //Toast.makeText(getBaseContext(), "ERROR - No Bluetooth", Toast.LENGTH_SHORT).show();
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
            {
                super.onPostExecute(result);

                if (!ConnectSuccess)
                {
                    //msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                    disableBT();
                    //Toast.makeText(getBaseContext(), "ERROR - No Bluetooth", Toast.LENGTH_SHORT).show();
                    finish();
                    System.exit(0);

                }
                else
                {
                    msg("Connected");
                    isBtConnected = true;
                    timeSpinner.setVisibility(View.VISIBLE);
                }
                progress.dismiss();
                beginListenForData();
            }
        }

        @Override
        public void onBackPressed()
        {
            //Toast.makeText(getBaseContext(), "Close Bluetooth and exit", Toast.LENGTH_SHORT).show();
            if (isBtConnected)
            {
                try
                {
                    closeBT();
                    disableBT();
                    finish();
                }
                catch (IOException ex)
                {

                }
            }
            disableBT();
            finish();
        }


    public void onRadioButtonClicked(View v)
    {

        radioGroup1 = (RadioGroup) findViewById(R.id.radioGroup1);
        radioGroup2 = (RadioGroup) findViewById(R.id.radioGroup2);
        radioGroup3 = (RadioGroup) findViewById(R.id.radioGroup3);

        rB1 = (RadioButton) findViewById(R.id.radioButton1);
        rB2 = (RadioButton) findViewById(R.id.radioButton2);
        rB3 = (RadioButton) findViewById(R.id.radioButton3);
        rB4 = (RadioButton) findViewById(R.id.radioButton4);
        rB5 = (RadioButton) findViewById(R.id.radioButton5);
        rB6 = (RadioButton) findViewById(R.id.radioButton6);
        rB7 = (RadioButton) findViewById(R.id.radioButton7);
        rB8 = (RadioButton) findViewById(R.id.radioButton8);
        rB9 = (RadioButton) findViewById(R.id.radioButton9);


        //is the current radio button now checked?
        boolean  checked = ((RadioButton) v).isChecked();

        switch(v.getId())
        {
            case R.id.radioButton1:
                if(checked)
                    radioGroup2.clearCheck();
                radioGroup3.clearCheck();
                try
                {
                    sendData("P");
                }catch (Exception e)
                {
                    msg("ERROR - No Bluetooth connection");
                }
                break;
            case R.id.radioButton2:
                if(checked)
                    radioGroup2.clearCheck();
                radioGroup3.clearCheck();
                try
                {
                    sendData("Q");
                }catch (Exception e)
                {
                    msg("ERROR - No Bluetooth connection");
                }
                break;
            case R.id.radioButton3:
                if(checked)
                    radioGroup2.clearCheck();
                radioGroup3.clearCheck();
                try
                {
                    sendData("A");
                }catch (Exception e)
                {
                    msg("ERROR - No Bluetooth connection");
                }
                break;
            case R.id.radioButton4:
                if(checked)
                    radioGroup1.clearCheck();
                radioGroup3.clearCheck();
                try
                {
                    sendData("S");
                }catch (Exception e)
                {
                    msg("ERROR - No Bluetooth connection");
                }
                break;
            case R.id.radioButton5:
                if(checked)
                    radioGroup1.clearCheck();
                radioGroup3.clearCheck();
                try
                {
                    sendData("T");
                }catch (Exception e)
                {
                    msg("ERROR - No Bluetooth connection");
                }
                break;
            case R.id.radioButton6:
                if(checked)
                    radioGroup1.clearCheck();
                radioGroup3.clearCheck();
                try
                {
                    sendData("U");
                }catch (Exception e)
                {
                    msg("ERROR - No Bluetooth connection");
                }
                break;
            case R.id.radioButton7:
                if(checked)
                    radioGroup1.clearCheck();
                radioGroup2.clearCheck();
                try
                {
                    sendData("V");
                }catch (Exception e)
                {
                    msg("ERROR - No Bluetooth connection");
                }
                break;
            case R.id.radioButton8:
                if(checked)
                    radioGroup1.clearCheck();
                radioGroup2.clearCheck();
                try
                {
                    sendData("W");
                }catch (Exception e)
                {
                    msg("ERROR - No Bluetooth connection");
                }
                break;
            case R.id.radioButton9:
                if(checked)
                    radioGroup1.clearCheck();
                radioGroup2.clearCheck();
                try
                {
                    sendData("X");
                }catch (Exception e)
                {
                    msg("ERROR - No Bluetooth connection");
                }
                break;
        }
    }

}




