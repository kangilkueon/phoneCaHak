package com.tocotoucan.soft.phonecahak;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
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
public class CameraActivity extends AppCompatActivity {
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

    Button structure_cancel_button;

    Camera mCamera;
    Camera.PictureCallback mPicture;

    int camera_type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

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

        structure_cancel_button = (Button) findViewById(R.id.structureCancelButton);
        structure_cancel_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                structure_layout.setVisibility(View.INVISIBLE);
            }
        });

        structure_button1 = (Button) findViewById(R.id.structureButton1);
        structure_button1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                structure_layout.setVisibility(View.INVISIBLE);
            }
        });

        structure_button2 = (Button) findViewById(R.id.structureButton2);
        structure_button2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                structure_layout.setVisibility(View.INVISIBLE);
            }
        });

        structure_button3 = (Button) findViewById(R.id.structureButton3);
        structure_button3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                structure_layout.setVisibility(View.INVISIBLE);
            }
        });

        structure_button4 = (Button) findViewById(R.id.structureButton4);
        structure_button4.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                structure_layout.setVisibility(View.INVISIBLE);
            }
        });

        structure_button5 = (Button) findViewById(R.id.structureButton5);
        structure_button5.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
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
            camera_id = getBackCamera();
            if (camera_id >= 0) {
                mCamera = Camera.open(camera_id);
                camera_type = BACK_CAMERA;
            }
        } else if(camera_type == BACK_CAMERA) {
            camera_id = getFrontCamera();
            if (camera_id >= 0) {
                mCamera = Camera.open(camera_id);
                camera_type = FRONT_CAMERA;
            }
        }
        refreshCamera();
    }

    private int getFrontCamera(){
        int camera_id = -1;
        int camera_num = Camera.getNumberOfCameras();
        for (int i = 0; i < camera_num; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                camera_id = i;
                break;
            }
        }

        return camera_id;
    }

    private int getBackCamera(){
        int camera_id = -1;
        int camera_num = Camera.getNumberOfCameras();
        for (int i = 0; i < camera_num; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK){
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
}