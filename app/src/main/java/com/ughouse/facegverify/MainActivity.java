package com.ughouse.facegverify;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ughouse.facegverify.activity.WelComeActivity;
import com.ughouse.facegverify.constant.FunctionType;
import com.ughouse.facegverify.service.FRService;
import com.ughouse.facegverify.service.MyJobService;
import com.ughouse.facegverify.util.SPUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    @BindView(R.id.etIp)
    EditText etIp;
    @BindView(R.id.etRoomCode)
    EditText etRoomCode;
    @BindView(R.id.etTime)
    EditText etTime;
    @BindView(R.id.etCount)
    EditText etCount;
    @BindView(R.id.tvCollectContrast)
    TextView tvCollectContrast;
    @BindView(R.id.rgFunction)
    RadioGroup rgFunction;
    //功能类型
    private int functionType = FunctionType.GATHER_CONTRAST_TYPE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置屏幕常亮
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        rgFunction.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rbGather:
                functionType = FunctionType.GATHER_TYPE;
                break;
            case R.id.rbContrast:
                functionType = FunctionType.CONTRAST_TYPE;
                break;
            case R.id.rbGatherContrast:
                functionType = FunctionType.GATHER_CONTRAST_TYPE;
                break;
        }
    }

    @OnClick({R.id.tvCollectContrast})
    public void onClick(View view) {
        if (view == tvCollectContrast) {//采集对比
            if (etIp.getText() == null || TextUtils.isEmpty(etIp.getText().toString().trim())) {
                Toast.makeText(this, "请输入主机 ip", Toast.LENGTH_LONG).show();
            }
            if (etRoomCode.getText() == null || TextUtils.isEmpty(etRoomCode.getText().toString().trim())) {
                Toast.makeText(this, "请输入当前们序号", Toast.LENGTH_LONG).show();
            }
            if (etTime.getText() == null || TextUtils.isEmpty(etTime.getText().toString().trim())) {
                Toast.makeText(this, "请输入采集最大时间", Toast.LENGTH_LONG).show();
            }
            if (etCount.getText() == null || TextUtils.isEmpty(etCount.getText().toString().trim())) {
                Toast.makeText(this, "请输入采集对比次数", Toast.LENGTH_LONG).show();
            }
            MyApplication.ip = etIp.getText().toString().trim();
            MyApplication.roomCode = etRoomCode.getText().toString();
            MyApplication.countDownTime = etTime.getText().toString();
            MyApplication.functionType = functionType;
            MyApplication.times = etCount.getText().toString();
            SPUtils.getInstance(this).saveData("ip", MyApplication.ip);
            SPUtils.getInstance(this).saveData("roomCode", MyApplication.roomCode);
            SPUtils.getInstance(this).saveData("countDownTime", MyApplication.countDownTime);
            SPUtils.getInstance(this).saveData("functionType", MyApplication.functionType + "");
            SPUtils.getInstance(this).saveData("times", MyApplication.times);
            //欢迎页
            Intent intent = new Intent(this, WelComeActivity.class);
            startActivity(intent);
            finish();
        }
    }

}
