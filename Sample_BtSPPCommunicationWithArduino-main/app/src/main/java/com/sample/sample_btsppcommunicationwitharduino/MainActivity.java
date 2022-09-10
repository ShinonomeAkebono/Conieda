package com.sample.sample_btsppcommunicationwitharduino;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";
    private static final String CONNECT_DEVICE_NONE = "NONE";
    private static final String CONNECT_STATUS_CONNECT = "接続";
    private static final String CONNECT_DISCONNECT_CONNECT = "未接続";

    private BluetoothAdapter mBluetoothAdapter;
    private ActivityResultLauncher<Intent> mActivityResultLauncher;

    private Button mAddDeviceButton;
    private Spinner mConnectDeviceSpinner;
    private ArrayAdapter<String> mConnectDeviceAdapter;

    private TextView mConnectStatusTextView;
    private Button mConnectButton;
    BluetoothSocket mBluetoothSocket;
    private TextView txtSeek;
    private EditText mSendDataEditText;
    private Button mSendButton;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private TextView mReceiveDataTextView;
    private Thread mReceiveThread;
    private ReceiveRunnable mReceiveRunnable;
    private Induction induction = new Induction();
    private SeekBar azimuthSeek;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtSeek = findViewById(R.id.receive_data_title_text_view);
        azimuthSeek = findViewById(R.id.azimuthSeek);
        getAzimuth();
        checkDeviceSupportBluetooth();
        initBluetoothAdapter();
        initAddDeviceButton();
        initConnectButton();
        initBluetoothIO();

    }

    private void checkDeviceSupportBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Device does not support Bluetooth.");
            finish();
        }
    }

    private void initBluetoothAdapter() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {

                    int resultCode = result.getResultCode();

                    switch (resultCode) {
                        case RESULT_OK:
                            break;
                        case RESULT_CANCELED:
                            showBluetoothDisabledAlertDialogAndFinishApp();
                            break;
                        case RESULT_FIRST_USER:
                        default:
                            break;
                    }
                });

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivityResultLauncher.launch(enableBtIntent);
        }
    }

    private void showBluetoothDisabledAlertDialogAndFinishApp() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("BluetoothがOFFになっています。")
                .setMessage("BluetoothをONにしてアプリを再起動してください。")
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
    }

    private void initAddDeviceButton() {
        mAddDeviceButton = findViewById(R.id.add_device_button);
        mAddDeviceButton.setOnClickListener(view -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        });
    }

    private void initConnectButton() {
        mConnectStatusTextView = findViewById(R.id.connect_status_text_view);
        mBluetoothSocket = null;
        mOutputStream = null;
        mInputStream = null;
        mConnectButton = findViewById(R.id.connect_button);
        mConnectButton.setOnClickListener(view -> {
            int selectedItemPosition = mConnectDeviceSpinner.getSelectedItemPosition();
            String deviceName = mConnectDeviceAdapter.getItem(selectedItemPosition);

            if(deviceName.equals(CONNECT_DEVICE_NONE)) {
                Log.e(TAG,"device is NONE");
            } else {
                connectDevice(deviceName);
            }
        });
    }

    private void connectDevice(String connectDeviceName) {
        BluetoothDevice connectBluetoothDevice = null;
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices != null) {
            for (BluetoothDevice bluetoothDevice : pairedDevices) {
                if (connectDeviceName.equals(bluetoothDevice.getName())) {
                    connectBluetoothDevice = bluetoothDevice;
                    break;
                }
            }
        }

        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        try {
            mBluetoothSocket = connectBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException createIOException) {
            createIOException.printStackTrace();
        }
        if (mBluetoothSocket != null) {
            try {
                mBluetoothSocket.connect();
                mConnectStatusTextView.setText(CONNECT_STATUS_CONNECT);
                mOutputStream = mBluetoothSocket.getOutputStream();
                mInputStream = mBluetoothSocket.getInputStream();
                //startReceiveTask();
            } catch (IOException connectIOException) {
                connectIOException.printStackTrace();
                try {
                    mBluetoothSocket.close();
                } catch (IOException closeIOException) {
                    closeIOException.printStackTrace();
                }
                mBluetoothSocket = null;
                Log.e(TAG, "Bluetooth connect failed.");
            }
        }
    }

    private void initBluetoothIO() {
        mSendDataEditText = findViewById(R.id.send_data_edit_text);
        mSendButton = findViewById(R.id.send_button);
        mSendButton.setOnClickListener(view -> sendData());
        mReceiveDataTextView = findViewById(R.id.receive_data_text_view);
        mReceiveThread = null;
    }

    private void sendData() {
        String sendString = mSendDataEditText.getText().toString();
        if(mOutputStream != null) {
            byte[] sendBytes = sendString.getBytes();
            try {
                mOutputStream.write(sendBytes);
            } catch (IOException writeIOException) {
                writeIOException.printStackTrace();
            }
        }

    }
    private void sendDataInduction(int right,int left) {
        int mright = 90 - right;
        int mleft = 90 + left;
        String sendString = String.format("%d,%d;",mright,mleft);
        if(mOutputStream != null) {
            byte[] sendBytes = sendString.getBytes();
            try {
                mOutputStream.write(sendBytes);
            } catch (IOException writeIOException) {
                writeIOException.printStackTrace();
            }
        }

    }

    private void startReceiveTask() {
        if(mReceiveRunnable != null) {
            mReceiveRunnable.shutdown();
        }
        mReceiveRunnable = new ReceiveRunnable();
        mReceiveThread = new Thread(mReceiveRunnable);
        mReceiveThread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initConnectDeviceSpinner();
    }

    public void getAzimuth(){
        azimuthSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                i = i-180;
                induction.Goal_azimuth = (double) i;
                txtSeek.setText(String.format("%d",(int)induction.Goal_azimuth));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Thread t = new Thread(new backInduction());
                t.start();
            }
        });
    }

    private void initConnectDeviceSpinner() {
        mConnectDeviceSpinner = findViewById(R.id.connect_device_spinner);
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if(pairedDevices != null) {
            mConnectDeviceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);

            if(pairedDevices.isEmpty()) {
                mConnectDeviceAdapter.add(CONNECT_DEVICE_NONE);
            } else {
                for (BluetoothDevice bluetoothDevice : pairedDevices) {
                    String deviceName = bluetoothDevice.getName();
                    mConnectDeviceAdapter.add(deviceName);
                }
            }
        }
        mConnectDeviceSpinner.setAdapter(mConnectDeviceAdapter);
    }

    @Override
    protected void onPause() {
        disconnectBluetooth();
        super.onPause();
        induction.onPause();
    }

    private void disconnectBluetooth() {
        if(mReceiveRunnable != null) {
            mReceiveRunnable.shutdown();
            mReceiveRunnable = null;
        }
        if(mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException closeIOException) {
                closeIOException.printStackTrace();
            }
            mOutputStream = null;
        }
        if(mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException closeIOException) {
                closeIOException.printStackTrace();
            }
            mInputStream = null;
        }
        if(mBluetoothSocket != null) {
            try {
                mBluetoothSocket.close();
            } catch (IOException closeIOException) {
                closeIOException.printStackTrace();
            }
            mBluetoothSocket = null;
        }
        mConnectStatusTextView.setText(CONNECT_DISCONNECT_CONNECT);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    class ReceiveRunnable implements Runnable {
        private boolean mIsKeepRunning;

        @Override
        public synchronized void run() {
            mIsKeepRunning = true;
            while(mIsKeepRunning) {
                receiveData();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException threadSleepInterruptedException) {
                    threadSleepInterruptedException.printStackTrace();
                }
            }
        }

        public void shutdown() {
            mIsKeepRunning = false;
        }

        private void receiveData() {
            if(mInputStream != null) {
                byte[] receiveData = new byte[256];
                int size;
                try {
                    size = mInputStream.read(receiveData);
                    if(size > 0) {
                        receiveData[size] = '\0';
                        String receiveString = new String(receiveData, java.nio.charset.StandardCharsets.UTF_8);
                        mReceiveDataTextView.setText(receiveString);
                    }
                } catch (IOException readIOException) {
                    readIOException.printStackTrace();
                }
            }
        }
    }

    class backInduction implements Runnable {
        @Override
        public void run() {
            induction.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            induction.onResume();
            while(true){
                sendDataInduction(induction.right,induction.left);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}