package com.ughouse.facegverify.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences本地存储
 */
public class SPUtils {

    /**
     * 内容
     */
    private static Context mycontext;

    /**
     * 该工具类实例
     */
    private static SPUtils preferencesUtils;

    /**
     * 数据管理类
     */
    private static SharedPreferences.Editor editor;

    /**
     * 数据库存储类
     */
    private static SharedPreferences sharedPreferences;

    public static SPUtils getInstance(Context context) {
        mycontext = context;
        if (null == preferencesUtils) {
            synchronized (SPUtils.class) {
                if (null == preferencesUtils) {
                    preferencesUtils = new SPUtils();
                    sharedPreferences = mycontext.getApplicationContext()
                            .getSharedPreferences("faceverify", 0);
                    editor = sharedPreferences.edit();

                }
            }
        }
        return preferencesUtils;
    }

    /**
     * 保存数据到Preferences
     *
     * @param key   String类型的key值
     * @param value String类型的数据 默认为值为null
     */
    public boolean saveData(String key, String value) {
        // 保存数据;
        editor.putString(key, value);
        // 提交完成
        return editor.commit();
    }

    /**
     * 根据key 返回数据
     *
     * @param key String类型的key值
     * @return
     */
    public String getData(String key) {
        return sharedPreferences.getString(key, null);
    }

    /**
     * 根据key 返回数据
     *
     * @param key          key String类型的key值
     * @param defaultValue 默认值 String类型
     * @return
     */
    public String getData(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }
}
