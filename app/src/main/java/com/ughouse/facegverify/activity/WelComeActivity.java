package com.ughouse.facegverify.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import com.ughouse.facegverify.R;
import com.ughouse.facegverify.service.FRService;
import com.ughouse.facegverify.service.MyJobService;
import com.ughouse.facegverify.util.CommonUtils;

public class WelComeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置屏幕常亮
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//去掉信息栏
        setContentView(R.layout.activity_wel_come);
        //启动后台服务循环查询数据库
        //查询数据库某个字段是否开启摄像头（开启或关闭）
        //打开摄像头后与本地存储的人脸特征信息进行对比（第一次启动的时候已经进行了人脸特征采集）
        // 把正确匹配的人脸信息写入到后台数据库
        //应用保活服务
        startService(new Intent(this, MyJobService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isLocalServiceWork = CommonUtils.isServiceWork(this, "com.ughouse.facegverify.service.FRService");
        if (!isLocalServiceWork) {
            this.startService(new Intent(this, FRService.class));
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            //返回按钮
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        //断开服务
//        stopService(new Intent(this, FRService.class));
        super.onDestroy();
    }
}
