package com.tocotoucan.soft.phonecahak;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by kangilkueon on 15. 10. 28.
 */
public class CameraActivity extends AppCompatActivity implements StructureSelectFragment.OnFragmentInteractionListener, SensorEventListener {
    static final int FRONT_CAMERA = 0;
    static final int BACK_CAMERA = 1;

    /* Layout component */
    SurfaceView camera_preview;
    SurfaceHolder holder;
    LinearLayout timer_btn_layout;
    LinearLayout timer_count_layout;
    TextView timer_count_txt;

    ImageView structure_image;
    ImageView angle_status;

    Camera mCamera;
    Camera.PictureCallback mPicture;

    static int camera_type = BACK_CAMERA;
    static int timer_count = 0;

    /* Orientation Sensor */
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

    private Vibrator vibrator;

    /* Animation */
    private Animation timer_area_out_anim;
    private Animation timer_area_in_anim;

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
                    Toast.makeText(CameraActivity.this, "사진이 저장되었습니다 : " + pictureFile.getName(), Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(CameraActivity.this, "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_LONG).show();
                }
                galleryAddPic(pictureFile.getPath());
                refreshCamera();
                getThumbnail();
            }
        };

        structure_image = (ImageView) findViewById(R.id.structureImageView);

        timer_btn_layout = (LinearLayout) findViewById(R.id.layout_timer_btn_set);
        timer_count_layout = (LinearLayout) findViewById(R.id.layout_timer_count);

        camera_preview = (SurfaceView) findViewById(R.id.cameraSurface);

        holder = camera_preview.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(surfaceListener);


        angle_status = (ImageView) findViewById(R.id.angleStatus);

        /* 화면 방향에 따른 레이아웃 설정 */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);


        backButtonHandler = new BackButtonHandler(this);
        structureFragment = new StructureSelectFragment();

        camera_preview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    float x = event.getX();
                    float y = event.getY();

                    touchFocus((int) x, (int) y);
                    DrawFocusRect(x - 50, y - 50, x + 50, y + 50, Color.GREEN);

                }
                return true;
            }
        });
        getThumbnail();

        SurfaceView transparentView = (SurfaceView)findViewById(R.id.transparentSurface);

        holderTransparent = transparentView.getHolder();
        holderTransparent.setFormat(PixelFormat.TRANSPARENT);
        holderTransparent.addCallback(surfaceListener2);
        holderTransparent.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        timer_count_txt = (TextView) findViewById(R.id.txt_timer_count);

        /* Initialize animation */
        timer_area_out_anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.move_bottom_top);

        timer_area_in_anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.small_to_big);

        mTask = new TimerTask() {
            @Override
            public void run() {
                Log.i("Timer", "Tick tokc" + timer_count);
                Message msg = handle.obtainMessage();
                handle.sendMessage(msg);
                if (timer_count <= 0) {
                    mTimer.cancel();
                    mCamera.takePicture(null, null, mPicture);
                    photo_taken_lock = false;
                    Log.i("Timer", "Take a picture!");
                }
            }
        };

        mTimer = new Timer();

        buttonInit();
        layoutInit();

    }

    final Handler handle = new Handler () {
        public void handleMessage (Message msg){
            timer_count_txt.setText("" + timer_count--);
            if (timer_count < 0) {
                timer_count_layout.setVisibility(View.INVISIBLE);
            }
        }
    };

    private void layoutInit() {
        closeFragment();

        structure_image.setVisibility(View.INVISIBLE);
        timer_btn_layout.setVisibility(View.INVISIBLE);
        timer_count_layout.setVisibility(View.INVISIBLE);
    }

    SurfaceHolder holderTransparent;

    private void DrawFocusRect(float RectLeft, float RectTop, float RectRight, float RectBottom, int color)
    {

        Canvas canvas = holderTransparent.lockCanvas();
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        //border's properties
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(3);
        canvas.drawRect(RectLeft, RectTop, RectRight, RectBottom, paint);


        holderTransparent.unlockCanvasAndPost(canvas);
    }
    /* Fragment 생성 및 변경 */
    private void replaceFragment(){
        isFragmentOn = true;

        Bundle argument = new Bundle();
        argument.putBoolean("isLandscape", isLandscape);
        argument.putInt("camera_type", camera_type);
        structureFragment.setArguments(argument);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.move_bottom_top, R.anim.move_bottom_top);
        transaction.replace(R.id.structureLayout, structureFragment);
        transaction.commit();
    }

    /* Fragment 닫기 */
    private void closeFragment () {
        isFragmentOn = false;

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.move_top_bottom, R.anim.move_top_bottom);
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

            int defaultsizeh = parameters.getPreviewSize().height;
            int defaultsizew = parameters.getPreviewSize().width;

            Log.e("DEFAULT", defaultsizew + " * " + defaultsizeh);
            Log.e("paramet", width + " * " + height);

            if(isLandscape) {
                parameters.set("orientation", "landscape");
                parameters.setPreviewSize(width, height);
            }
            else {
                parameters.set("orientation", "portrait");
                parameters.setPreviewSize(height, width);
            }
            mCamera.setParameters(parameters);
            mCamera.startPreview();

            Log.e("On Resume", "Camera type :: " + camera_type);
            selectCamera(camera_type);
            Log.e("On Resume", "Camera type :: " + camera_type);
        }
    };

    private SurfaceHolder.Callback surfaceListener2 = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder){
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,	int height) {
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
                isAlarmed = false;
            }
        } else if (display.getRotation() == Surface.ROTATION_90) {
            if(azimuth < 85 && azimuth > 75) {
                angleStatusSwitch(true);
                notifyGoodAngle ();
            } else {
                angleStatusSwitch(false);
                isAlarmed = false;
            }
        } else if (display.getRotation() == Surface.ROTATION_270) {
            if(azimuth < -85 && azimuth > -95) {
                angleStatusSwitch(true);
                notifyGoodAngle ();
            } else {
                angleStatusSwitch(false);
                isAlarmed = false;
            }
        }
    }

    private long sensorBreakTimer = 0;      /* Write the time when was last alarm */
    private boolean isAlarmed = false;     /* Prevent too many vibrations */
    private void notifyGoodAngle () {
        angleStatusSwitch(true);
        if (System.currentTimeMillis() > sensorBreakTimer + 10000) {
            sensorBreakTimer = System.currentTimeMillis();
            Toast.makeText(this, "사진 찍기 좋은 각도입니다.", Toast.LENGTH_SHORT).show();
            if (!isAlarmed) {
                vibrator.vibrate(500);
                isAlarmed = true;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private boolean photo_taken_lock = false;
    private void buttonInit () {
        Button rotate_camera_button;
        Button timer_button, timer_0_button, timer_1_button, timer_3_button,timer_5_button,timer_10_button;
        Button camera_button;
        Button select_structure_button;
        Button etc_button;

        rotate_camera_button = (Button) findViewById(R.id.rotateCameraButton);
        rotate_camera_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutInit();
                switchCamera();
                if (isFragmentOn){
                    closeFragment();
                }
            }
        });

        timer_button = (Button) findViewById(R.id.btn_timer);
        timer_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutInit();
                timerButtonSetAnim(0);
            }
        });

        timer_0_button = (Button) findViewById(R.id.btn_timer_0);
        timer_0_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer_count = 0;
                layoutInit();
                timerButtonSetAnim(1);
            }
        });

        timer_1_button = (Button) findViewById(R.id.btn_timer_1);
        timer_1_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer_count = 1;
                layoutInit();
                timerButtonSetAnim(1);
            }
        });

        timer_3_button = (Button) findViewById(R.id.btn_timer_3);
        timer_3_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer_count = 3;
                layoutInit();
                timerButtonSetAnim(1);
            }
        });

        timer_5_button = (Button) findViewById(R.id.btn_timer_5);
        timer_5_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer_count = 5;
                layoutInit();
                timerButtonSetAnim(1);
            }
        });

        timer_10_button = (Button) findViewById(R.id.btn_timer_10);
        timer_10_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer_count = 10;
                layoutInit();
                timerButtonSetAnim(1);
            }
        });

        camera_button = (Button) findViewById(R.id.cameraButton);
        camera_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                layoutInit();

                if (photo_taken_lock == false) {
                    photo_taken_lock = true;
                    if (timer_count != 0) {
                        timer_count_layout.setVisibility(View.VISIBLE);

                        timer_count_txt.setText("" + timer_count);
                        timer_count--;
                        mTimer.schedule(mTask, 1000, 1000);
                    } else {
                        mCamera.takePicture(null, null, mPicture);
                        photo_taken_lock = false;
                    }
                }
            }
        });

        select_structure_button = (Button) findViewById(R.id.selectStructureButton);
        select_structure_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                if (isFragmentOn){
                    layoutInit();
                } else {
                    layoutInit();
                    replaceFragment();
                }
            }
        });

        etc_button = (Button) findViewById(R.id.etcButton);
        etc_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutInit();
                Intent intent = new Intent(CameraActivity.this, etcActivity.class);
                startActivity(intent);
            }
        });
    }

    private TimerTask mTask;
    private Timer mTimer;

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

    private void setAutoFocusArea(Camera camera, int posX, int posY, int focusRange, boolean flag, Point point) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return;
        }

        if (posX < 0 || posY < 0) {
            setArea(camera, null);
            return;
        }

        int touchPointX;
        int touchPointY;
        int endFocusY;
        int startFocusY;

        if (!flag) {
            /** Camera.setDisplayOrientation()을 이용해서 영상을 세로로 보고 있는 경우. **/
            touchPointX = point.y >> 1;
            touchPointY = point.x >> 1;

            startFocusY = posX;
            endFocusY   = posY;
        } else {
            /** Camera.setDisplayOrientation()을 이용해서 영상을 가로로 보고 있는 경우. **/
            touchPointX = point.x >> 1;
            touchPointY = point.y >> 1;

            startFocusY = posY;
            endFocusY = point.x - posX;
        }

        float startFocusX   = 1000F / (float) touchPointY;
        float endFocusX     = 1000F / (float) touchPointX;

        /** 터치된 위치를 기준으로 focusing 영역으로 사용될 영역을 구한다. **/
        startFocusX = (int) (startFocusX * (float) (startFocusY - touchPointY)) - focusRange;
        startFocusY = (int) (endFocusX * (float) (endFocusY - touchPointX)) - focusRange;
        endFocusX = startFocusX + focusRange;
        endFocusY = startFocusY + focusRange;

        if (startFocusX < -1000)
            startFocusX = -1000;

        if (startFocusY < -1000)
            startFocusY = -1000;

        if (endFocusX > 1000) {
            endFocusX = 1000;
        }

        if (endFocusY > 1000) {
            endFocusY = 1000;
        }

    /*
     * 주의 : Android Developer 예제 소스 처럼 ArrayList에 Camera.Area를 2개 이상 넣게 되면
     *          에러가 발생되는 것을 경험할 수 있을 것입니다.
     **/
        Rect rect = new Rect((int) startFocusX, (int) startFocusY, (int) endFocusX, (int) endFocusY);
        ArrayList<Camera.Area> arraylist = new ArrayList<Camera.Area>();
        arraylist.add(new Camera.Area(rect, 1000)); // 지정된 영역을 100%의 가중치를 두겠다는 의미입니다.

        setArea(camera, arraylist);
    }

    private void setArea(Camera camera, List<Camera.Area> list) {
        boolean enableFocusModeMacro = true;

        Camera.Parameters parameters;
        parameters = camera.getParameters();

        int maxNumFocusAreas    = parameters.getMaxNumFocusAreas();
        int maxNumMeteringAreas = parameters.getMaxNumMeteringAreas();

        if (maxNumFocusAreas > 0) {
            parameters.setFocusAreas(list);
        }

        if (maxNumMeteringAreas > 0) {
            parameters.setMeteringAreas(list);
        }

        if (list == null || maxNumFocusAreas < 1 || maxNumMeteringAreas < 1) {
            enableFocusModeMacro = false;
        }

        if (enableFocusModeMacro == true) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        } else {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        camera.setParameters(parameters);
    }

    public void touchFocus(final int posX, final int posY){
        setAutoFocusArea(mCamera, posX, posY, 128, true, new Point(camera_preview.getWidth(), camera_preview.getHeight()));

        mCamera.autoFocus(null);
    }

    private void timerButtonSetAnim (int inout) {
        if (inout == 0) {
            timer_btn_layout.animate()
                    .translationY(0).alpha(1.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            timer_btn_layout.setVisibility(View.VISIBLE);
                            timer_btn_layout.setAlpha(0.0f);
                        }
                    });
        } else {
            timer_btn_layout.animate()
                    .translationY(0).alpha(0.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            timer_btn_layout.setVisibility(View.INVISIBLE);
                        }
                    });
        }
    }
}