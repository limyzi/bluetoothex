package com.example.bluetoothex;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    // Get permission
    String[] permission_list = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    TextView textStatus;
    Button btnParied, btnSearch, btnSend;
    ListView listView;

    BluetoothAdapter btAdapter;
    Set<BluetoothDevice> pairedDevices;
    ArrayAdapter<String> btArrayAdapter;
    ArrayList<String> deviceAddressArray;

    BluetoothSocket btSocket;
    ConnectedThread connectedThread;

    private final static int REQUEST_ENABLE_BT = 1;
    final static UUID BT_UUID = UUID.fromString("8CE255C0-200A-11E0-AC64-0800200C9A66");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //위치 권한
        ActivityCompat.requestPermissions(MainActivity.this, permission_list,  1);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //블루투스 활성화하기
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!btAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        textStatus = (TextView) findViewById(R.id.text_status);
        btnParied = (Button) findViewById(R.id.btn_paired);
        btnSearch = (Button) findViewById(R.id.btn_search);
        btnSend = (Button) findViewById(R.id.btn_send);
        listView = (ListView) findViewById(R.id.listview);

        btArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        deviceAddressArray = new ArrayList<>();
        listView.setAdapter(btArrayAdapter);

        listView.setOnItemClickListener(new myOnItemClickListener());


    }

    //기존에 페어링된 장치목록
    public void onClickButtonPaired(View view){
        btArrayAdapter.clear();
        if(deviceAddressArray!=null && !deviceAddressArray.isEmpty()){
            deviceAddressArray.clear();
        }
        pairedDevices = btAdapter.getBondedDevices();
        if(pairedDevices.size()>0){
            //페어링된 장치 이름이랑 주소 가져오기
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName(); // 디바이스 이름
                String deviceHardwareAddress = device.getAddress();
                btArrayAdapter.add(deviceName);
                deviceAddressArray.add(deviceHardwareAddress);
            }
        }
    }

    //블루투스로 연결할 주변 장치 검색하기
    public void onClickButtonSearch(View view){
        //장치가 이미 발견되었는지.

        if(btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        } else {
            if(btAdapter.isEnabled()){
                btArrayAdapter.clear();
                if(deviceAddressArray!=null && !deviceAddressArray.isEmpty()){
                    deviceAddressArray.clear();
                }
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                // receiver 검색된기기 정보가져오기
                registerReceiver(receiver, filter);
            } else{
                Toast.makeText(getApplicationContext(), "Blutooth Off", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                btArrayAdapter.add(deviceName);
                deviceAddressArray.add(deviceHardwareAddress);
                btArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

    public class myOnItemClickListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Toast.makeText(getApplicationContext(), btArrayAdapter.getItem(position),Toast.LENGTH_SHORT).show();
            textStatus.setText("try");

            final String name = btArrayAdapter.getItem(position);
            final String address = deviceAddressArray.get(position);
            boolean flag = true;

            BluetoothDevice device = btAdapter.getRemoteDevice(address);

            try{
                btSocket = device.createInsecureRfcommSocketToServiceRecord(BT_UUID);
                if(!btSocket.isConnected()){
                    btSocket.connect();
                }
            }catch (IOException e){
                flag = false;
                textStatus.setText("connection failed!");
                e.printStackTrace();
            }
            if(flag){
                textStatus.setText("connected to "+name);
                connectedThread = new ConnectedThread(btSocket);
                connectedThread.start();
            }

        }

    }
    public void onClickButtonSend(View view){
        if(connectedThread!=null){ connectedThread.write("a"); }
    }

}

