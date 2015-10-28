package com.tocotoucan.soft.phonecahak;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

/**
 * Created by kangilkueon on 15. 10. 28.
 */
public class CameraActivity extends AppCompatActivity {
    static final int FRONT_CAMERA = 0;
    static final int BACK_CAMERA = 1;
    SurfaceView camera_preview;
    SurfaceHolder holder;
    Button rotate_camera_button;
    Camera mCamera;

    int camera_type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        rotate_camera_button = (Button) findViewById(R.id.rotateCameraButton);
        rotate_camera_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCamera();
            }
        });

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
        mCamera.stopPreview();
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
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(90);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
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
}