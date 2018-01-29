package com.ughouse.facegverify;

import android.content.Context;
import android.net.Uri;

import com.marswin89.marsdaemon.DaemonApplication;
import com.marswin89.marsdaemon.DaemonConfigurations;
import com.ughouse.facegverify.constant.FunctionType;
import com.ughouse.facegverify.db.FaceDB;
import com.ughouse.facegverify.receiver.AlarmReceiver;
import com.ughouse.facegverify.receiver.BootReceiver;
import com.ughouse.facegverify.service.DaemonService;
import com.ughouse.facegverify.service.FRService;

/**
 * Created by qiaobing on 2018/1/16.
 */
public class MyApplication extends DaemonApplication {

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

    /**
     * give the configuration to lib in this callback
     *
     * @return
     */
    @Override
    protected DaemonConfigurations getDaemonConfigurations() {
        DaemonConfigurations.DaemonConfiguration configuration1 = new DaemonConfigurations.DaemonConfiguration(
                "com.ughouse.facegverify:process1",
                FRService.class.getCanonicalName(),
                BootReceiver.class.getCanonicalName());

        DaemonConfigurations.DaemonConfiguration configuration2 = new DaemonConfigurations.DaemonConfiguration(
                "com.ughouse.facegverify:process2",
                DaemonService.class.getCanonicalName(),
                AlarmReceiver.class.getCanonicalName());

        DaemonConfigurations.DaemonListener listener = new MyDaemonListener();
        //return new DaemonConfigurations(configuration1, configuration2);//listener can be null
        return new DaemonConfigurations(configuration1, configuration2, listener);
    }


    class MyDaemonListener implements DaemonConfigurations.DaemonListener {
        @Override
        public void onPersistentStart(Context context) {
        }

        @Override
        public void onDaemonAssistantStart(Context context) {
        }

        @Override
        public void onWatchDaemonDaed() {
        }
    }
}
