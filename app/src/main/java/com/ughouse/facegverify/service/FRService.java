package com.ughouse.facegverify.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.ughouse.facegverify.MessageEvent;
import com.ughouse.facegverify.MyApplication;
import com.ughouse.facegverify.R;
import com.ughouse.facegverify.activity.DetecterActivity;
import com.ughouse.facegverify.activity.GatherFaceActivity;
import com.ughouse.facegverify.bean.UgHouseBean;
import com.ughouse.facegverify.constant.FunctionType;
import com.ughouse.facegverify.util.LogUtils;
import com.ughouse.facegverify.util.MySqlUtil;
import com.ughouse.facegverify.util.SPUtils;

import org.greenrobot.eventbus.EventBus;

import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 后台服务，主要进行人脸识别，开启/关闭人脸识别摄像头，识别正确写入后台数据库，
 * Created by qiaobing on 2018/1/16.
 */
public class FRService extends Service {
    //数据库存储读取消息
    private final static int MSG_EVENT_READ = 0x1001;//读取 ughouse
    private final static int MSG_EVENT_GATHER = 0x1002;//采集样本
    private final static int MSG_EVENT_CONTRAST = 0x1003;//人脸对比
    private final static int MSG_EVENT_DELETE = 0x1004;//删除样本
    private final static int MSG_EVENT_CLOSE_CAMERA = 0x1005;//关闭摄像头
    private final static int MSG_EVENT_CONNECT_DATABASE = 0x1006;//连接数据库

    private UgHouseBean houseBean;
    //数据库 id
    private String ip;
    private int functionType;
    //门牌号
    private String roomCode;
    //对比或采集次数
    private String times;
    //倒计时时间
    private String countDownTime;
    //当前 ug_house中 state 状态
    private int currentState = 0;
    //每一秒查询一下 ughouse
    private Timer queryTimer;
    private MyTimerTask queryTask;
    //服务销毁了
    private boolean isDestroy;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_EVENT_CONNECT_DATABASE://连接数据库
                    connectDataBase();
                    break;
                case MSG_EVENT_READ:
                    //启动线程读取ug_house表state值
                    queryUgHouseState();
                    break;
                case MSG_EVENT_GATHER://人脸采集
                    goToGatherFace();
                    break;
                case MSG_EVENT_CONTRAST://人脸对比
                    goToDetectre();
                    break;
                case MSG_EVENT_DELETE://删除数据库样本
                    deleteFaceData();
                    break;
                case MSG_EVENT_CLOSE_CAMERA://关闭摄像头
                    EventBus.getDefault().post(new MessageEvent("close"));
                    break;
            }
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //API 18以下，直接发送Notification并将其置为前台
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            startForeground(100, new Notification());
        } else {
            //API 18以上，发送Notification并将其置为前台后，启动InnerService
            Notification.Builder builder = new Notification.Builder(this);
            builder.setSmallIcon(R.mipmap.ic_launcher);
            startForeground(100, builder.build());
            startService(new Intent(this, InnerService.class));
        }

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.e("FRService---" + "onStartCommand");
        getOverallArguments();
        //连接数据库，成功后读取 Ug_house表
        try {
            if (MySqlUtil.conn == null || MySqlUtil.conn.isClosed()) {
                handler.sendEmptyMessage(MSG_EVENT_CONNECT_DATABASE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return START_STICKY;
    }

    /**
     * 获取全局参数
     */
    private void getOverallArguments() {
        if (!TextUtils.isEmpty(SPUtils.getInstance(this).getData("ip"))) {
            ip = SPUtils.getInstance(this).getData("ip");
        } else {
            ip = MyApplication.ip;
        }
        if (!TextUtils.isEmpty(SPUtils.getInstance(this).getData("roomCode"))) {
            roomCode = SPUtils.getInstance(this).getData("roomCode");
        } else {
            roomCode = MyApplication.roomCode;
        }
        if (!TextUtils.isEmpty(SPUtils.getInstance(this).getData("countDownTime"))) {
            countDownTime = SPUtils.getInstance(this).getData("countDownTime");
        } else {
            countDownTime = MyApplication.countDownTime;
        }
        if (!TextUtils.isEmpty(SPUtils.getInstance(this).getData("functionType"))) {
            functionType = Integer.valueOf(SPUtils.getInstance(this).getData("functionType"));
        } else {
            functionType = MyApplication.functionType;
        }
        if (!TextUtils.isEmpty(SPUtils.getInstance(this).getData("times"))) {
            times = SPUtils.getInstance(this).getData("times");
        } else {
            times = MyApplication.times;
        }
    }

    /**
     * 连接数据库
     */
    private void connectDataBase() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MySqlUtil.openConnection("jdbc:mysql://" + ip + ":3306/ugohousedb", "root", "ugo001");
                // conn = MySqlUtil.openConnection("jdbc:mysql://" + ip + ":3306/test", "root", "ugo001");
                if (MySqlUtil.conn != null) {
                    LogUtils.e("连接数据库成功");
                    //读取数据库所有人脸数据
//                    List<UgFaceDataBean> faceDatalist = ((MyApplication) getApplicationContext()).mFaceDB.loadFaceData();
//                    LogUtils.e("-------------------------读取数据库人脸数据成功开始读取 Ug_house表-------------------------");
                    handler.sendEmptyMessage(MSG_EVENT_READ);
                } else {
                    LogUtils.e("连接数据库失败，重连");
                    if (!isDestroy) {
                        handler.sendEmptyMessage(MSG_EVENT_CONNECT_DATABASE);
                    }
                }
            }
        }).start();
    }

    //每隔一秒查询 state 一次
    private void queryUgHouseState() {
        if (queryTimer != null) {
            if (queryTask != null) {
                queryTask.cancel();
            }
            queryTask = new MyTimerTask();
            queryTimer.schedule(queryTask, 0, 1000);
        } else {
            queryTimer = new Timer();
            queryTask = new MyTimerTask();
            queryTimer.schedule(queryTask, 0, 1000);
        }
    }

    /**
     * 删除人脸样本
     */
    private void deleteFaceData() {
        //删除样本流程：把样本表ug_face_data里面的对应样本状态state设置为1，
        // 把ug_house表里面的face_user_no设置为空，state=0，face_state=0，face_num=0。
//        String sql = "update ug_face_data set state='1' where id=" + houseBean.getId();
//        MySqlUtil.execSQL(MySqlUtil.conn, sql);
        new Thread() {
            @Override
            public void run() {
                String sql2 = "update ug_house set face_user_no='', state='0', face_state='0', face_num='0' where (state = 3 or  state = 4) and id=" + houseBean.getId();
                MySqlUtil.execSQL(MySqlUtil.conn, sql2);
            }
        }.start();
    }

    /**
     * 人脸对比
     */
    private void goToDetectre() {
        if ((null == houseBean.getFace_user_no() || TextUtils.isEmpty(houseBean.getFace_user_no())) && houseBean.getFace_state() != null && "0".equals(houseBean.getFace_state().trim())) {
            Intent it = new Intent(getBaseContext(), DetecterActivity.class);
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            it.putExtra("Camera", 1);
            getApplication().startActivity(it);
        }
    }

    /**
     * 人脸采集
     */
    private void goToGatherFace() {
        if (null != houseBean.getFace_user_no() && !TextUtils.isEmpty(houseBean.getFace_user_no()) && houseBean.getFace_state() != null && "0".equals(houseBean.getFace_state().trim())) {
            Intent intent = new Intent(getBaseContext(), GatherFaceActivity.class);
            intent.putExtra("face_user_no", houseBean.getFace_user_no());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApplication().startActivity(intent);
        }
    }

    private class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            //根据Ug_house表 id查询表数据
            houseBean = MySqlUtil.queryUgHouse(MySqlUtil.conn, "select * from ug_house where id=" + roomCode);
            if (houseBean != null) {
                int facestate = Integer.parseInt(houseBean.getFace_state());
                //如果facestate=-1的时候，说明已经执行或人脸采集或对比，没有采集或找到合适的。
                if (facestate != -1) {
                    int state = Integer.parseInt(houseBean.getState());
                    if (currentState != state) {
                        currentState = state;
                        switch (state) {
                            case 1://启动摄像头进入采集流程
                                if (functionType != FunctionType.CONTRAST_TYPE)
                                    handler.sendEmptyMessage(MSG_EVENT_GATHER);
                                break;
                            case 2://启动摄像头进入对比流程
                                if (functionType != FunctionType.GATHER_TYPE)
                                    handler.sendEmptyMessage(MSG_EVENT_CONTRAST);
                                break;
                            case 3://进入删除样本流程
                            case 4:
                                handler.sendEmptyMessage(MSG_EVENT_DELETE);
                                break;
                            default://停止摄像头工作
                                handler.sendEmptyMessage(MSG_EVENT_CLOSE_CAMERA);
                                break;
                        }
                    }
                }
            } else {
                //获取对应 id的表数据失败
                handler.sendEmptyMessage(MSG_EVENT_CONNECT_DATABASE);
            }
        }
    }


    @Override
    public void onDestroy() {
        try {
            if (queryTask != null) {
                queryTask.cancel();
            }
            if (queryTimer != null) {
                queryTimer.cancel();
            }
            currentState = 0;
            isDestroy = true;
            handler.removeCallbacksAndMessages(null);
            if (MySqlUtil.conn != null && !MySqlUtil.conn.isClosed()) {
                MySqlUtil.conn.close();
                MySqlUtil.conn = null;
                LogUtils.e("断开数据库连接");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //启动服务保活
        startService(new Intent(this, FRService.class));
        super.onDestroy();
    }
}
