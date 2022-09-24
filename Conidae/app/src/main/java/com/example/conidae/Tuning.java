package com.example.conidae;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class Tuning extends AppCompatActivity
        implements SensorEventListener {
    private SensorManager sensorManager;
    private TextView txtX,txtY,txtZ,txtRight,txtLeft,txtSeek;
    private SeekBar azimuthSeek;
    private double Goal_azimuth = 0;
    private double now_azimuth = 0;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] rotationFinal = new float[9];
    private final float[] orientationAngles = new float[3];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tuning);

        //センサーマネージャのインスタンスを立ち上げる
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
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
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(accelerometer != null){
            sensorManager.registerListener(this,accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if(magneticField != null){
            sensorManager.registerListener(this,magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL,SensorManager.SENSOR_DELAY_UI);
        }

    }
    @Override
    protected void onPause() {//センサがバックグランドになっても動き続けることを阻止するため、pauseの際に停止させる。
        super.onPause();
        sensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            //arraycopyはセンサの値を代入する方法。引数には、(コピー元,コピー元開始位置,コピー先,コピー先開始,コピーする要素数)の形。
            System.arraycopy(sensorEvent.values,0,accelerometerReading,
                    0,accelerometerReading.length);
        }
        else if(sensorEvent.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
            System.arraycopy(sensorEvent.values,0,magnetometerReading,
                    0,magnetometerReading.length);
        }
        updateOrientationAngles();
    }
    public void getAzimuth(){
        azimuthSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                i = i-180;
                Goal_azimuth = (double) i;
                txtSeek.setText(String.format("%d",(int)Goal_azimuth));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void updateOrientationAngles(){//センサ値から値を算出する関数
        //getRotationMatrixで回転行列を取得
        SensorManager.getRotationMatrix(rotationMatrix,null,accelerometerReading,magnetometerReading);
        //getOrientationで取得した回転行列から姿勢角を算出。
        SensorManager.remapCoordinateSystem(rotationMatrix,SensorManager.AXIS_Z,SensorManager.AXIS_MINUS_X,rotationFinal);
        SensorManager.getOrientation(rotationFinal,orientationAngles);
        //テキストビューに値を代入する。他で使う場合は削除
        txtX.setText(String.format("%f",orientationAngles[0]*180/Math.PI));
        txtY.setText(String.format("%f",orientationAngles[1]*180/Math.PI));
        txtZ.setText(String.format("%f",orientationAngles[2]*180/Math.PI));
        cal_induction();
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    public void sendData(int right,int left){
        txtRight.setText(String.format("%d",right));
        txtLeft.setText(String.format("%d",left));
    }
    public void cal_induction() {//駆動の処理をするため角度で分岐.出力をする関数であるsendData()との依存関係がある。
        now_azimuth = (double) orientationAngles[0]*180/Math.PI;
        double deltaarg = Goal_azimuth - now_azimuth;
        double phi;
                if ((-360 < deltaarg) && (deltaarg < -180)) {//右
                    phi = 360 + deltaarg;
                    System.out.println(phi);
                    sendData((int) -phi / 2, (int) phi / 2);

                } else if ((0 < deltaarg) && (deltaarg < 180)) {//右
                    phi = deltaarg;
                    System.out.println(phi);
                    sendData((int) -phi / 2, (int) phi / 2);

                } else if ((-180 < deltaarg) && (deltaarg < 0)) {//左
                    phi = -deltaarg;
                    System.out.println(phi);
                    sendData((int) phi / 2, (int) -phi / 2);

                } else if ((180 < deltaarg) && (deltaarg < 360)) {//左
                    phi = deltaarg - 180;
                    System.out.println(phi);
                    sendData((int) phi / 2, (int) -phi / 2);
                }
    }
}
