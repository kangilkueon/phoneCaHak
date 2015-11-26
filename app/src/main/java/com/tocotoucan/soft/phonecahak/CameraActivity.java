package com.tocotoucan.soft.phonecahak;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
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
public class CameraActivity extends AppCompatActivity implements StructureSelectFragment.OnFragmentInteractionListener, SensorEventListener {
    static final int FRONT_CAMERA = 0;
    static final int BACK_CAMERA = 1;

    /* Layout component */
    SurfaceView camera_preview;
    SurfaceHolder holder;
    LinearLayout structure_layout;

    ImageView structure_image;
    ImageView angle_status;

    Camera mCamera;
    Camera.PictureCallback mPicture;

    static int camera_type = BACK_CAMERA;

    /* Orientation Sensor */
    private int pitch;
    private int roll;
    private int azimuth;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    /* 화면 모드가 가로인지 세로인지 확인하는 변수 */
    private boolean isLandscape = false;

    /* 구도 선택 화면 on/off 확인하는 변수 */
    private boolean isFragmentOn = false;

    /* 구도 선택 fragment */
    private Fragment structureFragment;
    private BackButtonHandler backButtonHandler;

    Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

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
                galleryAddPic(pictureFile.getPath());
                refreshCamera();
                getThumbnail();
            }
        };

        buttonInit();

        structure_image = (ImageView) findViewById(R.id.structureImageView);

        structure_layout = (LinearLayout) findViewById(R.id.structureLayout);

        camera_preview = (SurfaceView) findViewById(R.id.cameraSurface);

        holder = camera_preview.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(surfaceListener);


        angle_status = (ImageView) findViewById(R.id.angleStatus);

        /* 화면 방향에 따른 레이아웃 설정 */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);


        backButtonHandler = new BackButtonHandler(this);
        structureFragment = new StructureSelectFragment();
        closeFragment();

        structure_image.setVisibility(View.INVISIBLE);

        getThumbnail();
    }

    /* Fragment 생성 및 변경 */
    private void replaceFragment(){
        isFragmentOn = true;

        Bundle argument = new Bundle();
        argument.putBoolean("isLandscape", isLandscape);
        argument.putInt("camera_type", camera_type);
        structureFragment.setArguments(argument);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.structureLayout, structureFragment);
        transaction.commit();
    }

    /* Fragment 닫기 */
    private void closeFragment () {
        isFragmentOn = false;

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.remove(structureFragment).commit();
    }

    @Override
    public void onSelectStructureFrame(int structure_num){
        structure_image.setVisibility(View.VISIBLE);
        switch (structure_num){
            case 0 :
                structure_image.setVisibility(View.INVISIBLE);
                break;
            case 1 :
				if (camera_type == BACK_CAMERA) {
					if (isLandscape) {
						structure_image.setBackgroundResource(R.drawable.horizontal_structure_1);
					} else {
						structure_image.setBackgroundResource(R.drawable.vertical_structure_1);
					}
				} else {
					if (isLandscape) {
						structure_image.setBackgroundResource(R.drawable.selfie_structure_1);
					} else {
						structure_image.setBackgroundResource(R.drawable.selfie_structure_2);
					}
				}
                break;
            case 2 :
                if (isLandscape) {
                    structure_image.setBackgroundResource(R.drawable.horizontal_structure_2);
                } else {
                    structure_image.setBackgroundResource(R.drawable.vertical_structure_2);
                }
                break;
            case 3 :
                if (isLandscape) {
                    structure_image.setBackgroundResource(R.drawable.horizontal_structure_3);
                } else {
                }
                break;
            case 4 :
                if (isLandscape) {
                    structure_image.setBackgroundResource(R.drawable.horizontal_structure_4);
                } else {
                }

                break;
        }
        isFragmentOn = false;
    }

    private SurfaceHolder.Callback surfaceListener = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder){
            mCamera = Camera.open();

            try {
                mCamera.setPreviewDisplay(holder);
                cameraRotateInit();
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
            if(mCamera == null) {
                return;
            }
            Camera.Parameters parameters = mCamera.getParameters();
            /*
            int defaultsizeh = parameters.getPreviewSize().height;
            int defaultsizew = parameters.getPreviewSize().width;

            Log.e("DEFAULT", defaultsizew + " * " + defaultsizeh);
            Log.e("paramet", width +" * " + height);

            parameters.setPreviewSize(width, height);
            mCamera.setParameters(parameters);
            */
            mCamera.startPreview();

            Log.e("On Resume", "Camera type :: " + camera_type);
            selectCamera(camera_type);
            Log.e("On Resume", "Camera type :: " + camera_type);
        }
    };

    private void switchCamera() {
        structure_image.setVisibility(View.INVISIBLE);
        if(camera_type == FRONT_CAMERA){
            selectCamera(BACK_CAMERA);
        } else if(camera_type == BACK_CAMERA) {
            selectCamera(FRONT_CAMERA);
        }
    }

    private void selectCamera(int type) {
        int camera_id = -1;
        if (mCamera == null) {
            return;
        }
        mCamera.release();
        if(type == FRONT_CAMERA){
            camera_id = getCamera(Camera.CameraInfo.CAMERA_FACING_FRONT);
            if (camera_id >= 0) {
                mCamera = Camera.open(camera_id);
                camera_type = FRONT_CAMERA;
            }
        } else if(type == BACK_CAMERA) {
            camera_id = getCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
            if (camera_id >= 0) {
                mCamera = Camera.open(camera_id);
                camera_type = BACK_CAMERA;
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
        Log.e("File path", MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString());
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath(), "/phoneCaHak");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        mediaFile = new File(mediaStorageDir.getAbsolutePath() + "/" + File.separator + "IMG_" + timeStamp + ".jpg");
        return mediaFile;
    }

    private void refreshCamera(){
        mCamera.stopPreview();
        try {
            mCamera.setPreviewDisplay(holder);
            cameraRotateInit ();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        if (display.getRotation() == Surface.ROTATION_0 || display.getRotation() == Surface.ROTATION_180) {
            isLandscape = false;

            Log.e("On Resume", "Camera type :: " + camera_type);
            selectCamera(camera_type);
            Log.e("On Resume", "Camera type :: " + camera_type);
        } else if (display.getRotation() == Surface.ROTATION_90 || display.getRotation() == Surface.ROTATION_270) {
            isLandscape = true;

            Log.e("On Resume", "Camera type :: " + camera_type);
            selectCamera(camera_type);
            Log.e("On Resume", "Camera type :: " + camera_type);
        }

        if (isFragmentOn){
            closeFragment();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        angleStatusSwitch(false);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        pitch = Math.round(event.values[0]);
        roll = Math.round(event.values[1]);
        azimuth = Math.round(event.values[2]);

        //Log.i("SENSOR TEST", "X::" + pitch + "  Y::" + roll + "  Z:: " + azimuth);
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        if (display.getRotation() == Surface.ROTATION_0 || display.getRotation() == Surface.ROTATION_180) {
            if (roll > -100 && roll < -90) {
                angleStatusSwitch(true);
                notifyGoodAngle();
            } else {
                angleStatusSwitch(false);
                isAlaramed = false;
            }
        } else if (display.getRotation() == Surface.ROTATION_90) {
            if(azimuth < 85 && azimuth > 75) {
                angleStatusSwitch(true);
                notifyGoodAngle ();
            } else {
                angleStatusSwitch(false);
                isAlaramed = false;
            }
        } else if (display.getRotation() == Surface.ROTATION_270) {
            if(azimuth < -85 && azimuth > -95) {
                angleStatusSwitch(true);
                notifyGoodAngle ();
            } else {
                angleStatusSwitch(false);
                isAlaramed = false;
            }
        }
    }

    private long sensorBreakTimer = 0;      /* Write the time when was last alarm */
    private boolean isAlaramed = false;     /* Prevent too many vibrations */
    private void notifyGoodAngle () {
        angleStatusSwitch(true);
        if (System.currentTimeMillis() > sensorBreakTimer + 10000) {
            sensorBreakTimer = System.currentTimeMillis();
            Toast.makeText(this, "사진 찍기 좋은 각도입니다.", Toast.LENGTH_SHORT).show();
            if (!isAlaramed) {
                vibrator.vibrate(500);
                isAlaramed = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private boolean photo_taken_lock = false;
    private void buttonInit () {
        Button rotate_camera_button;
        Button camera_button;
        Button select_structure_button;
        Button etc_button;

        rotate_camera_button = (Button) findViewById(R.id.rotateCameraButton);
        rotate_camera_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
                if (isFragmentOn){
                    closeFragment();
                }
            }
        });

        camera_button = (Button) findViewById(R.id.cameraButton);
        camera_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (photo_taken_lock == false) {
                    photo_taken_lock = true;
                    mCamera.takePicture(null, null, mPicture);
                    photo_taken_lock = false;
                }
            }
        });

        select_structure_button = (Button) findViewById(R.id.selectStructureButton);
        select_structure_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //structure_layout.setVisibility(View.VISIBLE);
                if (isFragmentOn){
                    closeFragment();
                } else {
                    replaceFragment();
                }
            }
        });

        etc_button = (Button) findViewById(R.id.etcButton);
        etc_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CameraActivity.this, etcActivity.class);
                startActivity(intent);
            }
        });
    }

    private void cameraRotateInit () {
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        Camera.CameraInfo camInfo = new Camera.CameraInfo();

        int result = camInfo.orientation;// + display.getRotation();
        if (display.getRotation() == Surface.ROTATION_0 || display.getRotation() == Surface.ROTATION_180) {
            result -= 0;
        } else if (display.getRotation() == Surface.ROTATION_90) {
            result -= 90;
        } else if (display.getRotation() == Surface.ROTATION_270) {
            result -= 270;
        }
        result += 450;
        result = result %360;
        mCamera.setDisplayOrientation(result);
    }

    @Override
    public void onBackPressed(){
        if (isFragmentOn) {
            closeFragment();
        } else {
            backButtonHandler.onBackPressed();
        }
    }

    private void angleStatusSwitch(boolean onOff) {
        if(onOff) {
            angle_status.setImageResource(R.drawable.gyro_on);
        } else {
            angle_status.setImageResource(R.drawable.gyro_off);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Log.w("Hello!", "WORLD");
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        Log.e("DESTROY", "YEAH!!");
    }

    private void getThumbnail() {
        String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE
        };
        final Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");

        if (cursor.moveToFirst()) {
            String imageLocation = cursor.getString(1);
            File imageFile = new File(imageLocation);
            if (imageFile.exists()) {
                Bitmap thumbnail = BitmapFactory.decodeFile(imageLocation);
                if (thumbnail != null) {
                    int height = thumbnail.getHeight();
                    int width = thumbnail.getWidth();

                    Bitmap resized = null;
                    while (height > 118) {
                        resized = Bitmap.createScaledBitmap(thumbnail, (width * 118) / height, 118, true);
                        height = resized.getHeight();
                        width = resized.getWidth();
                    }
                    ImageView thumbnailView = (ImageView) findViewById(R.id.thumbnailView);
                    thumbnailView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setAction(android.content.Intent.ACTION_VIEW);
                            intent.setType("image/*");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    });
                    thumbnailView.setImageBitmap(resized);
                } else {
                    Log.e("Thumbnail", "We can not find thumbnail");
                }
            } else {
                Log.e("Thumbnail", "File Cannot find");
            }
        }
    }

    private void galleryAddPic(String mCurrentPhotoPath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
}