package com.example.user.bluetooth_communication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    //tag for printing and debugging
    private static final String TAG = "MainActivity";

    //object for enabling bluetooth and discovering devices
    BluetoothAdapter mBluetoothAdapter;
    //for sending data
    BluetoothConnectionService mBluetoothConnection;

    //declare buttons
    Button btnStartConnection;
    Button btnSend;

    //display text above joysticks
    TextView etSend;

    //arraylist returned whe attempting to pair with a device
    ParcelUuid[] mDeviceUUIDs;
    //integer being sent via bluetooth
    int sendInt = 0;
    char sendChar;

    //seekbars for joystick controls
    SeekBar simpleSeekBar;
    SeekBar simpleSeekBar1;

    //int to hold the positions of the slider joysticks
    public int sliderH;
    public int sliderV;

    //UUID codes for pairing with certain devices.
    private static final UUID DESKTOPUUID =
            UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final UUID TIVAUUID =
            UUID.fromString("c7f94713-891e-496a-a0e7-983a0946126e");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    //device object
    BluetoothDevice mBTDevice;
    //arraylist for displaying potential devices to pair with
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    //adapter for populating the list of devices.
    public DeviceListAdapter mDeviceListAdapter;
    //list view for displaying the devices.
    ListView lvNewDevices;

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

     // Broadcast Receiver for changes made to bluetooth states such as:
     //Discoverability mode on/off or expire.
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

            }
        }
    };

     //Broadcast Receiver for listing devices that are not yet paired
     //Executed by btnDiscover() method.
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                Log.d(TAG, mBTDevices.size() + "");
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };


    //Broadcast Receiver that detects bond state changes (Pairing status changes)
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice;
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };


    //destroy broadcast recievers
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
    }

    //this method is called when the app builds, instatiates objects, defines button functionality
    //and joystick event listeners.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnONOFF = (Button) findViewById(R.id.btnONOFF);
        //instatiate values defined above
        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();
        //instatiate buttons and text views
        btnStartConnection = (Button) findViewById(R.id.btnStartConnection);
        btnSend = (Button) findViewById(R.id.btnSend);
        etSend = (TextView) findViewById(R.id.textView);

        //instatiate the seekbar object from the xml
        simpleSeekBar=(SeekBar)findViewById(R.id.seekBar);
        simpleSeekBar1=(SeekBar)findViewById(R.id.seekBarVert);

        //Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);
        //instatiate the bluetooth adapter object
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //set the list item click listener
        lvNewDevices.setOnItemClickListener(MainActivity.this);
        //define the On/Off button for bluetooth
        btnONOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: enabling/disabling bluetooth.");
                enableDisableBT();
            }
        });

        //start the connection when u select a device from the list
        btnStartConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConnection();
            }
        });
        //send data test button
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //char cast to byte array
                String w = "W";
                byte[] sendTest = w.getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(sendTest);
            }
        });

        //joystick event listener
        simpleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sliderH = progress;
                etSend.setText("horz = " + sliderH + ", " + " vert = " + sliderV);
                buildSendData();
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {

                simpleSeekBar.setProgress(50);
            }
        });

        //joystick event listener
        simpleSeekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sliderV = progress;
                String message = "horz = " + sliderH + ", " + " vert = " + sliderV;
                etSend.setText(message);
                byte[] send = message.getBytes();
                buildSendData();
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(MainActivity.this, "Seek bar progress is :" + progressChangedValue,
                //Toast.LENGTH_SHORT).show();
                simpleSeekBar1.setProgress(50);
            }
        });


    }

    //create method for starting connection
    //the conncction will fail and app will crash if you haven't paired first
    public void startConnection(){
        startBTConnection(mBTDevice, DESKTOPUUID);
    }


    //starting chat service method
    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");
        mBluetoothConnection.startClient(device,uuid);
    }

    //enable and disable bluetooth button method
    public void enableDisableBT(){

        if(mBluetoothAdapter == null){
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if(!mBluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }
        if(mBluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: disabling BT.");
            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

    }

    //use the bluetooth adapter to discover devices
    public void btnDiscover(View view) {

        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");
            //check BT permissions in manifest
            checkBTPermissions();
            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
        if(!mBluetoothAdapter.isDiscovering()){
            //check BT permissions in manifest
            checkBTPermissions();
            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
    }


     //Must programmatically check the permissions for bluetooth.
     //NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
    private void checkBTPermissions() {

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){

            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");

            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{

            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");

        }
    }

    //when an item is clicked in the devices list.
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        //NOTE: Requires API 17+? I think this is JellyBean
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Trying to pair with " + deviceName);
            mBTDevices.get(i).createBond();
            mBTDevice = mBTDevices.get(i);
            mDeviceUUIDs = mBTDevice.getUuids();
            if(mDeviceUUIDs == null){
                Log.d(TAG, "UUID " + "null");
            }
            else {
                for (int j = 0; j < mDeviceUUIDs.length; j++) {
                    Log.d(TAG, "UUID " + mDeviceUUIDs[j]);
                }
            }

            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
        }
    }

    //when the joysticks moves, call this method to update and send the cooresponding byte
    public void buildSendData(){
        //data to send
        sendInt = 0;
        // declare the pieces of the final sent value
        int     hbr,        // right hbridge bit (bit 3)
                hbl,        // left hbridge bit (bit 7)
                pwmr,       // right pwm bits (bits 0-2)
                pwml,       // left pwm bits (bits 4-6)
                finr,       // final right nibble (bits 0-3)
                finl;       // final left nibble (bits 4-7)

        if(sliderV < 50){
            // right joystick is up
            hbr = 0x08; // hbridge bit is 1.

            if(sliderV > 43.75){
                pwmr = 0x00;
            } else if(sliderV > 37.5){
                pwmr = 0x01;
            } else if(sliderV > 31.25){
                pwmr = 0x02;
            } else if(sliderV > 25){
                pwmr = 0x03;
            } else if(sliderV > 18.75){
                pwmr = 0x04;
            } else if(sliderV > 12.5){
                pwmr = 0x05;
            } else if(sliderV > 6.25){
                pwmr = 0x06;
            } else {
                pwmr = 0x07;
            }
        } else if(sliderV > 50) {
            // right joystick is down
            hbr = 0x00;

            if(sliderV < 56.25){
                pwmr = 0x00;
            } else if(sliderV < 62.5){
                pwmr = 0x01;
            } else if(sliderV < 68.75){
                pwmr = 0x02;
            } else if(sliderV < 75){
                pwmr = 0x03;
            } else if(sliderV < 81.25){
                pwmr = 0x04;
            } else if(sliderV < 87.5){
                pwmr = 0x05;
            } else if(sliderV < 93.75){
                pwmr = 0x06;
            } else {
                pwmr = 0x07;
            }
        } else { //sliderV is 50
            // right joystick is at rest
            pwmr = 0x00;
            hbr = 0x08;
        }

        finr = hbr + pwmr; // construct right nibble

        if(sliderH < 50){
            // left joystick is up
            hbl = 0x00; // hbridge bit is 0.

            if(sliderH > 43.75){
                pwml = 0x00;

            } else if(sliderH > 37.5){
                pwml = 0x10;
            } else if(sliderH > 31.25){
                pwml = 0x20;
            } else if(sliderH > 25){
                pwml = 0x30;
            } else if(sliderH > 18.75){
                pwml = 0x40;
            } else if(sliderH > 12.5){
                pwml = 0x50;
            } else if(sliderH > 6.25){
                pwml = 0x60;
            } else {
                pwml = 0x70;
            }

        } else if(sliderH > 50) {
            // left joystick is down
            hbl = 0x80;
            if(sliderH < 56.25){
                pwml = 0x00;
            } else if(sliderH < 62.5){
                pwml = 0x10;
            } else if(sliderH < 68.75){
                pwml = 0x20;
            } else if(sliderH < 75){
                pwml = 0x30;
            } else if(sliderH < 81.25){
                pwml = 0x40;
            } else if(sliderH < 87.5){
                pwml = 0x50;
            } else if(sliderH < 93.75){
                pwml = 0x60;
            } else {
                pwml = 0x70;
            }
        } else {
            // left joystick is at rest
            hbl = 0x00;
            pwml = 0x00;
        }
        finl = pwml + hbl;


        //send fin
        sendInt |= (finl | finr);
        sendChar = (char) sendInt;

        //convert the char to byte array
        byte[] sendData = charToBytes(sendChar);

        String send = "W";

        Log.d(TAG, "Send int data: " + sendInt);

        Log.d(TAG, "Send Data byte array: " + sendData);
        //send the byte array
        mBluetoothConnection.write(sendData);

    }


    //char to byte array method
    private byte[] charToBytes(char x) {
        String temp = new String(new char[] {x});
        try {
            return temp.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            // Log a complaint
            return null;
        }
    }



}
