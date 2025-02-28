package com.sarmale.BTAPP_rev1;
//Code used from https://github.com/The-Frugal-Engineer/ArduinoBTExamplev3.git for Bluetooth
//Code used from this https://www.youtube.com/watch?v=Z3GuccRUO5E for SPT
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.TextView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.Set;
import java.util.UUID;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;



import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    // Global variables we will use in the
    TextView textView;

    private int num = 0;
    private static final String TAG = "FrugalLogs";
    private ConnectThread connectThread = null; // Declare but don’t initialize
    private ConnectedThread connectedThread = null; // Declare but don’t initialize
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    //We will use a Handler to get the BT Connection statys
    public static Handler handler;
    private String textToShow = "";
    private final static int ERROR_READ = 0; // used in bluetooth handler to identify message update
    BluetoothDevice arduinoBTModule = null;
    UUID arduinoUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //We declare a default UUID to create the global variable

    private Button connectToDevice;
    private TextView btDevices;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkBluetoothPermissions();
        textView = findViewById(R.id.textView);

        //Intances of BT Manager and BT Adapter needed to work with BT in Android.
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        //Intances of the Android UI elements that will will use during the execution of the APP
        TextView btReadings = findViewById(R.id.btReadings);
        //TextView btDevices = findViewById(R.id.btDevices);
        btDevices = findViewById(R.id.btDevices);
        //Button connectToDevice = (Button) findViewById(R.id.connectToDevice);
        connectToDevice = findViewById(R.id.connectToDevice);
        connectToDevice.setEnabled(false);
        Button seachDevices = (Button) findViewById(R.id.seachDevices);
        Button clearValues = (Button) findViewById(R.id.refresh);
        Log.d(TAG, "Begin Execution");

        Button buttonGoToSecond = findViewById(R.id.buttonGoToSecond);

        buttonGoToSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Text to send to SecondActivity


                // Start SecondActivity and pass the text
                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                intent.putExtra("text_key", textToShow);
                startActivity(intent);
            }
        });


        //Using a handler to update the interface in case of an error connecting to the BT device
        //My idea is to show handler vs RxAndroid
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {

                    case ERROR_READ:
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        btReadings.setText(arduinoMsg);
                        break;
                }
            }
        };

        // Set a listener event on a button to clear the texts
        clearValues.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btDevices.setText("");
                btReadings.setText("");
                textView.setText(" ");
                textToShow = "";
                // Properly cancel existing connections
                if (connectedThread != null) {

                    connectedThread.cancel();
                    connectedThread = null;
                }
                if (connectThread != null) {
                    connectThread.cancel();
                    connectThread = null;
                }

                // Ensure arduinoBTModule is also reset so you must rescan
                arduinoBTModule = null;
                connectToDevice.setEnabled(false);
                //recreate();
                //seachDevices.performClick();
            }
        });

        // Create an Observable from RxAndroid
        //The code will be executed when an Observer subscribes to the the Observable
        final Observable<String> connectToBTObservable = Observable.create(emitter -> {
            Log.d(TAG, "Calling connectThread class");
            //Call the constructor of the ConnectThread class
            //Passing the Arguments: an Object that represents the BT device,
            // the UUID and then the handler to update the UI
            //ConnectThread connectThread = new ConnectThread(arduinoBTModule, arduinoUUID, handler);
            connectThread.start();
            //Check if Socket connected
            if (connectThread.getMmSocket().isConnected()) {
                Log.d(TAG, "Calling ConnectedThread class");
                //The pass the Open socket as arguments to call the constructor of ConnectedThread
                //ConnectedThread connectedThread = new ConnectedThread(connectThread.getMmSocket());
                connectedThread.start();
                if (connectedThread.getValueRead() != null) {
                    // If we have read a value from the Arduino
                    // we call the onNext() function
                    //This value will be observed by the observer
                    emitter.onNext(connectedThread.getValueRead());
                }
                //We just want to stream 1 value, so we close the BT stream
                connectedThread.cancel();
            }
            // SystemClock.sleep(5000); // simulate delay
            //Then we close the socket connection
            connectThread.cancel();
            //We could Override the onComplete function
            emitter.onComplete();

        });

        connectToDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btReadings.setText("");

                // Ensure arduinoBTModule is not null
                if (arduinoBTModule != null) {

                    // Check if connectThread already exists
                    if (connectThread == null) {
                        // Create a new connectThread only if it's not already created
                        connectThread = new ConnectThread(arduinoBTModule, arduinoUUID, handler);
                        connectThread.start();  // Start the connection thread

                        // Use a handler or delay to check if the socket is connected
                        new Handler().postDelayed(() -> {
                            // Ensure the socket is connected before starting the ConnectedThread
                            if (connectThread.getMmSocket() != null && connectThread.getMmSocket().isConnected()) {
                                if (connectedThread == null) {
                                    connectedThread = new ConnectedThread(connectThread.getMmSocket());
                                    connectedThread.start();  // Start the communication thread
                                }
                            } else {
                                Log.e(TAG, "Connection failed: Socket not connected.");
                            }
                        }, 3000); // Adjust the delay if needed
                    }
                } else {
                    Log.e(TAG, "No HC-05 device found. Please pair the device first.");
                }
            }
        });


        seachDevices.setOnClickListener(new View.OnClickListener() {
            //Display all the linked BT Devices
            @Override
            public void onClick(View view) {

                seachDevices.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        searchDevices();  // Just call the method to search for devices
                    }
                });


                if (bluetoothAdapter == null) {
                    Log.d(TAG, "Device doesn't support Bluetooth");
                    return;
                }

                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    return;
                }

                String btDevicesString = "";
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress();
                        btDevicesString += deviceName + " || " + deviceHardwareAddress + "\n";

                        if (deviceName.equals("HC-05")) { // If HC-05 is found
                            Log.d(TAG, "HC-05 found, saving device.");
                            if (device.getUuids() != null && device.getUuids().length > 0) {
                                arduinoUUID = device.getUuids()[0].getUuid();
                            } else {
                                Log.e(TAG, "No UUIDs found for device: " + device.getName());
                            }
                            arduinoBTModule = device;
                            connectToDevice.setEnabled(true); // Enable connect button
                        }
                    }
                    btDevices.setText(btDevicesString);
                } else {
                    Log.d(TAG, "No paired devices found.");
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Bluetooth permissions granted.");
            } else {
                Log.e(TAG, "Bluetooth permissions denied.");
            }
        }
    }

    private void searchDevices() {
        // Check and request permissions first if needed
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN },
                    REQUEST_BLUETOOTH_PERMISSIONS);
            return;  // Stop execution until permission is granted
        }

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.d(TAG, "Device doesn't support Bluetooth");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        String btDevicesString = "";

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                btDevicesString += deviceName + " || " + deviceHardwareAddress + "\n";

                if (deviceName.equals("HC-05")) { // If HC-05 is found
                    Log.d(TAG, "HC-05 found, saving device.");
                    if (device.getUuids() != null && device.getUuids().length > 0) {
                        arduinoUUID = device.getUuids()[0].getUuid();
                    } else {
                        Log.e(TAG, "No UUIDs found for device: " + device.getName());
                    }
                    arduinoBTModule = device;
                    connectToDevice.setEnabled(true); // Enable the button once the device is found
                }
            }
            btDevices.setText(btDevicesString);
        } else {
            Log.d(TAG, "No paired devices found.");
        }
    }


    private void checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        REQUEST_BLUETOOTH_PERMISSIONS);
            }
        }
    }

    public void speak(View view) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Start Speaking");
        startActivityForResult(intent, 100);
    }
    //This gives you the text data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String stopgap = "";
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            String recognizedText = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
            textView.setText(recognizedText);
            stopgap = recognizedText;
            LocalDateTime current = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formatted = current.format(formatter);

            textToShow = textToShow + formatted + ":   " + recognizedText + '\n';

            if (arduinoBTModule != null) {
                // Ensure Bluetooth is connected before sending data
                if (connectedThread != null && connectThread.getMmSocket().isConnected()) {
                    String message = textView.getText().toString() + "\n";
                    connectedThread.write(message.getBytes());

                    Log.d(TAG, "Sent to HC-05: " + message);
                } else {
                    Log.e(TAG, "No active Bluetooth connection.");
                }
            } else {
                Log.e(TAG, "No HC-05 device found. Scan for devices first.");
            }
            if(!stopgap.equals("stop")){
                speak(null);
            }
        }
        else {
            // If resultCode is not RESULT_OK, speech recognition failed
            Log.e(TAG, "Speech recognition failed. Retrying...");
            speak(null); // Automatically retry
        }
    }
}