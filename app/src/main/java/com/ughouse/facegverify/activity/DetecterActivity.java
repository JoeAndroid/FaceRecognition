package com.ughouse.facegverify.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.arcsoft.ageestimation.ASAE_FSDKAge;
import com.arcsoft.ageestimation.ASAE_FSDKEngine;
import com.arcsoft.ageestimation.ASAE_FSDKError;
import com.arcsoft.ageestimation.ASAE_FSDKFace;
import com.arcsoft.ageestimation.ASAE_FSDKVersion;
import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKMatching;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.arcsoft.facetracking.AFT_FSDKEngine;
import com.arcsoft.facetracking.AFT_FSDKError;
import com.arcsoft.facetracking.AFT_FSDKFace;
import com.arcsoft.facetracking.AFT_FSDKVersion;
import com.arcsoft.genderestimation.ASGE_FSDKEngine;
import com.arcsoft.genderestimation.ASGE_FSDKError;
import com.arcsoft.genderestimation.ASGE_FSDKFace;
import com.arcsoft.genderestimation.ASGE_FSDKGender;
import com.arcsoft.genderestimation.ASGE_FSDKVersion;
import com.guo.android_extend.java.AbsLoop;
import com.guo.android_extend.java.ExtByteArrayOutputStream;
import com.guo.android_extend.tools.CameraHelper;
import com.guo.android_extend.widget.CameraFrameData;
import com.guo.android_extend.widget.CameraGLSurfaceView;
import com.guo.android_extend.widget.CameraSurfaceView;
import com.ughouse.facegverify.MessageEvent;
import com.ughouse.facegverify.MyApplication;
import com.ughouse.facegverify.R;
import com.ughouse.facegverify.constant.FunctionType;
import com.ughouse.facegverify.db.FaceDB;
import com.ughouse.facegverify.util.CommonUtils;
import com.ughouse.facegverify.util.LogUtils;
import com.ughouse.facegverify.util.MySqlUtil;
import com.ughouse.facegverify.util.SPUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 人脸识别找出匹配正确的人脸
 */
public class DetecterActivity extends AppCompatActivity implements CameraSurfaceView.OnCameraListener, View.OnTouchListener, Camera.AutoFocusCallback {

    private final String TAG = this.getClass().getSimpleName();

    private int mWidth, mHeight, mFormat;
    private CameraSurfaceView mSurfaceView;
    private CameraGLSurfaceView mGLSurfaceView;
    private TextView tvCountDown;
    private TextView tvContent, tvError;
    private Camera mCamera;

    AFT_FSDKVersion version = new AFT_FSDKVersion();
    AFT_FSDKEngine engine = new AFT_FSDKEngine();
    ASAE_FSDKVersion mAgeVersion = new ASAE_FSDKVersion();
    ASAE_FSDKEngine mAgeEngine = new ASAE_FSDKEngine();
    ASGE_FSDKVersion mGenderVersion = new ASGE_FSDKVersion();
    ASGE_FSDKEngine mGenderEngine = new ASGE_FSDKEngine();
    List<AFT_FSDKFace> result = new ArrayList<>();
    List<ASAE_FSDKAge> ages = new ArrayList<>();
    List<ASGE_FSDKGender> genders = new ArrayList<>();

    int mCameraID;
    int mCameraRotate;
    boolean mCameraMirror;
    byte[] mImageNV21 = null;
    FRAbsLoop mFRAbsLoop = null;
    AFT_FSDKFace mAFT_FSDKFace = null;
    Handler mHandler;

    //门牌号
    private String roomCode;
    private String times;//对比次数
    private String countDownTime;
    private int currentDownTime;
    private CountDownTimer countDownTimer;
    private Timer queryTimer;//每秒查询一次是否有三个人脸名称
    private MyTimerTask timerTask;
    //人脸名称list中重复出现最大的次数
    private int max_cnt;
    //list 中重复出现次数最大人脸名称
    private String max_name;
    //存储大于0.75的样本名称
    private List<String> nameList = new ArrayList<>();

    class FRAbsLoop extends AbsLoop {

        AFR_FSDKVersion version = new AFR_FSDKVersion();
        AFR_FSDKEngine engine = new AFR_FSDKEngine();
        AFR_FSDKFace result = new AFR_FSDKFace();
        List<FaceDB.FaceRegist> mResgist = ((MyApplication) DetecterActivity.this.getApplicationContext()).mFaceDB.mRegister;
        List<ASAE_FSDKFace> face1 = new ArrayList<>();
        List<ASGE_FSDKFace> face2 = new ArrayList<>();

        @Override
        public void setup() {
            AFR_FSDKError error = engine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
            Log.d(TAG, "AFR_FSDK_InitialEngine = " + error.getCode());
            error = engine.AFR_FSDK_GetVersion(version);
            Log.d(TAG, "FR=" + version.toString() + "," + error.getCode()); //(210, 178 - 478, 446), degree = 1　780, 2208 - 1942, 3370
        }

        @Override
        public void loop() {
            if (mImageNV21 != null) {
                long time = System.currentTimeMillis();
                AFR_FSDKError error = engine.AFR_FSDK_ExtractFRFeature(mImageNV21, mWidth, mHeight, AFR_FSDKEngine.CP_PAF_NV21, mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree(), result);
                Log.d(TAG, "AFR_FSDK_ExtractFRFeature cost :" + (System.currentTimeMillis() - time) + "ms");
                Log.d(TAG, "Face=" + result.getFeatureData()[0] + "," + result.getFeatureData()[1] + "," + result.getFeatureData()[2] + "," + error.getCode());
                AFR_FSDKMatching score = new AFR_FSDKMatching();
                float max = 0.0f;
                String name = null;
                for (FaceDB.FaceRegist fr : mResgist) {
                    for (AFR_FSDKFace face : fr.mFaceList) {
                        error = engine.AFR_FSDK_FacePairMatching(result, face, score);
                        Log.d(TAG, "Score:" + score.getScore() + ", AFR_FSDK_FacePairMatching=" + error.getCode());
                        if (max < score.getScore()) {
                            max = score.getScore();
                            name = fr.mName;
                        }
                    }
                }

                //age & gender
                face1.clear();
                face2.clear();
                face1.add(new ASAE_FSDKFace(mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree()));
                face2.add(new ASGE_FSDKFace(mAFT_FSDKFace.getRect(), mAFT_FSDKFace.getDegree()));
                ASAE_FSDKError error1 = mAgeEngine.ASAE_FSDK_AgeEstimation_Image(mImageNV21, mWidth, mHeight, AFT_FSDKEngine.CP_PAF_NV21, face1, ages);
                ASGE_FSDKError error2 = mGenderEngine.ASGE_FSDK_GenderEstimation_Image(mImageNV21, mWidth, mHeight, AFT_FSDKEngine.CP_PAF_NV21, face2, genders);
                Log.d(TAG, "ASAE_FSDK_AgeEstimation_Image:" + error1.getCode() + ",ASGE_FSDK_GenderEstimation_Image:" + error2.getCode());
                Log.d(TAG, "age:" + ages.get(0).getAge() + ",gender:" + genders.get(0).getGender());

                //crop
                byte[] data = mImageNV21;
                YuvImage yuv = new YuvImage(data, ImageFormat.NV21, mWidth, mHeight, null);
                ExtByteArrayOutputStream ops = new ExtByteArrayOutputStream();
                yuv.compressToJpeg(mAFT_FSDKFace.getRect(), 80, ops);
                final Bitmap bmp = BitmapFactory.decodeByteArray(ops.getByteArray(), 0, ops.getByteArray().length);
                try {
                    ops.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (max > 0.75f) {
                    //fr success.
                    String nameface = name.split("_")[0];
                    nameList.add(nameface);
                }
                mImageNV21 = null;
            }

        }

        @Override
        public void over() {
            AFR_FSDKError error = engine.AFR_FSDK_UninitialEngine();
            Log.d(TAG, "AFR_FSDK_UninitialEngine : " + error.getCode());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        // 设置屏幕常亮
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//去掉信息栏
        mCameraID = getIntent().getIntExtra("Camera", 0) == 0 ? Camera.CameraInfo.CAMERA_FACING_BACK : Camera.CameraInfo.CAMERA_FACING_FRONT;
        mCameraRotate = getIntent().getIntExtra("Camera", 0) == 0 ? 90 : 270;
        mCameraMirror = getIntent().getIntExtra("Camera", 0) == 0 ? false : true;
        mWidth = 1280;
        mHeight = 960;
        mFormat = ImageFormat.NV21;
        mHandler = new Handler();
        //公共参数
        getOverallArguments();
        currentDownTime = Integer.valueOf(countDownTime);

        setContentView(R.layout.activity_detecter);
        mGLSurfaceView = (CameraGLSurfaceView) findViewById(R.id.glsurfaceView);
        mGLSurfaceView.setOnTouchListener(this);
        mSurfaceView = (CameraSurfaceView) findViewById(R.id.surfaceView);
        mSurfaceView.setOnCameraListener(this);
        mSurfaceView.setupGLSurafceView(mGLSurfaceView, true, mCameraMirror, mCameraRotate);
        mSurfaceView.debug_print_fps(true, false);

        //snap
        tvContent = (TextView) findViewById(R.id.tvContent);
        tvError = (TextView) findViewById(R.id.tvError);
        tvCountDown = (TextView) findViewById(R.id.tvCountDown);
        tvCountDown.setText(countDownTime);

        AFT_FSDKError err = engine.AFT_FSDK_InitialFaceEngine(FaceDB.appid, FaceDB.ft_key, AFT_FSDKEngine.AFT_OPF_0_HIGHER_EXT, 16, 5);
        Log.d(TAG, "AFT_FSDK_InitialFaceEngine =" + err.getCode());
        err = engine.AFT_FSDK_GetVersion(version);
        Log.d(TAG, "AFT_FSDK_GetVersion:" + version.toString() + "," + err.getCode());

        ASAE_FSDKError error = mAgeEngine.ASAE_FSDK_InitAgeEngine(FaceDB.appid, FaceDB.age_key);
        Log.d(TAG, "ASAE_FSDK_InitAgeEngine =" + error.getCode());
        error = mAgeEngine.ASAE_FSDK_GetVersion(mAgeVersion);
        Log.d(TAG, "ASAE_FSDK_GetVersion:" + mAgeVersion.toString() + "," + error.getCode());

        ASGE_FSDKError error1 = mGenderEngine.ASGE_FSDK_InitgGenderEngine(FaceDB.appid, FaceDB.gender_key);
        Log.d(TAG, "ASGE_FSDK_InitgGenderEngine =" + error1.getCode());
        error1 = mGenderEngine.ASGE_FSDK_GetVersion(mGenderVersion);
        Log.d(TAG, "ASGE_FSDK_GetVersion:" + mGenderVersion.toString() + "," + error1.getCode());

        mFRAbsLoop = new FRAbsLoop();
        mFRAbsLoop.start();
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

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        countDownTimer = new CountDownTimer(Integer.valueOf(countDownTime) * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentDownTime--;
                if (currentDownTime >= 0) {
                    tvCountDown.setText(currentDownTime + "");
                }
                queryListRepeat();
            }

            @Override
            public void onFinish() {
                tvCountDown.setText("0");
                countDownTimeFinish();
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        countDownTimer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        countDownTimer.cancel();
    }

    //一秒查找一次重复出现并且大于三的人脸样本名称
    private void queryListRepeat() {
        if (queryTimer != null) {
            if (timerTask != null) {
                timerTask.cancel();
            }
            timerTask = new MyTimerTask();
            queryTimer.schedule(timerTask, 0, 1000);
        } else {
            queryTimer = new Timer();
            timerTask = new MyTimerTask();
            queryTimer.schedule(timerTask, 0, 1000);
        }
    }

    //倒计时完成
    private void countDownTimeFinish() {
//     如果时间超过30秒还没采集3次样本就代表采集失败，face_state设置为-1，同时提升采集失败相关展示 2秒后返回首页。
        new Thread() {
            @Override
            public void run() {
                if (max_cnt >= Integer.valueOf(times)) {
                    String sql = "update ug_house set face_user_no='" + max_name + "', face_state='2' where state = 2 and face_user_no= '' and id=" + MyApplication.roomCode + ";";
                    MySqlUtil.execSQL(MySqlUtil.conn, sql);
                } else {
                    String sql = "update ug_house set face_state='-1' where face_state=0 and id=" + roomCode + ";";
                    MySqlUtil.execSQL(MySqlUtil.conn, sql);

                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countDownTimer.cancel();
                        if (queryTimer != null) {
                            queryTimer.cancel();
                        }
                        EventBus.getDefault().post(new MessageEvent("close"));
                    }
                });
            }
        }.start();
    }

    @Override
    public Camera setupCamera() {
        // TODO Auto-generated method stub
        mCamera = Camera.open(mCameraID);
        try {
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(mWidth, mHeight);
            parameters.setPreviewFormat(mFormat);

            for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
                Log.d(TAG, "SIZE:" + size.width + "x" + size.height);
            }
            for (Integer format : parameters.getSupportedPreviewFormats()) {
                Log.d(TAG, "FORMAT:" + format);
            }

            List<int[]> fps = parameters.getSupportedPreviewFpsRange();
            for (int[] count : fps) {
                Log.d(TAG, "T:");
                for (int data : count) {
                    Log.d(TAG, "V=" + data);
                }
            }
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mCamera != null) {
            mWidth = mCamera.getParameters().getPreviewSize().width;
            mHeight = mCamera.getParameters().getPreviewSize().height;
        }
        return mCamera;
    }

    @Override
    public void setupChanged(int format, int width, int height) {

    }

    @Override
    public boolean startPreviewLater() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Object onPreview(byte[] data, int width, int height, int format, long timestamp) {
        AFT_FSDKError err = engine.AFT_FSDK_FaceFeatureDetect(data, width, height, AFT_FSDKEngine.CP_PAF_NV21, result);
        Log.d(TAG, "AFT_FSDK_FaceFeatureDetect =" + err.getCode());
        Log.d(TAG, "Face=" + result.size());
        for (AFT_FSDKFace face : result) {
            Log.d(TAG, "Face:" + face.toString());
        }
        if (mImageNV21 == null) {
            if (!result.isEmpty()) {
                mAFT_FSDKFace = result.get(0).clone();
                mImageNV21 = data.clone();
            }
        }
        //copy rects
        Rect[] rects = new Rect[result.size()];
        for (int i = 0; i < result.size(); i++) {
            rects[i] = new Rect(result.get(i).getRect());
        }
        //clear result.
        result.clear();
        //return the rects for render.
        return rects;
    }

    @Override
    public void onBeforeRender(CameraFrameData data) {

    }

    @Override
    public void onAfterRender(CameraFrameData data) {
//        mGLSurfaceView.getGLES2Render().draw_rect((Rect[]) data.getParams(), Color.GREEN, 2);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        CameraHelper.touchFocus(mCamera, event, v, this);
        return false;
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
            Log.d(TAG, "Camera Focus SUCCESS!");
        }
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

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    private class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            getSampMaxCout(nameList);
            if (max_cnt >= Integer.valueOf(times)) {
//                    把对比阈值大于0 .75 的样本名称寸到数组里面，还要有个线程一直在获取数组里面的名字出现次数最多并且大于3次的人脸样本名字保存到face_user_no里面，
//                    这个时候把face_state修改为2，同时提升采集成功页面，2 秒后返回首页，state = 2 and face_user_no= ''
                String sql = "update ug_house set face_user_no='" + max_name + "', face_state='2' where state = 2 and face_user_no= '' and id=" + roomCode + ";";
                MySqlUtil.execSQL(MySqlUtil.conn, sql);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvContent.setVisibility(View.VISIBLE);
                        tvContent.setTextColor(Color.parseColor("#66ff00"));
                    }
                });
            }
        }
    }


    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if (queryTimer != null) {
            if (timerTask != null) {
                timerTask.cancel();
            }
        }
        if (countDownTime != null)
            countDownTimer.cancel();
        mFRAbsLoop.shutdown();
        AFT_FSDKError err = engine.AFT_FSDK_UninitialFaceEngine();
        Log.d(TAG, "AFT_FSDK_UninitialFaceEngine =" + err.getCode());

        ASAE_FSDKError err1 = mAgeEngine.ASAE_FSDK_UninitAgeEngine();
        Log.d(TAG, "ASAE_FSDK_UninitAgeEngine =" + err1.getCode());

        ASGE_FSDKError err2 = mGenderEngine.ASGE_FSDK_UninitGenderEngine();
        Log.d(TAG, "ASGE_FSDK_UninitGenderEngine =" + err2.getCode());
        mHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 查找出出现次数最多的元素，并且获得出现次数
     *
     * @param list
     */
    private void getSampMaxCout(List<String> list) {
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
        this.max_cnt = max_cnt;
        this.max_name = max_str;
        LogUtils.e("人脸名称 " + max_str + " 出现的最大次数：" + max_cnt);
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

}
