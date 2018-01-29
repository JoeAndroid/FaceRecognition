package com.ughouse.facegverify.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.opengl.EGLSurface;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.arcsoft.facedetection.AFD_FSDKEngine;
import com.arcsoft.facedetection.AFD_FSDKError;
import com.arcsoft.facedetection.AFD_FSDKFace;
import com.arcsoft.facedetection.AFD_FSDKVersion;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.guo.android_extend.image.ImageConverter;
import com.ughouse.facegverify.MessageEvent;
import com.ughouse.facegverify.MyApplication;
import com.ughouse.facegverify.R;
import com.ughouse.facegverify.db.FaceDB;
import com.ughouse.facegverify.util.CommonUtils;
import com.ughouse.facegverify.util.LogUtils;
import com.ughouse.facegverify.util.MySqlUtil;
import com.ughouse.facegverify.util.SPUtils;
import com.wuwang.aavt.gl.FrameBuffer;
import com.wuwang.aavt.gl.YuvOutputFilter;
import com.wuwang.aavt.media.CameraProvider;
import com.wuwang.aavt.media.RenderBean;
import com.wuwang.aavt.media.SurfaceShower;
import com.wuwang.aavt.media.VideoSurfaceProcessor;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * 采集人脸
 */
public class GatherFaceActivity extends AppCompatActivity {

    private final String TAG = this.getClass().toString();

    private final static int MSG_CODE = 0x1000;
    private final static int MSG_EVENT_REG = 0x1001;
    private final static int MSG_EVENT_NO_FACE = 0x1002;
    private final static int MSG_EVENT_NO_FEATURE = 0x1003;
    private final static int MSG_EVENT_FD_ERROR = 0x1004;
    private final static int MSG_EVENT_FR_ERROR = 0x1005;
    private final static int MSG_EVENT_GATHER_SUCCESS = 0x1006;
    private final static int MSG_EVENT_GATHER_FAIL = 0x1007;

    private TextView tvCountDown;//倒计时
    private TextView tvContent, tvError;
    private VideoSurfaceProcessor mProcessor;
    private CameraProvider mProvider;
    private SurfaceShower mShower;
    private FrameBuffer mFb;
    private YuvOutputFilter mOutputFilter;
    private byte[] tempBuffer;
    private boolean exportFlag = false;
    private Bitmap mBitmap;
    private int picX = 368;
    private int picY = 640;

    private UIHandler mUIHandler;
    private Rect src = new Rect();
    private Rect dst = new Rect();
    private Thread view;
    private AFR_FSDKFace mAFR_FSDKFace;

    private String face_user_no;
    private String roomCode;
    private String times;//对比或采集次数
    private String countDownTime;
    private int currentDownTime;
    private CountDownTimer countDownTimer;
    //是否成功三次采集
    private boolean isSuccessGather;
    //成功次数
    private int successCount;
    //人脸个数
    private int face_num;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置屏幕常亮
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initStatusBar();
        setContentView(R.layout.activity_gather_face);
        if (null != this.getIntent().getExtras()) {
            face_user_no = this.getIntent().getExtras().getString("face_user_no", null);
        }
        //公共参数
        getOverallArguments();
        currentDownTime = Integer.valueOf(MyApplication.countDownTime);
        cameraInit();
        mUIHandler = new UIHandler();
        tvContent = (TextView) findViewById(R.id.tvContent);
        tvError = (TextView) findViewById(R.id.tvError);
        SurfaceView view = (SurfaceView) findViewById(R.id.mSurfaceView);
        tvCountDown = (TextView) findViewById(R.id.tvCountDown);
        tvCountDown.setText(countDownTime);
        view.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                mShower.open();
                mShower.setSurface(holder.getSurface());
                mShower.setOutputSize(width, height);
                mProcessor.start();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mProcessor.stop();
                mShower.close();
            }
        });
    }

    /**
     * 获取全局参数
     */
    private void getOverallArguments() {
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
        if (!TextUtils.isEmpty(SPUtils.getInstance(this).getData("times"))) {
            times = SPUtils.getInstance(this).getData("times");
        } else {
            times = MyApplication.times;
        }
    }

    /**
     * 初始化沉浸式状态栏
     */
    private void initStatusBar() {
        //设置是否沉浸式
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;
        int flag_translucent_status = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        //透明状态栏
        getWindow().setFlags(flag_translucent_status, flag_translucent_status);
    }


    @Override
    protected void onStart() {
        super.onStart();
        countDownTimer = new CountDownTimer(Integer.valueOf(countDownTime) * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentDownTime--;
                if (currentDownTime >= 0) {
                    tvCountDown.setText(currentDownTime + "");
                }
                if (!exportFlag) exportFlag = true;
            }

            @Override
            public void onFinish() {
                tvCountDown.setText("0");
                countDownTimeFinish();
            }
        };
    }

    /**
     * 倒计时结束
     */
    private void countDownTimeFinish() {
        if (!isSuccessGather) {
            //倒计时时间内没有成功存储3次样本代表采集失败，face_state设置为-1
            //同时提升采集失败相关展示 2秒后返回首页。
            new Thread() {
                @Override
                public void run() {
                    String sql = "update ug_house set face_state='-1' where face_state=0 and id=" + roomCode + ";";
                    MySqlUtil.execSQL(MySqlUtil.conn, sql);
                    mUIHandler.sendEmptyMessage(MSG_EVENT_GATHER_FAIL);
                }
            }.start();

        } else {
            new Thread() {
                @Override
                public void run() {
//                            成功存储3次样本，就不需要在采集了，这个时候需要把表中face_state修改为1，同时提升采集成功页面，2秒后返回首页。
                    String sql = "update ug_house set face_state='1',face_num='" + face_num + "' where (state = 1 or  face_state = 0) and id=" + roomCode + ";";
                    MySqlUtil.execSQL(MySqlUtil.conn, sql);
                }
            }.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        countDownTimer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        countDownTimer.cancel();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    // Called in Android UI's main thread
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessage(MessageEvent event) {
        if (event.getMessage().equals("close")) {
            String activityName = CommonUtils.getTopActivityName(this);
            if (!"com.ughouse.facegverify.WelComeActivity".equals(activityName)) {
                startActivity(new Intent(this, WelComeActivity.class));
            }
            finish();
        }
    }

    /**
     * 初始化摄像头
     */
    private void cameraInit() {
        mShower = new SurfaceShower();
        mProvider = new CameraProvider();
        mProcessor = new VideoSurfaceProcessor();
        mProcessor.setTextureProvider(mProvider);
        mProcessor.addObserver(mShower);
        mFb = new FrameBuffer();
        mShower.setOnDrawEndListener(new SurfaceShower.OnDrawEndListener() {
            @Override
            public void onDrawEnd(EGLSurface surface, RenderBean bean) {
                if (exportFlag && !isSuccessGather) {
                    if (mOutputFilter == null) {
                        mOutputFilter = new YuvOutputFilter(YuvOutputFilter.EXPORT_TYPE_NV21);
                        mOutputFilter.create();
                        mOutputFilter.sizeChanged(picX, picY);
                        mOutputFilter.setInputTextureSize(bean.sourceWidth, bean.sourceHeight);
                        tempBuffer = new byte[picX * picY * 3 / 2];
                    }

                    mOutputFilter.drawToTexture(bean.textureId);
                    mOutputFilter.getOutput(tempBuffer, 0, picX * picY * 3 / 2);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mBitmap != null) {
                                mBitmap.recycle();
                                mBitmap = null;
                            }
                            try {
                                mBitmap = CommonUtils.rawByteArray2RGBABitmap2(tempBuffer, picX, picY);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        src.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
                                    } catch (Exception e) {//捕获空指针
                                        e.printStackTrace();
                                    }
                                    byte[] data = new byte[mBitmap.getWidth() * mBitmap.getHeight() * 3 / 2];
                                    ImageConverter convert = new ImageConverter();
                                    convert.initial(mBitmap.getWidth(), mBitmap.getHeight(), ImageConverter.CP_PAF_NV21);
                                    if (convert.convert(mBitmap, data)) {
                                        Log.d(TAG, "convert ok!");
                                    }
                                    convert.destroy();

                                    AFD_FSDKEngine engine = new AFD_FSDKEngine();
                                    AFD_FSDKVersion version = new AFD_FSDKVersion();
                                    List<AFD_FSDKFace> result = new ArrayList<AFD_FSDKFace>();
                                    AFD_FSDKError err = engine.AFD_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.fd_key, AFD_FSDKEngine.AFD_OPF_0_HIGHER_EXT, 16, 5);
                                    Log.d(TAG, "AFD_FSDK_InitialFaceEngine = " + err.getCode());
                                    if (err.getCode() != AFD_FSDKError.MOK) {
                                        Message reg = Message.obtain();
                                        reg.what = MSG_CODE;
                                        reg.arg1 = MSG_EVENT_FD_ERROR;
                                        reg.arg2 = err.getCode();
                                        mUIHandler.sendMessage(reg);
                                    }
                                    err = engine.AFD_FSDK_GetVersion(version);
                                    Log.d(TAG, "AFD_FSDK_GetVersion =" + version.toString() + ", " + err.getCode());
                                    err = engine.AFD_FSDK_StillImageFaceDetection(data, mBitmap.getWidth(), mBitmap.getHeight(), AFD_FSDKEngine.CP_PAF_NV21, result);
                                    Log.d(TAG, "AFD_FSDK_StillImageFaceDetection =" + err.getCode() + "<" + result.size());
                                    if (!result.isEmpty()) {
                                        AFR_FSDKVersion version1 = new AFR_FSDKVersion();
                                        AFR_FSDKEngine engine1 = new AFR_FSDKEngine();
                                        AFR_FSDKFace result1 = new AFR_FSDKFace();
                                        AFR_FSDKError error1 = engine1.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
                                        Log.d("com.arcsoft", "AFR_FSDK_InitialEngine = " + error1.getCode());
                                        if (error1.getCode() != AFD_FSDKError.MOK) {
                                            Message reg = Message.obtain();
                                            reg.what = MSG_CODE;
                                            reg.arg1 = MSG_EVENT_FR_ERROR;
                                            reg.arg2 = error1.getCode();
                                            mUIHandler.sendMessage(reg);
                                        }
                                        error1 = engine1.AFR_FSDK_GetVersion(version1);
                                        Log.d("com.arcsoft", "FR=" + version.toString() + "," + error1.getCode()); //(210, 178 - 478, 446), degree = 1　780, 2208 - 1942, 3370
                                        error1 = engine1.AFR_FSDK_ExtractFRFeature(data, mBitmap.getWidth(), mBitmap.getHeight(), AFR_FSDKEngine.CP_PAF_NV21, new Rect(result.get(0).getRect()), result.get(0).getDegree(), result1);
                                        Log.d("com.arcsoft", "Face=" + result1.getFeatureData()[0] + "," + result1.getFeatureData()[1] + "," + result1.getFeatureData()[2] + "," + error1.getCode());
                                        if (error1.getCode() == error1.MOK) {
                                            mAFR_FSDKFace = result1.clone();
                                            int width = result.get(0).getRect().width();
                                            int height = result.get(0).getRect().height();
                                            Bitmap face_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                                            Canvas face_canvas = new Canvas(face_bitmap);
                                            face_canvas.drawBitmap(mBitmap, result.get(0).getRect(), new Rect(0, 0, width, height), null);
                                            Message reg = Message.obtain();
                                            reg.what = MSG_CODE;
                                            reg.arg1 = MSG_EVENT_REG;
                                            face_num = result.size();
                                            reg.obj = face_bitmap;
                                            mUIHandler.sendMessage(reg);
                                        } else {
                                            Message reg = Message.obtain();
                                            reg.what = MSG_CODE;
                                            reg.arg1 = MSG_EVENT_NO_FEATURE;
                                            mUIHandler.sendMessage(reg);
                                        }
                                        error1 = engine1.AFR_FSDK_UninitialEngine();
                                        Log.d("com.arcsoft", "AFR_FSDK_UninitialEngine : " + error1.getCode());
                                    } else {
                                        Message reg = Message.obtain();
                                        reg.what = MSG_CODE;
                                        reg.arg1 = MSG_EVENT_NO_FACE;
                                        mUIHandler.sendMessage(reg);
                                    }
                                    err = engine.AFD_FSDK_UninitialFaceEngine();
                                    Log.d(TAG, "AFD_FSDK_UninitialFaceEngine =" + err.getCode());
                                }
                            }.start();
                        }
                    });
                    exportFlag = false;
                }
            }
        });
    }

    class UIHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_CODE) {
                if (msg.arg1 == MSG_EVENT_REG) {
                    tvError.setVisibility(View.GONE);
                    tvContent.setVisibility(View.GONE);
                    if (face_user_no != null && !TextUtils.isEmpty(face_user_no)) {
                        //保存样本名为face_user_no+"-1"+"_"+guid(),face_user_no+"-2"+"_"+guid()等，保存到ug_face_data表中
                        ((MyApplication) GatherFaceActivity.this.getApplicationContext()).mFaceDB.addFace(face_user_no + "_" + CommonUtils.getUUID(), mAFR_FSDKFace);
                        successCount++;
                        if (successCount >= Integer.valueOf(times) && !isSuccessGather) {
                            isSuccessGather = true;
//                        成功存储3次样本，就不需要在采集了，这个时候需要把表中face_state修改为1，同时提升采集成功页面，2秒后返回首页。
                            new Thread() {
                                @Override
                                public void run() {
                                    String sql = "update ug_house set face_state='1',face_num='" + face_num + "' where (state = 1 or  face_state = 0) and id=" + roomCode + ";";
                                    MySqlUtil.execSQL(MySqlUtil.conn, sql);
                                    mUIHandler.sendEmptyMessage(MSG_EVENT_GATHER_SUCCESS);
                                }
                            }.start();
                        }
                    }
                } else if (msg.arg1 == MSG_EVENT_NO_FEATURE) {
                    LogUtils.e("人脸特征无法检测，请换一张图片");
                    tvError.setVisibility(View.VISIBLE);
//                    Toast.makeText(GatherFaceActivity.this, "人脸特征无法检测，请换一张图片", Toast.LENGTH_SHORT).show();
                } else if (msg.arg1 == MSG_EVENT_NO_FACE) {
                    tvError.setVisibility(View.VISIBLE);
                    LogUtils.e("没有检测到人脸，请换一张图片");
//                    Toast.makeText(GatherFaceActivity.this, "没有检测到人脸，请换一张图片", Toast.LENGTH_SHORT).show();
                } else if (msg.arg1 == MSG_EVENT_FD_ERROR) {
                    LogUtils.e("FD初始化失败，错误码：" + msg.arg2);
//                    Toast.makeText(GatherFaceActivity.this, "FD初始化失败，错误码：" + msg.arg2, Toast.LENGTH_SHORT).show();
                } else if (msg.arg1 == MSG_EVENT_FR_ERROR) {
                    LogUtils.e("FR初始化失败，错误码：" + msg.arg2);
//                    Toast.makeText(GatherFaceActivity.this, "FR初始化失败，错误码：" + msg.arg2, Toast.LENGTH_SHORT).show();
                }
            } else if (msg.what == MSG_EVENT_GATHER_SUCCESS) {
//                跳转集成功页面，2秒后返回首页。
                tvContent.setText(getResources().getString(R.string.gather_success));
                tvContent.setTextColor(Color.parseColor("#66ff00"));
                tvContent.setVisibility(View.VISIBLE);
                tvError.setVisibility(View.GONE);

            } else if (msg.what == MSG_EVENT_GATHER_FAIL) {
//                跳转集成功页面，2秒后返回首页。
                EventBus.getDefault().post(new MessageEvent("close"));
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            //返回按钮
            EventBus.getDefault().post(new MessageEvent("close"));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTime != null)
            countDownTimer.cancel();
        if (mUIHandler != null)
            mUIHandler.removeCallbacksAndMessages(null);
        isSuccessGather = false;
    }
}
