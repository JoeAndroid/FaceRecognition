package com.ughouse.facegverify.util;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * Created by qiaobing on 2018/1/17.
 */

public class FileUtils {


    /**
     * 获取手机存储路径
     */
    public static String getFilePath() {
        String path = null;
        // 判断外部设备是否存在
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // 存在获取外部文件路径
            if (null != getExternalCacheDir()) {
                path = getExternalCacheDir().getPath();
            } else {
                path = Environment.getExternalStorageDirectory().getPath() + "/com.ughome.facegverify/";
            }
        } else {
            // 不存在获取内部存储
            path = Environment.getDataDirectory().getPath() + "/com.ughome.facegverify/";
        }
        return path;
    }

    private static File getExternalCacheDir() {
        File dataDir = new File(new File(Environment.getExternalStorageDirectory(), "Android"), "data");
        File appCacheDir = new File(new File(dataDir, "com.ughome.facegverify"), "cache");
        if (!appCacheDir.exists()) {
            if (!appCacheDir.mkdirs()) {
                LogUtils.w("Unable to create external cache directory");
                return null;
            }

            try {
                (new File(appCacheDir, ".nomedia")).createNewFile();
            } catch (IOException var4) {
                LogUtils.i("Can\'t create \".nomedia\" file in application external cache directory");
            }
        }

        return appCacheDir;
    }
}