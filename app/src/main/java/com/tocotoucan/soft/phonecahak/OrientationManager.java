package com.tocotoucan.soft.phonecahak;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.OrientationListener;

/**
 * Created by kangilkueon on 15. 11. 5.
 */
public class OrientationManager implements SensorEventListener{
    private static Sensor sensor;
    private static SensorManager sensorManager;

    /* 센서 데이터를 저장할 변수 */
    private float[] sensorData;

    private Context context;
    public OrientationManager(Context _context) {
        context = _context;
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
    }

    public Sensor getSensor () {
        return sensor;
    }
    public SensorManager getSensorManager(){
        return sensorManager;
    }

    public float[] getSensorData () {
        return sensorData;
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        sensorData = event.values.clone();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
