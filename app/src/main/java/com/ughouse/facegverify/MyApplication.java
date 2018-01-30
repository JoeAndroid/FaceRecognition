package com.ughouse.facegverify;

import android.app.Application;
import android.net.Uri;

import com.ughouse.facegverify.constant.FunctionType;
import com.ughouse.facegverify.db.FaceDB;

/**
 * Created by qiaobing on 2018/1/16.
 */
public class MyApplication extends Application {

    private final String TAG = this.getClass().toString();
    public FaceDB mFaceDB;
    Uri mImage;

    public static String ip;
    public static String roomCode;
    public static String times = "5";
    public static String countDownTime = "30";
    public static int functionType = FunctionType.GATHER_CONTRAST_TYPE;

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.create(this);
        mFaceDB = new FaceDB(this.getExternalCacheDir().getPath());
        mImage = null;
    }

    public void setCaptureImage(Uri uri) {
        mImage = uri;
    }

    public Uri getCaptureImage() {
        return mImage;
    }
}
