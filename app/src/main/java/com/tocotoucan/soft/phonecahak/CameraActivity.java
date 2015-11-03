package com.tocotoucan.soft.phonecahak;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Created by kangilkueon on 15. 10. 28.
 */
public class CameraActivity extends AppCompatActivity implements SensorEventListener {
    static final int FRONT_CAMERA = 0;
    static final int BACK_CAMERA = 1;

    /* Layout component */
    SurfaceView camera_preview;
    SurfaceHolder holder;
    Button rotate_camera_button;
    Button camera_button;
    Button select_structure_button;
    LinearLayout structure_layout;

    Button structure_button1;
    Button structure_button2;
    Button structure_button3;
    Button structure_button4;
    Button structure_button5;

    ImageView structure_image;

    Button structure_cancel_button;

    Camera mCamera;
    Camera.PictureCallback mPicture;

    int camera_type;

    /* gyroscope Sensor */
    int gyro_x;
    int gyro_y;
    int gyro_z;

    private SensorManager mSensorManager;
    private Sensor gyroSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        mPicture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                File pictureFile = getOutputMediaFile();
                if (pictureFile == null) {
                    return;
                }
                try {
                    //write the file
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                    Toast toast = Toast.makeText(CameraActivity.this, "Picture saved: " + pictureFile.getName(), Toast.LENGTH_LONG);
                    toast.show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                refreshCamera();
            }
        };

        rotate_camera_button = (Button) findViewById(R.id.rotateCameraButton);
        rotate_camera_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCamera();
            }
        });

        camera_button = (Button) findViewById(R.id.cameraButton);
        camera_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mCamera.takePicture(null, null, mPicture);
            }
        });

        select_structure_button = (Button) findViewById(R.id.selectStructureButton);
        select_structure_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                structure_layout.setVisibility(View.VISIBLE);
            }
        });

        structure_image = (ImageView) findViewById(R.id.structureImageView);
        structure_button1 = (Button) findViewById(R.id.structureButton1);
        structure_button1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                structure_image.setBackgroundResource(R.drawable.vertical_structure_1);
                structure_layout.setVisibility(View.INVISIBLE);
            }
        });

        structure_button2 = (Button) findViewById(R.id.structureButton2);
        structure_button2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                structure_image.setBackgroundResource(R.drawable.vertical_structure_2);
                structure_layout.setVisibility(View.INVISIBLE);
            }
        });

        structure_button3 = (Button) findViewById(R.id.structureButton3);
        structure_button3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                structure_image.setBackgroundResource(R.drawable.vertical_structure_3);
                structure_layout.setVisibility(View.INVISIBLE);
            }
        });

        structure_layout = (LinearLayout) findViewById(R.id.structureLayout);

        camera_preview = (SurfaceView) findViewById(R.id.cameraSurface);

        holder = camera_preview.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(surfaceListener);

        camera_type = BACK_CAMERA;
    }

    private SurfaceHolder.Callback surfaceListener = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder){
            mCamera = Camera.open();

            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.setDisplayOrientation(90);
            } catch (Exception e){
                e.printStackTrace();
                mCamera.release();
                mCamera = null;
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,	int height) {
            Camera.Parameters parameters = mCamera.getParameters();
            //parameters.setPreviewSize(width, height);
            //mCamera.setParameters(parameters);
            mCamera.startPreview();
        }
    };

    private void selectCamera() {
        int camera_id = -1;
        mCamera.release();
        if(camera_type == FRONT_CAMERA){
            camera_id = getCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
            if (camera_id >= 0) {
                mCamera = Camera.open(camera_id);
                camera_type = BACK_CAMERA;
            }
        } else if(camera_type == BACK_CAMERA) {
            camera_id = getCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
            if (camera_id >= 0) {
                mCamera = Camera.open(camera_id);
                camera_type = FRONT_CAMERA;
            }
        }
        refreshCamera();
    }

    private int getCamera(int type){
        int camera_id = -1;
        int camera_num = Camera.getNumberOfCameras();
        for (int i = 0; i < camera_num; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == type){
                camera_id = i;
                break;
            }
        }

        return camera_id;
    }

    private static File getOutputMediaFile() {
        //make a new file directory inside the "sdcard" folder
        File mediaStorageDir = new File("/sdcard/", "phoneCaHak");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        return mediaFile;
    }

    private void refreshCamera(){
        mCamera.stopPreview();
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(90);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    @Override

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        System.out.println("####################FUCK###################");


        switch(newConfig.orientation) {

            case Configuration.ORIENTATION_PORTRAIT:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 세로전환
                setContentView(R.layout.activity_camera);
            break;

            case Configuration.ORIENTATION_LANDSCAPE:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); // 가로전환
                setContentView(R.layout.activity_camera);
            break;

        }

    }






    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyro_x = Math.round(event.values[0] * 1000);
            gyro_y = Math.round(event.values[1] * 1000);
            gyro_z = Math.round(event.values[2] * 1000);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}