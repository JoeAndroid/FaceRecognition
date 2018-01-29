package com.ughouse.facegverify.util;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by qiaobing on 2018/1/19.
 */

public class CommonUtils {

    /**
     * 获得一个UUID
     *
     * @return String UUID
     */
    public static String getUUID() {
        String uuid = UUID.randomUUID().toString();
//去掉“-”符号
        return uuid.replaceAll("-", "");
    }

    /**
     * 查找出出现次数最多的元素，并且获得出现次数
     *
     * @param list
     */
    public static void getSampMaxCout(List<String> list) {
        String regex;
        Pattern p;
        Matcher m;
        String tmp = "";
        int max_cnt = 0;
        String max_str = "";
        for (String str : list) {
            if (tmp.equals(str)) continue;
            tmp = str;
            regex = str;
            p = Pattern.compile(regex);
            m = p.matcher(list.toString());
            int cnt = 0;
            while (m.find()) {
                cnt++;
            }
            //System.out.println(str + ":" + cnt);
            if (cnt > max_cnt) {
                max_cnt = cnt;
                max_str = str;
            }
        }
        LogUtils.e("人脸名称 " + max_str + " 出现的最大次数：" + max_cnt);
    }

    /**
     * 将摄像头数据转为 bitmap
     *
     * @param data
     * @param width
     * @param height
     * @return
     */
    public static Bitmap rawByteArray2RGBABitmap2(byte[] data, int width, int height) {
        int frameSize = width * height;
        int[] rgba = new int[frameSize];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int y = (0xff & ((int) data[i * width + j]));
                int u = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 0]));
                int v = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 1]));
                y = y < 16 ? 16 : y;
                int r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128));
                int g = Math.round(1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = Math.round(1.164f * (y - 16) + 2.018f * (u - 128));
                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);
                rgba[i * width + j] = 0xff000000 + (b << 16) + (g << 8) + r;
            }
        }
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0, width, 0, 0, width, height);
        return bmp;
    }

    public static String getTopActivityName(Context context) {
        String topActivityClassName = null;
        ActivityManager activityManager = (ActivityManager) (context.getSystemService(Context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = activityManager.getRunningTasks(1);
        if (runningTaskInfos != null) {
            ComponentName f = runningTaskInfos.get(0).topActivity;
            topActivityClassName = f.getClassName();
        }
        return topActivityClassName;
    }

    //判断应用是否在前端运行
    public static boolean isRunningForeground(Context context) {
        String packageName = context.getPackageName();
        String topActivityClassName = getTopActivityName(context);
        if (packageName != null && topActivityClassName != null && topActivityClassName.startsWith(packageName)) {
            return true;
        } else {
            return false;
        }
    }

    // 判断服务是否正在运行
    public static boolean isServiceWork(Context mContext, String serviceName) {
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

