package com.example.salfino.naviblind_110217;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.IARegion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private int[] textCommands = {
            R.string.welcome,
            R.string.main_door,
            R.string.after_main_door,
            R.string.four_steps,
            R.string.narrow_corridor,
            R.string.two_steps,
            R.string.at_two_steps,
            R.string.end_point,
            R.string.response_yes,
            R.string.response_no,
            R.string.yes_or_no,
            R.string.repeat,
            R.string.please_repeat,
            R.string.welcome_two,
            R.string.empty,
            R.string.no_service,
            R.string.try_again,
            R.string.start_point,
            R.string.narrow_open_doorway
    };

    private int[] audioCommands = {
            R.raw.after_main_door,
            R.raw.at_two_steps,
            R.raw.end_point,
            R.raw.four_steps,
            R.raw.main_door,
            R.raw.narrow_corridor,
            R.raw.please_repeat,
            R.raw.repeat,
            R.raw.response_no,
            R.raw.response_yes,
            R.raw.two_steps,
            R.raw.welcomemale,
            R.raw.yes_or_no,
            R.raw.welcomemaletwo,
            R.raw.alert,
            R.raw.service_not_ready,
            R.raw.try_again,
            R.raw.start_position,
            R.raw.tick,
            R.raw.metal_metronome,
            R.raw.narrow_open_door
    };
    //Waypoint geo-coordinates in decimal degrees (DD)
    private static final double GR_OFFICE_LAT = 51.52222145;
    private static final double GR_OFFICE_LON = -0.13049584;
    private static final double START_POSITION_LAT = 51.52231720;
    private static final double START_POSITION_LON = -0.13089649;
    private static final double FOUR_STEPS_LAT = 51.52221143;
    private static final double FOUR_STEPS_LON = -0.13077848;
    private static final double TWO_STEPS_LAT = 51.52228758;
    private static final double TWO_STEPS_LON = -0.13059912;
    private static final double MAIN_DOOR_LAT = 51.52213759;
    private static final double MAIN_DOOR_LON = -0.13069935;
    private static final int REQUEST_CODE = 1234;
    private final int MY_CODE_PERMISSIONS = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_COARSE_BL = 2;
    private long DEFAULT_INTERVAL = 100L;//milliseconds
    private float DEFAULT_DISPLACEMENT = 0.2f;//meters
    public IALocationManager mIALocationManager;
    public MediaPlayer mPlayer;
    public SpeechRecognizer mSR;
    public BluetoothAdapter mBTAdapter;
    public static Set<MediaPlayer> activePlayers = new HashSet<MediaPlayer>();
    //private Button mLocationButton;
    //private Button mStopButton;
    private Button mYes;
    private Button mNo;
    //private Button mlaunchFloorPlan;
    private TextView mLogging;
    private TextView mTextView;
    private ScrollView mScrollView;
    private static final String TAG = "IndoorAtlas";
    private long mRequestStartTime;
    private int myStartFlag = 2;
    private boolean calibrationOK = false;
    private boolean statusOK = false;
    private boolean permissionOK = false;

    BluetoothGatt mBluetoothGatt;
    BluetoothLeScanner scanner;
    ScanSettings scanSettings;
    public boolean mScanning;
    public Handler mHandler;
    public static final long SCAN_PERIOD = 10000;
    String dName = "";
    String macAddress = "";
    public double rssiGR = -17;
    public double rssiSR = -17;
    public double metersGR = 0;
    public double metersSR = 0;

    private void logText(String msg) {
        double duration = mRequestStartTime != 0
                ? (SystemClock.elapsedRealtime() - mRequestStartTime) / 1e3
                : 0d;
        //mLogging.append(String.format(Locale.UK, "\n[%06.2f]: %s", duration, msg));
        //mLogging.setText(String.format(Locale.UK, "\n[%06.2f]:\n %s", duration, msg));
        mLogging.setText(String.format(Locale.UK, "\n %s", msg));
        mLogging.setTextSize(35);
        mLogging.setTextColor(0xFFFF8290);
        mScrollView.smoothScrollBy(0, mLogging.getBottom());
    }

    //Method to convert a byte array to a HEX. string.
    private String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for(byte b: a)
            sb.append(String.format("%02x", b & 0xff));

        return sb.toString();
    }

    public void initialiseBLE(){

        final BluetoothManager bluetoothManager =  (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBTAdapter = bluetoothManager.getAdapter();//Get the Bluetooth Adapter first
        //Create the scan settings
        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
        //Set scan latency mode. Lower latency, faster device detection/more battery and resources consumption
        scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        //Wrap settings together and save on a settings var (declared globally).
        scanSettings = scanSettingsBuilder.build();
        //Get the BLE scanner from the BT adapter (var declared globally)
        scanner = mBTAdapter.getBluetoothLeScanner();
    }
    private void startLeScan(boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
           /* mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    scanner.stopScan(mScanCallback);
                    mTest.setText("Stopped Scanning...");
                }
            }, SCAN_PERIOD);*/
            //********************
            //START THE BLE SCAN
            //********************
            //Scanning parameters FILTER / SETTINGS / RESULT CALLBACK. Filter are used to define a particular
            //device to scan for. The Callback is defined above as a method.
            mScanning = true;
            scanner.startScan(null, scanSettings, mScanCallback);
            //Toast.makeText(BLEScanner.this, "SCANNING FOR BLE DEVICES...", Toast.LENGTH_SHORT).show();
        }else{
            //Stop scan
            mScanning = false;
            scanner.stopScan(mScanCallback);
            //Toast.makeText(BLEScanner.this, "SCANNING STOPPED...", Toast.LENGTH_SHORT).show();
        }
    }

    private double getDistance(double rssi, String location) {//RSSI (dBm) = -10n log10(d) + A and n = 2 for free space and A is average RSSI at 1m
        double A = -60.0;
        if (location.equals("GC")){
            A = -60.0;// average RSSI for beacon installed in George Roussos office
        } else if (location.equals("SR")) {
            A =  -50.0; // average RSSI for beacon isntalled in Staff Room, next to lifts
        }
        return Math.pow(10.0,((rssi-(A))/-25.0));//-60dBm is average RSSI at 1m distance i.e. A
    }

    //Finding BLE Devices
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            String advertisingString = byteArrayToHex(result.getScanRecord().getBytes());
            dName = result.getDevice().getName();
            if (dName != null){
                dName = (result.getDevice().getName()).trim();
            }
            macAddress = result.getDevice().getAddress();
            if (macAddress != null){
                macAddress = (result.getDevice().getAddress()).trim();
            }

            if (macAddress != null && macAddress.equals("F4:46:EA:8F:C2:2D")) {
                rssiGR = result.getRssi();
                metersGR = getDistance(rssiGR,"GC");

            } else if (macAddress != null && macAddress.equals("C3:4E:E7:D1:2E:3A")) {
                rssiSR = result.getRssi();
                metersSR = getDistance(rssiSR,"SR");
            }
            else

            {
                //Do nothing
            }

            /*if (dName != null && macAddress != null) {
                if (dName.equals("iBKS105") && macAddress.equals("F4:46:EA:8F:C2:2D")) {
                    mTest.setText(String.format(Locale.UK, "RSSI: %d, \nAdvertisment: %s, \nMAC Address: %s, \nDevice Name: %s",
                        result.getRssi(), advertisingString, result.getDevice().getAddress(), result.getDevice().getName()));
                } else {
                     mTest.setText("Scanning for GR Waypoint Device...");
            }
            } else {
                mTest.setText("Reading NULL...");
            }*/

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    private void initialiseBluetooth(){

        //Check if device does support BT by hardware
        if (!getBaseContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            //Toast shows a message on the screen for a LENGTH_SHORT period
            Toast.makeText(this, "BLUETOOTH NOT SUPPORTED!", Toast.LENGTH_SHORT).show();
            finish();
        }

        //Check if device does support BT Low Energy by hardware. Else close the app(finish())!
        if (!getBaseContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //Toast shows a message on the screen for a LENGTH_SHORT period
            Toast.makeText(this, "BLE NOT SUPPORTED!", Toast.LENGTH_SHORT).show();
            finish();
        }else {
            //If BLE is supported, get the BT adapter. Preparing for use!
            mBTAdapter = BluetoothAdapter.getDefaultAdapter();
            //If getting the adapter returns error, close the app with error message!
            if (mBTAdapter == null) {
                Toast.makeText(this, "ERROR GETTING BLUETOOTH ADAPTER!", Toast.LENGTH_SHORT).show();
                finish();
            }else{
                //Check if BT is enabled! This method requires BT permissions in the manifest.
                if (!mBTAdapter.isEnabled()) {
                    //If it is not enabled, ask user to enable it with default BT enable dialog! BT enable response will be received in the onActivityResult method.
                    Intent enableBTintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBTintent, PERMISSION_REQUEST_COARSE_BL);
                }
            }
        }
    }

    private void timer(int seconds){

        new CountDownTimer(seconds, 1000){// x seconds count down timer
            @Override
            public void onTick(long millisUntilFinished) {
                Toast.makeText(MainActivity.this, "Timer On...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                Toast.makeText(MainActivity.this, "Timer Stopped.", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void tryAgain(){

        new CountDownTimer(2000, 1000){//2 second count down timer
            @Override
            public void onTick(long millisUntilFinished) {
                //
            }
            @Override
            public void onFinish() {
                displayTextTwo(14,14);//Pre-audio alert
                new CountDownTimer(2000, 1000){//2 second count down timer
                    @Override
                    public void onTick(long millisUntilFinished) {
                        //
                    }
                    @Override
                    public void onFinish() {
                        displayTextTwo(16,16);//Try again audio
                        new CountDownTimer(6000, 1000){//10 second count down timer
                            @Override
                            public void onTick(long millisUntilFinished) {
                                //Toast.makeText(MainActivity.this, "Timer On...", Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onFinish() {
                                //Toast.makeText(MainActivity.this, "launch Speech Recognition debug.", Toast.LENGTH_SHORT).show();
                                startVoiceRecognitionActivity();//Just temporary action
                            }
                        }.start();
                    }
                }.start();
            }
        }.start();
    }

    private void displayText (int textCommandIndex, String voiceCommandURL) {

        mLogging.setText("");
        mLogging.setText(textCommands[textCommandIndex]);
        mLogging.setTextSize(30);
        mLogging.setTextColor(0xFFFF4046);
        mScrollView.smoothScrollBy(0, mLogging.getBottom());
        //MediaPlayer mPlayer = MediaPlayer.create(this,R.raw.welcome);
        try {
            mPlayer = new MediaPlayer();
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setDataSource(voiceCommandURL);
            mPlayer.prepare();
            mPlayer.start();

        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Check Wi-Fi connection or audio file missing!!", Toast.LENGTH_LONG).show();
            //MediaPlayer mPlayer = MediaPlayer.create(this,R.raw.welcome);
            //mPlayer.start();
        }
    }

    private void confirmText (final int textCommandIndex, final int audioCommandIndex) {

        mLogging.setText("");
        mLogging.setText(textCommands[textCommandIndex]);
        mLogging.setTextSize(30);
        mLogging.setTextColor(0xFFFF4046);
        mScrollView.smoothScrollBy(0, mLogging.getBottom());

        try {
            mPlayer = MediaPlayer.create(this,audioCommands[audioCommandIndex]);
            activePlayers.add(mPlayer);//Garbage collector issue ....keeping at least one pointer to the instance somewhere
            AudioAttributes myAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            mPlayer.setAudioAttributes(myAttributes);
            mPlayer.setLooping(false);
            mPlayer.start();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.stop();
                    activePlayers.remove(mp);
                    mp.release();
                    mPlayer = null;
                    if (textCommandIndex == 3) {
                        startVoiceRecognitionActivity();//Launch speech recogniser
                    }else if (textCommandIndex == 5){
                        startVoiceRecognitionActivity();//Launch speech recogniser
                    }else if (textCommandIndex == 18) {
                        startVoiceRecognitionActivity();//Launch speech recogniser
                    } else {
                        Toast.makeText(MainActivity.this, "PLAYBACK COMPLETE - UNDEFINED!!!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Toast.makeText(MainActivity.this, "ERROR!!!!!!", Toast.LENGTH_SHORT).show();
                    switch (what) {
                        case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                            switch (extra) {
                                case MediaPlayer.MEDIA_ERROR_IO:
                                    break;
                                case MediaPlayer.MEDIA_ERROR_MALFORMED:
                                    break;
                                case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                                    break;
                                case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                                    break;
                            }
                            logText("ERROR: " + "What Code: " + what + "Extra Code: " +extra);
                            break;
                        case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                            switch (extra) {
                                case MediaPlayer.MEDIA_ERROR_IO:
                                    break;
                                case MediaPlayer.MEDIA_ERROR_MALFORMED:
                                    break;
                                case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                                    break;
                                case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                                    break;
                            }
                            logText("ERROR: " + "What Code: " + what + "Extra Code: " +extra);
                            break;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Check Wi-Fi connection or audio file missing!!", Toast.LENGTH_LONG).show();
            //MediaPlayer mPlayer = MediaPlayer.create(this,R.raw.welcome);
            //mPlayer.start();
        }
    }

    private void displayTextTwo (final int textCommandIndex, final int audioCommandIndex) {

        mLogging.setText("");
        mLogging.setText(textCommands[textCommandIndex]);
        mLogging.setTextSize(30);
        mLogging.setTextColor(0xFFFF4046);
        mScrollView.smoothScrollBy(0, mLogging.getBottom());

        try {
            mPlayer = MediaPlayer.create(this,audioCommands[audioCommandIndex]);
            /*mPlayer = new MediaPlayer();
            AssetFileDescriptor afd = getApplicationContext().getResources().openRawResourceFd(audioCommands[audioCommandIndex]);
            //Uri mediaPath = Uri.parse("android.resource://" + getPackageName() + "/" + audioCommands[audioCommandIndex]);
            mPlayer.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
            afd.close();*/
            activePlayers.add(mPlayer);//Garbage collector issue ....keeping at least one pointer to the instance somewhere
            AudioAttributes myAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            mPlayer.setAudioAttributes(myAttributes);
            mPlayer.setLooping(false);
            /*mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {//Running media player on a separate UI thread
                @Override
                public void onPrepared(MediaPlayer mp) {//called when media is done preparing
                    Toast.makeText(MainActivity.this, "PLAYBACK START!!!!!!", Toast.LENGTH_SHORT).show();
                    mp.start();
                }
            });*/
            //mPlayer.prepareAsync();//Prepares media in the background
            mPlayer.start();
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.stop();
                    activePlayers.remove(mp);
                    mp.release();
                    mPlayer = null;
                    if (textCommandIndex == 13) {
                        startVoiceRecognitionActivity();//Launch speech recogniser
                    }else if (textCommandIndex == 14){
                        //Do nothing
                    }else if (textCommandIndex == 17) {
                        new CountDownTimer(5000, 1000) {// 5 seconds count down timer
                            @Override
                            public void onTick(long millisUntilFinished) {
                            }

                            @Override
                            public void onFinish() {
                                confirmText(3, 3);//Confirm 4 steps
                            }
                        }.start();
                    }else if (textCommandIndex == 1){
                            new CountDownTimer(5000, 1000){// 5 seconds count down timer
                                @Override
                                public void onTick(long millisUntilFinished) {
                                }

                                @Override
                                public void onFinish() {
                                    confirmText(3,3);//Confirm 4 steps also after turning back
                                }
                            }.start();
                        //mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener);
                    }else if (textCommandIndex == 4){
                        new CountDownTimer(5000, 1000){// 5 seconds count down timer
                            @Override
                            public void onTick(long millisUntilFinished) {
                            }

                            @Override
                            public void onFinish() {
                                confirmText(5,10);//Confirm 2 steps
                            }
                        }.start();
                        //mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener);
                    }else if (textCommandIndex == 6){
                        new CountDownTimer(5000, 1000){// 5 seconds count down timer
                            @Override
                            public void onTick(long millisUntilFinished) {
                            }

                            @Override
                            public void onFinish() {
                                confirmText(18,20);//Confirm final destination
                            }
                        }.start();
                       // mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener);
                    }else if (textCommandIndex == 7){
                        //mainMenu();
                       // mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener);
                    }
                    else {
                        //Toast.makeText(MainActivity.this, "PLAYBACK COMPLETE - UNDEFINED!!!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Toast.makeText(MainActivity.this, "ERROR!!!!!!", Toast.LENGTH_SHORT).show();
                    switch (what) {
                        case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                            switch (extra) {
                                case MediaPlayer.MEDIA_ERROR_IO:
                                    break;
                                case MediaPlayer.MEDIA_ERROR_MALFORMED:
                                    break;
                                case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                                    break;
                                case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                                    break;
                            }
                            logText("ERROR: " + "What Code: " + what + "Extra Code: " +extra);
                            break;
                        case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                            switch (extra) {
                                case MediaPlayer.MEDIA_ERROR_IO:
                                    break;
                                case MediaPlayer.MEDIA_ERROR_MALFORMED:
                                    break;
                                case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                                    break;
                                case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                                    break;
                            }
                            logText("ERROR: " + "What Code: " + what + "Extra Code: " +extra);
                            break;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Check Wi-Fi connection or audio file missing!!", Toast.LENGTH_LONG).show();
            //MediaPlayer mPlayer = MediaPlayer.create(this,R.raw.welcome);
            //mPlayer.start();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Toast.makeText(MainActivity.this, "DEBUG::onCreate() callback...", Toast.LENGTH_LONG).show();

        /*if (savedInstanceState != null) {
            mFastestInterval = savedInstanceState.getLong(FASTEST_INTERVAL);
            mShortestDisplacement = savedInstanceState.getFloat(SHORTEST_DISPLACEMENT);
        }*/
        String[] neededPermissions = {
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO
        };
        ActivityCompat.requestPermissions( this, neededPermissions, MY_CODE_PERMISSIONS);
        setContentView(R.layout.activity_main);
       // mLocationButton = (Button) findViewById(R.id.locationButton);
        //mStopButton = (Button) findViewById(R.id.stopButton);
        //mlaunchFloorPlan = (Button) findViewById(R.id.launchPlan);
        mYes = (Button) findViewById(R.id.locationButton);//IMP:Rename id to a more meaningful name
        mNo = (Button) findViewById(R.id.stopButton);//IMP:Rename id to a more meaningful name
        mLogging = (TextView) findViewById(R.id.mytextView);
        mTextView = (TextView) findViewById(R.id.coordinates);
        mTextView.setTextSize(15);
        mTextView.setTextColor(0xFFFF4046);
        mScrollView = (ScrollView) findViewById(R.id.myscrollView);

        //Create a new instance of IALocationManager using its create() method
        mIALocationManager = IALocationManager.create(this);
        IALocation location = new IALocation.Builder().withLatitude(START_POSITION_LAT)
                                                      .withLongitude(START_POSITION_LON)
                                                      .withAccuracy(75f)
                                                      .withFloorLevel(2).build();
        mIALocationManager.setLocation(location);//Explicitly set the the initial fix as specified above
        //Toast.makeText(MainActivity.this, "DEBUG::First fix location set...", Toast.LENGTH_LONG).show();

        IALocationRequest request = IALocationRequest.create();
        request.setPriority(IALocationRequest.PRIORITY_HIGH_ACCURACY);//High-accuracy updates requested
        request.setFastestInterval(DEFAULT_INTERVAL);//Explicitly set the fastest interval for location updates in milliseconds
        request.setSmallestDisplacement(DEFAULT_DISPLACEMENT);//Set the minimum displacement between location updates in meters
        //Toast.makeText(MainActivity.this, "DEBUG::Interval & Displacement set...", Toast.LENGTH_LONG).show();

        //Create a new instance of SpeechRecognizer using its createSpeechRecognizer() method
        mSR = SpeechRecognizer.createSpeechRecognizer(this);
        //Set the Speech Listener as the new speechListener defined in inner class below
        mSR.setRecognitionListener(new speechListener());

        mYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        //introduction();
        initialiseBluetooth();
        initialiseBLE();

        //Intent serviceIntent = new Intent(MainActivity.this, BLEScanner.class);//Start BLE Scanning Service
        //startService(serviceIntent);//Start background BLE scanning
        //startService();

         //mBLEReceiver = new BLEReceiver();//Register Broadcast Receiver to receive data from BLE Service
         //IntentFilter intentFilter = new IntentFilter();
         //intentFilter.addAction(BLEScanner.BLE_ACTION);
         //registerReceiver(mBLEReceiver, intentFilter);

        displayTextTwo(14,14);
        new CountDownTimer(3000, 1000){// 3 seconds count down timer
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                displayTextTwo(13,13);
            }
        }.start();

        //displayText(0,"https://naviblind.000webhostapp.com/welcomemale.mp3");

        /*mLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "getLocation Button pressed!", Toast.LENGTH_LONG).show();
                mRequestStartTime = SystemClock.elapsedRealtime();
                mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener);
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "stopLocation Button pressed!", Toast.LENGTH_LONG).show();
                mLogging.setText("");
                mIALocationManager.removeLocationUpdates(mIALocationListener);
                mIALocationManager.destroy();
            }
        });*/

       /* mlaunchFloorPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "launch Floor Plan Button pressed!", Toast.LENGTH_LONG).show();
                Intent i = new Intent(MainActivity.this, FloorPlanActivity.class);
                //Intent i = new Intent(MainActivity.this, TestActivity.class);
                startActivity(i);
            }
        });*/

        /*new CountDownTimer(2000, 1000){
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                displayText(0,"https://iwtcourseworksa.000webhostapp.com/welcomemale.mp3");
            }
        }.start();

        new CountDownTimer(20000, 1000){
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                displayText(1,"https://iwtcourseworksa.000webhostapp.com/main_door.mp3");
            }
        }.start();*/

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_CODE_PERMISSIONS: {// If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) { // permission was granted

                    Toast.makeText(MainActivity.this, "Permission granted for coarse location and Wi-Fi status", Toast.LENGTH_SHORT).show();
                    permissionOK = true;
                } else {// permission denied

                    Toast.makeText(MainActivity.this, "Permission denied for coarse location and Wi-Fi status", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }

    }

    public IALocationListener mIALocationListener = new IALocationListener() {
      //Implement an IALocationListener interface and override its onLocationChanged() callback method
      @Override
      public void onLocationChanged(IALocation iaLocation) {
          //Toast.makeText(MainActivity.this, "Location Changing...", Toast.LENGTH_SHORT).show();
          mTextView.setText(String.format(Locale.UK, "Latitude: %.8f,\nLongitude: %.8f,\nAccuracy: %.8f,\nCertainty: %.8f,\nLevel: %d,\nBLE DISTANCE SR: %.8f,\nBLE DISTANCE GR: %.8f",
                  iaLocation.getLatitude(), iaLocation.getLongitude(),iaLocation.getAccuracy(),iaLocation.getFloorCertainty(),
                  iaLocation.getFloorLevel(),metersSR,metersGR));
          //mTextView.setText(String.valueOf(iaLocation.getLatitude() + ", " + iaLocation.getLongitude()));
          //Location updates being delivered here
          mTextView.setTextSize(15);
          //Below shows possible way of implementing voice and text triggers
          Haversine havObject = new Haversine(); //Create the Haversine object
          double currentDistance_SP = 1000*(havObject.distance(iaLocation.getLatitude(),iaLocation.getLongitude(),START_POSITION_LAT,START_POSITION_LON));
          double currentDistance_4S = 1000*(havObject.distance(iaLocation.getLatitude(),iaLocation.getLongitude(),FOUR_STEPS_LAT,FOUR_STEPS_LON));
          double currentDistance_2S = 1000*(havObject.distance(iaLocation.getLatitude(),iaLocation.getLongitude(),TWO_STEPS_LAT,TWO_STEPS_LON));
          double currentDistance_GR = 1000*(havObject.distance(iaLocation.getLatitude(),iaLocation.getLongitude(),GR_OFFICE_LAT,GR_OFFICE_LON));
          double currentDistance_MD = 1000*(havObject.distance(iaLocation.getLatitude(),iaLocation.getLongitude(),MAIN_DOOR_LAT,MAIN_DOOR_LON));

          //mLogging.setText("");
          mLogging.setText(String.valueOf(" SP: " + currentDistance_SP + ",\n MD: " + currentDistance_MD + ",\n 4S: " + currentDistance_4S + ",\n 2S: " + currentDistance_2S + ",\n GR: " + currentDistance_GR));
          mLogging.setTextSize(20);
          mLogging.setTextColor(0xFFFF4046);
          mScrollView.smoothScrollBy(0, mLogging.getBottom());

          //if (calibrationOK && statusOK && permissionOK && iaLocation.getFloorLevel() == 2 && iaLocation.getAccuracy()<=15) {
          if (iaLocation.getFloorLevel() == 2 && iaLocation.getAccuracy()<=8) {
              //Toast.makeText(MainActivity.this, "SERVICE RUNNING OK", Toast.LENGTH_SHORT).show();

            if (currentDistance_SP <= 5) {
                // displayText(0,"https://naviblind.000webhostapp.com/welcomemale.mp3");
                //mIALocationManager.removeLocationUpdates(mIALocationListener);
                //Toast.makeText(MainActivity.this, "START POSITION", Toast.LENGTH_SHORT).show();
                mIALocationManager.removeLocationUpdates(mIALocationListener);
                startLeScan(false);
                displayTextTwo(17,17);

            } else if (currentDistance_4S <=5){
                //displayText(4,"https://naviblind.000webhostapp.com/narrow_corridor.mp3");
                //Toast.makeText(MainActivity.this, "FOUR STEPS", Toast.LENGTH_SHORT).show();
                mIALocationManager.removeLocationUpdates(mIALocationListener);
                startLeScan(false);
                displayTextTwo(4,5);

            } else if (currentDistance_2S <=5) {
                //displayText(6,"https://naviblind.000webhostapp.com/at_two_steps.mp3");
                //Toast.makeText(MainActivity.this, "TWO STEPS", Toast.LENGTH_SHORT).show();
                mIALocationManager.removeLocationUpdates(mIALocationListener);
                startLeScan(false);
                displayTextTwo(6,1);

            } else if (currentDistance_MD <=5 || (metersSR <=4 && metersSR > 0)) {
                //displayText(6,"https://naviblind.000webhostapp.com/at_two_steps.mp3");
                //Toast.makeText(MainActivity.this, "NEAR LIFTS", Toast.LENGTH_SHORT).show();
                mIALocationManager.removeLocationUpdates(mIALocationListener);
                startLeScan(false);
                displayTextTwo(1,4);

            } else if (currentDistance_GR <=5 || (metersGR <=3 && metersSR > 0)) {
                //displayText(7,"https://naviblind.000webhostapp.com/end_point.mp3");
                //Toast.makeText(MainActivity.this, "FINAL DESTINATION", Toast.LENGTH_SHORT).show();
                mIALocationManager.removeLocationUpdates(mIALocationListener);
                startLeScan(false);
                displayTextTwo(7,2);
            }

          } else {
              displayTextTwo(15,18);
              //Toast.makeText(MainActivity.this, "WAIT FOR SERVICE!!!!!", Toast.LENGTH_SHORT).show();
          }
      }

      @Override
      public void onStatusChanged(String provider, int status, Bundle bundle) {
          switch (status) {
              case IALocationManager.STATUS_CALIBRATION_CHANGED:
                  String quality = "unknown";
                  switch (bundle.getInt("quality")) {
                      case IALocationManager.CALIBRATION_POOR:
                          quality = "Poor";
                          calibrationOK = false;
                          break;
                      case IALocationManager.CALIBRATION_GOOD:
                          quality = "Good";
                          calibrationOK = true;
                          break;
                      case IALocationManager.CALIBRATION_EXCELLENT:
                          quality = "Excellent";
                          calibrationOK = true;
                          break;
                  }
                  //logText("Calibration Quality: " + quality + " Status Code: " + status);
                  logText("Calibration: " + quality);
                  break;
              case IALocationManager.STATUS_AVAILABLE:
                  //logText("onStatusChanged: Available" + " Status Code: " + status);
                  logText("Status: Available");
                  statusOK = true;
                  break;
              case IALocationManager.STATUS_LIMITED:
                  logText("Status: Limited");
                  statusOK = true;
                  break;
              case IALocationManager.STATUS_OUT_OF_SERVICE:
                  logText("Status: Out of service");
                  statusOK = false;
                  break;
              case IALocationManager.STATUS_TEMPORARILY_UNAVAILABLE:
                  logText("Status: Temporarily unavailable");
                  statusOK = false;
          }
      }
  };

    private IARegion.Listener mRegionListener = new IARegion.Listener() {
        @Override
        public void onEnterRegion(IARegion iaRegion) {
            if (iaRegion.getType() == IARegion.TYPE_FLOOR_PLAN) {
                String id = iaRegion.getId();
                Log.d(TAG, "floorPlan changed to " + id);
                //Toast.makeText(MainActivity.this, "REGION CHANGE: " + id, Toast.LENGTH_SHORT).show();
                IALocation location = new IALocation.Builder()
                        .withFloorLevel(2).build();
                mIALocationManager.setLocation(location);//Explicitly set floor level to 2
            }
        }

        @Override
        public void onExitRegion(IARegion iaRegion) {
            // leaving a previously entered region
        }
    };
    @Override
    protected void onResume() {
        if(mPlayer != null)
        {
            mPlayer.start();
        }
        super.onResume();
        //Toast.makeText(MainActivity.this, "DEBUG::onResume() callback...", Toast.LENGTH_LONG).show();
        mRequestStartTime = SystemClock.elapsedRealtime();
        //mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener);
        mIALocationManager.registerRegionListener(mRegionListener);
    }

    @Override
    protected void onPause() {
        if(mPlayer != null && mPlayer.isPlaying())
        {
            mPlayer.pause();
        }
        super.onPause();
        //Toast.makeText(MainActivity.this, "DEBUG::onPause() callback...", Toast.LENGTH_LONG).show();
        mIALocationManager.removeLocationUpdates(mIALocationListener);
        mIALocationManager.unregisterRegionListener(mRegionListener);
        //stopService();
        //mPlayer.pause();
        //mPlayer.release();//releasing and nullifying MediaPLayer
        //mPlayer = null;
    }

    @Override
    protected void onDestroy() {
        //Toast.makeText(MainActivity.this, "DEBUG::onDestroy() callback...", Toast.LENGTH_LONG).show();
        mIALocationManager.destroy();
        startLeScan(false);
        mPlayer.release();//releasing and nullifying MediaPLayer
        super.onDestroy();

        //stopService();
        //unregisterReceiver(mReceiver);//Unregister broadcast receiver
        //mPlayer = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.debug_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.menu_item_floor_plan:
                Toast.makeText(MainActivity.this, "launch Floor Plan debug.", Toast.LENGTH_LONG).show();
                Intent i = new Intent(MainActivity.this, FloorPlanActivity.class);
                //Intent i = new Intent(MainActivity.this, TestActivity.class);
                startActivity(i);
                return true;
            case R.id.menu_item_BLE:
                Toast.makeText(MainActivity.this, "BLE Scanning...", Toast.LENGTH_LONG).show();
                Intent k = new Intent(MainActivity.this, BluetoothScanner.class);
                startActivity(k);
                return true;
            case R.id.menu_test_activity:
                Toast.makeText(MainActivity.this, "Testing...", Toast.LENGTH_LONG).show();
                Intent m = new Intent(MainActivity.this, TestActivity.class);
                startActivity(m);
                if(mPlayer != null && mPlayer.isPlaying())
                {
                    mPlayer.pause();
                }
                return true;
            case R.id.menu_item_test_speech:
                Toast.makeText(MainActivity.this, "launch Speech Recognition debug.", Toast.LENGTH_LONG).show();
                startVoiceRecognitionActivity();//Just temporary action
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*@Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong(FASTEST_INTERVAL, mFastestInterval);
        savedInstanceState.putFloat(SHORTEST_DISPLACEMENT, mShortestDisplacement);
        super.onSaveInstanceState(savedInstanceState);
    }*/

    private class speechListener implements RecognitionListener {

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "onReadyForSpeech");
            /*new CountDownTimer(12000, 1000){//10 second count down timer
                @Override
                public void onTick(long millisUntilFinished) {
                    Toast.makeText(MainActivity.this, "Timer On...", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFinish() {
                }
            }.start();*/
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");

        }

        @Override
        public void onRmsChanged(float rmsdB) {
            Log.d(TAG, "onRmsChanged");

        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "onBufferReceived");

        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "onEndofSpeech");
            /*new CountDownTimer(8000, 1000){//10 second count down timer
                @Override
                public void onTick(long millisUntilFinished) {
                    Toast.makeText(MainActivity.this, "Timer On...", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFinish() {
                }
            }.start();*/

        }

        @Override
        public void onError(int error) {
            Log.d(TAG,  "error " +  error);
            //Toast.makeText(MainActivity.this, "DEBUG::Recognition Listener onError()", Toast.LENGTH_LONG).show();
            mLogging.setText("");
            mLogging.setText("Error Number " + error + " occurred.");
            mLogging.setTextSize(30);
            mLogging.setTextColor(0xFFFF4046);
            mScrollView.smoothScrollBy(0, mLogging.getBottom());
            long startTime = System.currentTimeMillis();
            tryAgain();
            long estimatedTime = System.currentTimeMillis() - startTime;
            //Toast.makeText(MainActivity.this, "ESTIMATED TIME IN ms IS::" + estimatedTime, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onResults(Bundle results) {
            Log.d(TAG, "onResults " + results);

            //Toast.makeText(MainActivity.this, "DEBUG::Recognition Listener onResult()", Toast.LENGTH_SHORT).show();
            //String mystr = new String();

            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String input = data.get(0).toString();
            String myInput = input.trim();
            mLogging.setText("");
            mLogging.setText(String.format(Locale.UK, "\n %s", "You said : " + myInput));
            //mLogging.setText("You said : " + input);
            mLogging.setTextSize(30);
            mLogging.setTextColor(0xFFFF4046);
            mScrollView.smoothScrollBy(0, mLogging.getBottom());

            if (myInput.equals("I am ready to start")){
                //Toast.makeText(MainActivity.this, "DEBUG::Request Location Updates", Toast.LENGTH_SHORT).show();
                mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener);
                startLeScan(true);
                Toast.makeText(MainActivity.this, "BLE ON!", Toast.LENGTH_SHORT).show();
            }else if (myInput.equals("yes I can confirm")){
                mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener);
                startLeScan(true);
                Toast.makeText(MainActivity.this, "BLE ON!", Toast.LENGTH_SHORT).show();
            }

            else{
                long startTime = System.currentTimeMillis();
                tryAgain();
                long estimatedTime = System.currentTimeMillis() - startTime;
                //Toast.makeText(MainActivity.this, "ESTIMATED TIME IN ms IS::" + estimatedTime, Toast.LENGTH_SHORT).show();
            }
            //for (int i = 0; i < data.size(); i++)
            //{
            //   Log.d(TAG, "result " + data.get(i));
            //  mystr += data.get(i);
            //}
            // mTextView.setText("results: " + String.valueOf(data.size()));

        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "onPartialResults");

            /*Toast.makeText(MainActivity.this, "DEBUG::Recognition Listener onPartialResults()", Toast.LENGTH_LONG).show();
            String mystr = new String();
            Log.d(TAG, "onResults " + partialResults);

            ArrayList data = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            mLogging.setText("");
            mLogging.setText("You said : " + data.get(0));
            mLogging.setTextSize(30);
            mLogging.setTextColor(0xFFFF4046);
            mScrollView.smoothScrollBy(0, mLogging.getBottom());*/
            tryAgain();
            //introduction();
            //for (int i = 0; i < data.size(); i++)
            //{
             //   Log.d(TAG, "result " + data.get(i));
             //   mystr += data.get(i);
            //}
            //mTextView.setText("results: " + String.valueOf(data.size()));
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent " + eventType);
            tryAgain();

        }
    }

    /**
     * Fire an intent to start the voice recognition activity.
     */
    private void    startVoiceRecognitionActivity()
    {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Please speak now...");
        //intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"com.example.salfino.voice_test");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,1000);
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,5000);
        //Toast.makeText(MainActivity.this, "DEBUG::Launch startListening()", Toast.LENGTH_SHORT).show();
        mSR.startListening(intent);

        //startActivityForResult(intent, REQUEST_CODE);//Start an activity and get a result back
        //When user is done, onActivityResult() method is called
    }

    @Override
    //INPUT: Request code passed to startActivityForResult(), resultCode is either RESULT_OK or RESULT_CANCELED
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK)
        {
            // Populate the wordsList with the String values the recognition engine thought it heard
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            //mwordsList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, matches));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
