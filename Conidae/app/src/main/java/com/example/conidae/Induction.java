package com.example.conidae;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

@SuppressLint("NotConstructor")
public class Induction implements SensorEventListener {
    public SensorManager sensorManager;//これはmain関数内でインスタンス名.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE)として初期化。
    public int right;
    public int left;
    public double Goal_azimuth = 0;
    public double now_azimuth = 0;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] rotationFinal = new float[9];
    public final float[] orientationAngles = new float[3];
    public Induction(){
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
    public void update(SensorEvent sensorEvent){
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
    public void updateOrientationAngles(){//センサ値から値を算出する関数
        //getRotationMatrixで回転行列を取得
        SensorManager.getRotationMatrix(rotationMatrix,null,accelerometerReading,magnetometerReading);
        //getOrientationで取得した回転行列から姿勢角を算出。
        SensorManager.remapCoordinateSystem(rotationMatrix,SensorManager.AXIS_Z,SensorManager.AXIS_MINUS_X,rotationFinal);
        SensorManager.getOrientation(rotationFinal,orientationAngles);
        cal_induction();
    }

    public void cal_induction() {//駆動の処理をするため角度で分岐.出力をする関数であるsendData()との依存関係がある。
        now_azimuth = (double) orientationAngles[0]*180/Math.PI;
        double deltaarg = Goal_azimuth - now_azimuth;
        double phi;
        if ((-360 < deltaarg) && (deltaarg < -180)) {//右
            phi = 360 + deltaarg;
            System.out.println(phi);
            right = (int) -phi / 2;
            left =(int) phi / 2;

        } else if ((0 < deltaarg) && (deltaarg < 180)) {//右
            phi = deltaarg;
            System.out.println(phi);
            right = (int) -phi / 2;
            left =(int) phi / 2;

        } else if ((-180 < deltaarg) && (deltaarg < 0)) {//左
            phi = -deltaarg;
            System.out.println(phi);
            right = (int) phi / 2;
            left =(int) -phi / 2;

        } else if ((180 < deltaarg) && (deltaarg < 360)) {//左
            phi = deltaarg - 180;
            System.out.println(phi);
            right = (int) phi / 2;
            left =(int) -phi / 2;
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void onResume(){
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
    public void onPause(){
        sensorManager.unregisterListener(this);
    }

}

