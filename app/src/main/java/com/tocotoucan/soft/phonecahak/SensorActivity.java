package com.tocotoucan.soft.phonecahak;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Created by kangilkueon on 15. 10. 30.
 */
public class SensorActivity extends Activity implements SensorEventListener {
    int gyro_x;
    int gyro_y;
    int gyro_z;

    private SensorManager mSensorManager;
    private Sensor gyroSensor;
    @Override
    protected void onCreate(Bundle savedInsanceState) {
        super.onCreate(savedInsanceState);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyro_x = Math.round(event.values[0] * 1000);
            gyro_y = Math.round(event.values[1] * 1000);
            gyro_z = Math.round(event.values[2] * 1000);

            Toast.makeText(this, "X::" + gyro_x + "  Y::" + gyro_y + "  Z:: " + gyro_z, Toast.LENGTH_LONG);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
