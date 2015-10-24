package com.example.jiangshen.feedmethis;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

/**
 * Created by Jesse on 10/24/2015.
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback{

    Context mContext;
    Camera mCamera;

    public CameraView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public void init() {
        try {
            mCamera.open();
        } catch (Exception e) {
            Toast.makeText(mContext, "Something went wrong! " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
