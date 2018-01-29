package com.ughouse.facegverify.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.ughouse.facegverify.activity.PermissionAcitivity;
import com.ughouse.facegverify.service.FRService;


/**
 * Created by qiaobing on 2018/1/27.
 */

public class BootReceiver extends BroadcastReceiver {

    public static final String action_boot = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(action_boot)) {
            //后边的XXX.class就是要启动的服务
            Intent service = new Intent(context, FRService.class);
            context.startService(service);
            Log.v("TAG", "开机自动服务自动启动.....");
            //启动应用，参数为需要自动启动的应用的包名
            Intent intent1 = context.getPackageManager().getLaunchIntentForPackage("com.ughouse.facegverify");
            context.startActivity(intent1);
           /* Toast.makeText(context, "收到开机广播", Toast.LENGTH_SHORT).show();
            Intent targetIntent = new Intent(context, PermissionAcitivity.class);
            targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(targetIntent);*/
        }

    }
}
