package com.example.salfino.naviblind_110217;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Locale;

public class TestActivity extends AppCompatActivity {

    public TextView mTest;
    public TextView mTest2;
    public final static int REQUEST_ENABLE_BT = 1;
    public BluetoothAdapter mBTAdapter;
    BluetoothGatt mBluetoothGatt;
    BluetoothLeScanner scanner;
    ScanSettings scanSettings;
    public boolean mScanning;
    public Handler mHandler;
    public static final long SCAN_PERIOD = 10000;
    String dName = "";
    String macAddress = "";
    double rssi = 0;
    double meters = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        mTest = (TextView) findViewById(R.id.textView);
        mTest.setTextSize(20);
        mTest.setTextColor(0xFFFF4046);

        mTest2 = (TextView) findViewById(R.id.textViewtwo);
        mTest2.setTextSize(20);
        mTest2.setTextColor(0xFFFF4044);

        initialiseBLE();
        startLeScan(true);
        Toast.makeText(TestActivity.this, "SCAN STARTED...", Toast.LENGTH_LONG).show();
        /*if (mScanning) {
            mTest.setText("Scanning for BLE Devices");
        }
        else{
            mTest.setText("Stopped Scanning...");
        }*/
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

        if (mBTAdapter == null || !mBTAdapter.isEnabled()) {//Enable Bluetooth
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);//Locally defined integer greater than zero
        }
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
            mTest.setText("Scanning for BLE Devices");
            mTest2.setText("Scanning for BLE Devices");
        }else{
            //Stop scan
            mScanning = false;
            scanner.stopScan(mScanCallback);
            mTest.setText("Stopped Scanning...");
            mTest2.setText("Stopped Scanning...");
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
            //rssi = result.getRssi();
            //meters = getDistance(rssi);
            dName = result.getDevice().getName();
            if (dName != null) {
                dName = (result.getDevice().getName()).trim();
            }
            macAddress = result.getDevice().getAddress();
            if (macAddress != null) {
                macAddress = (result.getDevice().getAddress()).trim();
            }

            /*mTest.setText(String.format(Locale.UK, "RSSI: %d, \nMAC Address: %s, \nDevice Name: %s",
                    result.getRssi(), result.getDevice().getAddress(), result.getDevice().getName()));*/

            if (macAddress != null && macAddress.equals("F4:46:EA:8F:C2:2D")) {
                mTest.setText(String.format(Locale.UK, "Position: %s,\nRSSI: %d, \nMAC Address: %s, \nDevice Name: %s, \nMeters: %f",
                        "End Point Beacon", result.getRssi(), result.getDevice().getAddress(), result.getDevice().getName(), getDistance(result.getRssi(),"GC")));
            } else if (macAddress != null && macAddress.equals("C3:4E:E7:D1:2E:3A")) {
                mTest2.setText(String.format(Locale.UK, "Position: %s,\nRSSI: %d, \nMAC Address: %s, \nDevice Name: %s, \nMeters: %f",
                        "Lifts Area Beacon", result.getRssi(), result.getDevice().getAddress(), result.getDevice().getName(), getDistance(result.getRssi(),"SR")));
            }
         else

            {
             mTest.setText("Can't find End Point Beacon!!!");
             mTest2.setText("Can't find Lifts Area Beacon!!!");
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

    //Connecting to a GATT Server on the BLE device
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        startLeScan(false);
        Toast.makeText(TestActivity.this, "SCAN STOPPED...", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
