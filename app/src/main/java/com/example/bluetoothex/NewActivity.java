package com.example.bluetoothex;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NewActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 10; // 블루투스 활성화 상태
    private BluetoothAdapter bluetoothAdapter; // 블루투스 어댑터
    private Set<BluetoothDevice> devices; // 블루투스 디바이스 데이터 셋
    private BluetoothDevice bluetoothDevice; // 블루투스 디바이스
    private BluetoothSocket bluetoothSocket = null; // 블루투스 소켓
    private OutputStream outputStream = null; // 블루투스에 데이터를 출력하기 위한 출력 스트림
    private InputStream inputStream = null; // 블루투스에 데이터를 입력하기 위한 입력 스트림
    private Thread workerThread = null; // 문자열 수신에 사용되는 쓰레드
    private byte[] readBuffer; // 수신 된 문자열을 저장하기 위한 버퍼
    private int readBufferPosition; // 버퍼 내 문자 저장 위치
    List<String> listPairedDevice;

    private TextView textViewReceive; // 수신 된 데이터를 표시하기 위한 텍스트 뷰
    private EditText editTextSend; // 송신 할 데이터를 작성하기 위한 에딧 텍스트
    private Button buttonSend; // 송신하기 위한 버튼
    private TextView textViewStatus;
    private Button buttonBluetoothOn;
    private Button buttonBluetoothOff;
    private Button buttonConnect;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("8CE255C0-200A-11E0-AC64-0800200C9A66");

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_old);

        textViewReceive = findViewById(R.id.receive);
        editTextSend = findViewById(R.id.message);
        buttonSend = findViewById(R.id.sendMsg);
        textViewStatus = findViewById(R.id.status);
        buttonBluetoothOn = findViewById(R.id.bluetoothOn);
        buttonBluetoothOff = findViewById(R.id.bluetoothOff);
        buttonConnect = findViewById(R.id.connect);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //블투 활성화

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

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData(editTextSend.getText().toString());
            }
        });

        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectBluetoothDevice();
            }
        });
    }

    void sendData(String text){
        text += "\n";
        try{
            outputStream.write(text.getBytes());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

    //블루투스 활성화
    void bluetoothOn(){
        if(bluetoothAdapter == null){
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show();
        }else{
            if (bluetoothAdapter.isEnabled()) {
                Toast.makeText(getApplicationContext(), "블루투스가 이미 활성화 되어 있습니다.", Toast.LENGTH_LONG).show();
                textViewStatus.setText("활성화");
            }
            else {
                Toast.makeText(getApplicationContext(), "블루투스가 활성화 되어 있지 않습니다.", Toast.LENGTH_LONG).show();
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE);
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

    public void selectBluetoothDevice(){
        devices = bluetoothAdapter.getBondedDevices();
        int pairedDevice = devices.size();
        if(pairedDevice == 0){
            //페어링 하기위한 함수 호출 << 이게머야
        }else {
            //이미 페어링된 디바이스가 있을경우엔
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("장치 선택");

            listPairedDevice = new ArrayList();
            for (BluetoothDevice device : devices) {
                listPairedDevice.add(device.getName());
                //mListPairedDevices.add(device.getName() + "\n" + device.getAddress());
            }
            listPairedDevice.add("취소");

            final CharSequence[] charSequences = listPairedDevice.toArray(new CharSequence[listPairedDevice.size()]);
            listPairedDevice.toArray(new CharSequence[listPairedDevice.size()]);

            builder.setItems(charSequences, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    connectDevice(charSequences[which].toString());
                }
            });

            builder.setCancelable(false);
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    public void connectDevice(String deviceName){
        for(BluetoothDevice tempDevice:devices){
            if(deviceName.equals(tempDevice.getName())){
                bluetoothDevice = tempDevice;
                break;
            }
        }
        try{
            bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(BT_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            receiveData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveData(){
        final Handler handler = new Handler();
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                {
                    while (Thread.currentThread().isInterrupted()){
                        try{
                            int byteAvailable = inputStream.available();
                            if(byteAvailable>0){
                                byte[] bytes = new byte[byteAvailable];
                                inputStream.read(bytes);
                                for(int i=0; i<byteAvailable; i++){
                                    byte tempByte = bytes[i];

                                    if(tempByte == '\n'){
                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                        final String text = new String(encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                textViewReceive.append(text+"\n");
                                            }
                                        });
                                    } else {
                                        readBuffer[readBufferPosition++] = tempByte;
                                    }
                                }
                            }
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                        try{
                            Thread.sleep(1000);
                        }catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        workerThread.start();
    }

}