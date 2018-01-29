package com.ughouse.facegverify.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by qiaobing on 2018/1/19.
 */

public class DateUtils {

    public static String getDateyyMMdd(long timeLong) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(new Date(timeLong));
    }
}
