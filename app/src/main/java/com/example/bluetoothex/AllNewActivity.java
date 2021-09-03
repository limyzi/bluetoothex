package com.example.bluetoothex;

import android.annotation.TargetApi;
import android.app.LauncherActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@TargetApi(21)
public class AllNewActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler handler;
    private static final long SCAN_PERIOD = 500;
    private BluetoothLeScanner leScanner;
    private ScanSettings settings;
    private List<ScanFilter> filterList;
    private BluetoothGatt gatt;

//    private List<BluetoothDevice> devices = new ArrayList<>(); // 블루투스 디바이스 데이터 셋
//    private BluetoothDevice bluetoothDevice; // 블루투스 디바이스
    List<Map<String, Object>> listPairedDevice = new ArrayList<>();

//    private TextView textViewReceive; // 수신 된 데이터를 표시하기 위한 텍스트 뷰
//    private EditText editTextSend; // 송신 할 데이터를 작성하기 위한 에딧 텍스트
//    private Button buttonSend; // 송신하기 위한 버튼
    private TextView textViewStatus;
    private Button buttonBluetoothOn;
    private Button buttonBluetoothOff;
    private Button buttonConnect;
    private ListView connectableList;
    private TextView connectStatus;
    private ArrayAdapter adapter;
    private Button disconnect;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allnew);

//        textViewReceive = findViewById(R.id.receive);
//        editTextSend = findViewById(R.id.message);
//        buttonSend = findViewById(R.id.sendMsg);
        textViewStatus = findViewById(R.id.status);
        buttonBluetoothOn = findViewById(R.id.bluetoothOn);
        buttonBluetoothOff = findViewById(R.id.bluetoothOff);
        buttonConnect = findViewById(R.id.connect);
        connectableList = findViewById(R.id.connectableList);
        connectStatus = findViewById(R.id.connectStatus);
        disconnect = findViewById(R.id.disconnect);

        handler = new Handler();
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        buttonBluetoothOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothOn();
            }
        });

        buttonBluetoothOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothOff();
            }
        });

        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectBluetoothDevice();
            }
        });

        connectableList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               // connectBluetoothDevice(adapter.getItem(position).toString());
                Map<String, Object> info = (Map<String, Object>) adapter.getItem(position);
                connectToDevice((BluetoothDevice) info.get("device"));
            }
        });

        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

    }

    //블투활성화화
    void bluetoothOn(){
        if(bluetoothAdapter == null){
            //ble기기인지 아닌짖.
            if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
                Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
                finish();
            }else{
                Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show();
            }
        }else{
            if (bluetoothAdapter.isEnabled()) {
                Toast.makeText(getApplicationContext(), "블루투스가 이미 활성화 되어 있습니다.", Toast.LENGTH_LONG).show();
                textViewStatus.setText("활성화");
            }
            else {
                Toast.makeText(getApplicationContext(), "블루투스를 활성화 합니다.", Toast.LENGTH_LONG).show();
                //블루투스 활성화하기
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentBluetoothEnable, REQUEST_ENABLE_BT);
            }
        }
    }

    //블루투스 비활성화
    void bluetoothOff() {
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되었습니다.", Toast.LENGTH_SHORT).show();
            textViewStatus.setText("비활성화");
        }
        else {
            Toast.makeText(getApplicationContext(), "블루투스가 이미 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    //디바이스 검색
    public void selectBluetoothDevice(){
        if(adapter!=null) adapter.clear();
        if(Build.VERSION.SDK_INT >= 21) {
            leScanner = bluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            filterList = new ArrayList<ScanFilter>();
        }
        scanLeDevice(true);
    }

    //디바이스 선택연결
//    public void connectBluetoothDevice(String name){
//        for (BluetoothDevice tempDevice : devices) {
//            if (name.equals(tempDevice.getName()) || name.equals(tempDevice.getAddress())) {
//                connectToDevice(tempDevice);
//                break;
//            }
//        }
//    }

    //스캔
    private void scanLeDevice(final boolean enable){
        if(enable){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(Build.VERSION.SDK_INT < 21){
                        bluetoothAdapter.stopLeScan(leScanCallback);
                    }else{
                        leScanner.stopScan(scanCallback);
                    }
                }
            }, SCAN_PERIOD);
            if(Build.VERSION.SDK_INT < 21){
                bluetoothAdapter.startLeScan(leScanCallback);
            } else {
                leScanner.startScan(filterList, settings, scanCallback);
            }

        } else {
            if(Build.VERSION.SDK_INT < 21){
                bluetoothAdapter.stopLeScan(leScanCallback);
            } else {
                leScanner.stopScan(scanCallback);
            }
        }
    }

    //스캔콜백
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            //le아닐때 연결하는건갑네
            BluetoothDevice btDevice = result.getDevice();

            Map<String, Object> deviceinfo = new HashMap<>();
            deviceinfo.put("name",btDevice.getName());
            deviceinfo.put("address", btDevice.getAddress());
            deviceinfo.put("device", btDevice);
            listPairedDevice.add(deviceinfo);
            adapter = new ArrayAdapter(AllNewActivity.this, android.R.layout.simple_list_item_1, listPairedDevice);
            connectableList.setAdapter(adapter);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for(ScanResult sr : results){
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    //le스캔콜백
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i("onLeScan",device.toString());
                    //devices.add(device);
                }
            });
        }
    };

    //연결
    public void connectToDevice(BluetoothDevice device){
        gatt = device.connectGatt(this, false, gattCallback);
        connectStatus.setText("connected");
    }

    //연결해제
    public void disconnect(){
        gatt.disconnect();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                gatt.close();
            }
        },1000);
        connectStatus.setText("disconnected");
    }

    //gatt콜백
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.i("onCharacteristicRead", characteristic.getUuid().toString());
        }
    };
}
