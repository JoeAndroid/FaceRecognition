package com.ughouse.facegverify.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import com.ughouse.facegverify.R;

/**
 * Created by qiaobing on 2018/1/29.
 */

public class InnerService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //发送与KeepLiveService中ID相同的Notification，然后将其取消并取消自己的前台显示
        Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        startForeground(100, builder.build());
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopForeground(true);
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                manager.cancel(100);
                stopSelf();
            }
        }, 100);

    }
}