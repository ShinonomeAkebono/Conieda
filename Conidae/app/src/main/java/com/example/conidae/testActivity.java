package com.example.conidae;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class testActivity extends AppCompatActivity
 {
    private TextView txtX,txtY,txtZ,txtRight,txtLeft,txtSeek;
    private SeekBar azimuthSeek;
    private Induction induction = new Induction();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tuning);

        //センサーマネージャのインスタンスを立ち上げる
        induction.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        txtX = findViewById(R.id.txtX);
        txtY = findViewById(R.id.txtY);
        txtZ = findViewById(R.id.txtZ);
        Button btnReturn = findViewById(R.id.btnReturn);
        btnReturn.setOnClickListener(view -> finish());

        //計算結果出力用のviewたち
        txtRight = findViewById(R.id.txtRight);
        txtLeft = findViewById(R.id.txtLeft);
        txtSeek = findViewById(R.id.txtSeek);
        azimuthSeek = findViewById(R.id.azimuthSeek);
        getAzimuth();

    }
    @Override
    protected  void  onResume(){
        //再開時にはセンサを登録しなおす。
        super.onResume();
        induction.onResume();
    }
    @Override
    protected void onPause() {//センサがバックグランドになっても動き続けることを阻止するため、pauseの際に停止させる。
        super.onPause();
        induction.onPause();
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

            }
        });
    }

    public void sendData(int right,int left){
        txtRight.setText(String.format("%d",right));
        txtLeft.setText(String.format("%d",left));
    }


 }

