/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wuwang.aavt.media;

import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.wuwang.aavt.log.AvLog;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * CameraProvider 相机数据
 *
 * @author wuwang
 * @version v1.0 2017:10:26 18:09
 */
public class CameraProvider implements ITextureProvider {

    private Camera mCamera;
    private int cameraId = 1;
    private SurfaceTexture surface;
    private Semaphore mFrameSem;
    private boolean isLandscape;//是否是横屏
    private boolean isFlashLight;//是否开启了闪光灯
    private String tag = getClass().getSimpleName();

    @Override
    public Point open(SurfaceTexture surface) {
        final Point size = new Point();
        this.surface = surface;
        try {
            mFrameSem = new Semaphore(0);
            mCamera = Camera.open(cameraId);
            mCamera.setPreviewTexture(surface);
            surface.setOnFrameAvailableListener(frameListener);
            Camera.Size s = mCamera.getParameters().getPreviewSize();
            mCamera.startPreview();
            if (isLandscape) {
                mCamera.setDisplayOrientation(270);
                size.x = s.width;
                size.y = s.height;
            } else {
                mCamera.setDisplayOrientation(0);
                size.x = s.height;
                size.y = s.width;
            }
            AvLog.i(tag, "Camera Opened");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return size;
    }

    @Override
    public void switchCamera() {
        Point size = new Point();
        try {
            mCamera.stopPreview();
            mCamera.release();
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
            } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                //如果开启闪光灯关闭
                Camera.Parameters mParameters;
                mParameters = mCamera.getParameters();
                mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(mParameters);
            }
            mCamera.setPreviewTexture(surface);
            surface.setOnFrameAvailableListener(frameListener);
            Camera.Size s = mCamera.getParameters().getPreviewSize();
            mCamera.startPreview();
            size.x = s.height;
            size.y = s.width;
            AvLog.i(tag, "Camera Opened");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setIsLandscape(boolean isLandscape) {
        this.isLandscape = isLandscape;
    }

    @Override
    public void switchFlashLight() {
        try {
            if (mCamera != null && cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                Point size = new Point();
                if (!isFlashLight) {
                    isFlashLight = true;
                    Camera.Parameters mParameters;
                    mParameters = mCamera.getParameters();
                    mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    mCamera.setParameters(mParameters);
                } else {
                    isFlashLight = false;
                    Camera.Parameters mParameters;
                    mParameters = mCamera.getParameters();
                    mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(mParameters);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void close() {
        mFrameSem.drainPermits();
        mFrameSem.release();

        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    @Override
    public boolean frame() {
        try {
            mFrameSem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public long getTimeStamp() {
        return -1;
    }

    @Override
    public boolean isLandscape() {
        return true;
    }

    private SurfaceTexture.OnFrameAvailableListener frameListener = new SurfaceTexture.OnFrameAvailableListener() {

        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            AvLog.d(tag, "onFrameAvailable");
            mFrameSem.drainPermits();
            mFrameSem.release();
        }

    };

}
