package com.ughouse.facegverify.service;

import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.ughouse.facegverify.activity.WelComeActivity;
import com.ughouse.facegverify.util.CommonUtils;

import java.util.List;

/**
 * Created by qiaobing on 2018/1/29.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MyJobService extends JobService {

    private int kJobId = 0;

    @Override
    public void onCreate() {
        super.onCreate();
//        scheduleJob(getJobInfo());
        startJobSheduler();
    }

    public void startJobSheduler() {
        try {
            int id = 1;
            JobInfo.Builder builder = new JobInfo.Builder(id,
                    new ComponentName(getPackageName(), MyJobService.class.getName()));
            builder.setPeriodic(1000);  //间隔500毫秒调用onStartJob函数， 500只是为了验证
            JobScheduler jobScheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            int ret = jobScheduler.schedule(builder.build());
            // Android24版本才有scheduleAsPackage方法， 期待中
            //Class clz = Class.forName("android.app.job.JobScheduler");
            //Method[] methods = clz.getMethods();
            //Method method = clz.getMethod("scheduleAsPackage", JobInfo.class , String.class, Integer.class, String.class);
            //Object obj = method.invoke(jobScheduler, builder.build(), "com.brycegao.autostart", "brycegao", "test");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d("MyJobService", "onStartJob alive");
        boolean isLocalServiceWork = isServiceWork(this, "com.ughouse.facegverify.service.FRService");
        if (!isLocalServiceWork) {
            this.startService(new Intent(this, FRService.class));
        }
       /* String activityName = CommonUtils.getTopActivityName(this);
        if (!activityName.contains("com.ughouse.facegverify")) {
           *//* Intent intent = new Intent(this, WelComeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);*//*
            boolean isLocalServiceWork = isServiceWork(this, "com.ughouse.facegverify.service.FRService");
            if (!isLocalServiceWork) {
                this.startService(new Intent(this, FRService.class));
            }
        }*/
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d("MyJobService", "onStopJob alive");
//        scheduleJob(getJobInfo());
        return true;
    }

    //将任务作业发送到作业调度中去
    public void scheduleJob(JobInfo t) {
//        Log.i("MyJobService", "调度job");
        JobScheduler tm =
                (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        tm.schedule(t);
    }

    public JobInfo getJobInfo() {
        JobInfo.Builder builder = new JobInfo.Builder(kJobId++, new ComponentName(this, MyJobService.class));
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        builder.setPersisted(true);
        builder.setRequiresCharging(false);
        builder.setRequiresDeviceIdle(false);
        //间隔100毫秒
        builder.setPeriodic(100);
        return builder.build();
    }

    // 判断服务是否正在运行
    public boolean isServiceWork(Context mContext, String serviceName) {
        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(100);
        if (myList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }
}


