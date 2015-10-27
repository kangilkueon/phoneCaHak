package com.tocotoucan.soft.phonecahak;

import android.content.Context;

import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by kangilkueon on 15. 9. 15.
 */
public class MyCameraSurface  extends SurfaceView implements SurfaceHolder.Callback{
    SurfaceHolder mHolder;
    Camera mCamera;

    public MyCameraSurface(Context context){
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder){
        mCamera = Camera.open();

        try {
            mCamera.setPreviewDisplay(mHolder);
        } catch (Exception e){
            e.printStackTrace();
            mCamera.release();
            mCamera = null;
        }
    }
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;

    }

    // 표면의 크기가 결정될 때 최적의 미리보기 크기를 구해 설정한다.
    public void surfaceChanged(SurfaceHolder holder, int format, int width,	int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        //parameters.setPreviewSize(width, height);
        //mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

}
