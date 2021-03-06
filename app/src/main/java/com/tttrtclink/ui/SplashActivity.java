package com.tttrtclink.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tttrtclink.LocalConfig;
import com.tttrtclink.LocalConstans;
import com.tttrtclink.R;
import com.tttrtclink.bean.JniObjs;
import com.tttrtclink.callback.MyTTTRtcEngineEventHandler;
import com.tttrtclink.utils.MySpUtils;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.wushuangtech.wstechapi.model.PublisherConfiguration;
import com.yanzhenjie.permission.AndPermission;

import java.util.Random;

public class SplashActivity extends BaseActivity {

    public static boolean mIsLoging;
    private MyLocalBroadcastReceiver mLocalBroadcast;
    private EditText mRoomIDET;
    private ProgressDialog mDialog;
    private TTTRtcEngine mTTTEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
        //1.首先需要申请 SDK 运行的必要权限。
        AndPermission.with(this)
                .permission(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA)
                .start();
        //2.创建 SDK 的回调接收类对象，接收 SDK 回调的所有信令。
        LocalConfig.mMyTTTRtcEngineEventHandler = new MyTTTRtcEngineEventHandler(getApplicationContext());
        //3.创建 SDK 的实例对象，用于执行 SDK 各项功能。此函数仅需要调用一次即可。
        mTTTEngine = TTTRtcEngine.create(getApplicationContext(), <这里填APPID>, false,
                LocalConfig.mMyTTTRtcEngineEventHandler);
        if (mTTTEngine == null) {
            System.exit(0);
            return;
        }
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mLocalBroadcast);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() {
        mRoomIDET = findViewById(R.id.room_id);
        //获取 SDK 的版本信息并显示
        TextView mVersion = findViewById(R.id.version);
        String string = getResources().getString(R.string.ttt_splash_versioninfo);
        String result = String.format(string, mTTTEngine.getSdkVersion());
        mVersion.setText(result);
        //注册广播，接收 SDK 回调的信令。
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);
        //创建进度对话框
        mDialog = new ProgressDialog(this);
        mDialog.setTitle("");
        mDialog.setCancelable(false);
        mDialog.setMessage(getString(R.string.ttt_loading_channel));
    }

    private void initSDK() {
        //4.设置频道模式为直播模式。
        mTTTEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        //5.启用视频模式
        mTTTEngine.enableVideo();
        //6.设置角色为主播
        mTTTEngine.setClientRole(Constants.CLIENT_ROLE_ANCHOR);
    }

    public void onClickEnterButton(View v) {
        String mRoomName = mRoomIDET.getText().toString().trim();
        if (TextUtils.isEmpty(mRoomName)) {
            Toast.makeText(this, R.string.hint_channel_name_limit, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.getTrimmedLength(mRoomName) > 18) {
            Toast.makeText(this, R.string.hint_channel_name_limit, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mIsLoging) {
            return;
        }
        mIsLoging = true;
        // 随机生成用户ID
        Random mRandom = new Random();
        final long mUserId = mRandom.nextInt(999999);
        LocalConfig.mUid = mUserId;
        LocalConfig.mRoomID = mRoomName;
        // 保存配置
        MySpUtils.setParam(this, "RoomID", mRoomName);
        // 配置 SDK
        initSDK();
        // 配置推流地址，此推流地址仅供demo测试使用！
        PublisherConfiguration mPublisherConfiguration = new PublisherConfiguration();
        mPublisherConfiguration.setPushUrl("rtmp://push.3ttest.cn/sdk2/" + mRoomName);
        mTTTEngine.configPublisher(mPublisherConfiguration);
        // 开始加入频道
        mTTTEngine.joinChannel("", mRoomName, mUserId);
        mDialog.setMessage(getString(R.string.ttt_loading_channel));
        mDialog.show();
    }

    private class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyTTTRtcEngineEventHandler.TAG.equals(action)) {
                JniObjs mJniObjs = intent.getParcelableExtra(MyTTTRtcEngineEventHandler.MSG_TAG);
                switch (mJniObjs.mJniType) {
                    case LocalConstans.CALL_BACK_ON_ENTER_ROOM: //接收进房间成功的回调信令
                        //进行界面跳转，进入主界面
                        Intent activityIntent = new Intent();
                        activityIntent.setClass(SplashActivity.this, MainActivity.class);
                        startActivity(activityIntent);
                        if (mDialog != null) {
                            mDialog.dismiss();
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_ERROR: //接收进房间失败的回调信令，具体的错误信息，请看 Toast 的提示信息
                        mIsLoging = false;
                        if (mDialog != null) {
                            mDialog.dismiss();
                        }
                        int errorType = mJniObjs.mErrorType;
                        if (errorType == Constants.ERROR_ENTER_ROOM_INVALIDCHANNELNAME) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_format), Toast.LENGTH_SHORT).show();
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_TIMEOUT) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_timeout), Toast.LENGTH_SHORT).show();
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_VERIFY_FAILED) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_token_invaild), Toast.LENGTH_SHORT).show();
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_BAD_VERSION) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_version), Toast.LENGTH_SHORT).show();
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_CONNECT_FAILED) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_unconnect), Toast.LENGTH_SHORT).show();
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_NOEXIST) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_room_no_exist), Toast.LENGTH_SHORT).show();
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_SERVER_VERIFY_FAILED) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_verification_failed), Toast.LENGTH_SHORT).show();
                        } else if (errorType == Constants.ERROR_ENTER_ROOM_UNKNOW) {
                            Toast.makeText(mContext, mContext.getResources().getString(R.string.ttt_error_enterchannel_unknow), Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        }
    }
}
