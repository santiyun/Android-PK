package com.tttrtclink.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tttrtclink.Helper.RemoteWindowManager;
import com.tttrtclink.LocalConfig;
import com.tttrtclink.LocalConstans;
import com.tttrtclink.R;
import com.tttrtclink.bean.JniObjs;
import com.tttrtclink.bean.UserInfo;
import com.tttrtclink.callback.MyTTTRtcEngineEventHandler;
import com.tttrtclink.dialog.ExitRoomDialog;
import com.wushuangtech.expansion.bean.VideoCompositingLayout;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.wushuangtech.wstechapi.model.VideoCanvas;

public class MainActivity extends BaseActivity {

    //音量的最大值，范围是0~9
    public static final int VOLUME_MAX_NUM = 9;

    public ViewGroup mFullScreenShowView;
    public TextView mAudioSpeedShow;
    public EditText mRoomET;
    public TextView mVideoSpeedShow;
    public ImageView mAudioChannel;
    private AlertDialog.Builder mErrorExitDialog;
    private ExitRoomDialog mExitRoomDialog;
    private MyLocalBroadcastReceiver mLocalBroadcast;
    private RemoteWindowManager mRemoteWindowManager;
    private boolean mIsHeadset;
    private boolean mIsMute;
    private TTTRtcEngine mTTTEngine;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_videochat);
        mRemoteWindowManager = new RemoteWindowManager(this);
        mTTTEngine = TTTRtcEngine.getInstance();
        initView();
        initData();
        SplashActivity.mIsLoging = false;
    }

    public void onLinkButtonClick(View v) {
        String roomID = mRoomET.getText().toString().trim();
        if (TextUtils.isEmpty(roomID)) {
            Toast.makeText(MainActivity.this, R.string.ttt_error_checkchannel_empty, Toast.LENGTH_LONG).show();
            return;
        }

        if (roomID.equals(LocalConfig.mRoomID)) {
            Toast.makeText(MainActivity.this, R.string.ttt_error_checkchannel_owner, Toast.LENGTH_LONG).show();
            return;
        }

        try {
            LocalConfig.mLinkRoomID = Long.parseLong(roomID);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, R.string.ttt_error_checkchannel_format, Toast.LENGTH_LONG).show();
            return;
        }
        mTTTEngine.subscribeOtherChannel(LocalConfig.mLinkRoomID);
    }

    @Override
    public void onBackPressed() {
        mExitRoomDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalConfig.mUserEnterOrder.clear();
        try {
            unregisterReceiver(mLocalBroadcast);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    private void initView() {
        mRoomET = findViewById(R.id.remote_room_id);
        mAudioSpeedShow = findViewById(R.id.main_btn_audioup);
        mVideoSpeedShow = findViewById(R.id.main_btn_videoup);
        mFullScreenShowView = findViewById(R.id.main_background);
        //显示频道ID
        String channel = getResources().getString(R.string.ttt_com_channel_prefix);
        String channelName = channel + LocalConfig.mRoomID;
        ((TextView) findViewById(R.id.local_room_id)).setText(channelName);
        //显示自己的用户ID
        String uidPrefix = getResources().getString(R.string.ttt_com_uid_prefix);
        String uidName = uidPrefix + LocalConfig.mUid;
        ((TextView) findViewById(R.id.local_uid_id)).setText(uidName);
        findViewById(R.id.main_btn_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExitRoomDialog.show();
            }
        });
        //本地摄像头前后置切换操作，注意此接口的状态是全局的，即从前置切换为后置，退出频道再加入，依然是后置摄像头
        View mReversalCamera = findViewById(R.id.main_btn_camera);
        mReversalCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //本地摄像头前后置切换接口
                mTTTEngine.switchCamera();
            }
        });
        //本地静音/不静音的切换操作，注意此接口的状态也是全局的。
        mAudioChannel = findViewById(R.id.main_btn_audio_channel);
        mAudioChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsMute) {
                    if (mIsHeadset) {
                        mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
                    } else {
                        mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_selector);
                    }
                    //本地取消静音，重新发送音频流
                    mTTTEngine.muteLocalAudioStream(false);
                    mIsMute = false;
                } else {
                    if (mIsHeadset) {
                        mAudioChannel.setImageResource(R.drawable.mainly_btn_muted_headset_selector);
                    } else {
                        mAudioChannel.setImageResource(R.drawable.mainly_btn_mute_speaker_selector);
                    }
                    //本地静音，禁止发送音频流
                    mTTTEngine.muteLocalAudioStream(true);
                    mIsMute = true;
                }
            }
        });
    }

    private void initData() {
        //启用本地和远端用户的音量上报
        mTTTEngine.enableAudioVolumeIndication(300, 3);
        //创建本地视频显示控件 SurfaceView
        SurfaceView localSurfaceView = mTTTEngine.CreateRendererView(mContext);
        localSurfaceView.setZOrderMediaOverlay(false);
        //配置本地视频显示控件，用户ID传0即可、显示模式使用 RENDER_MODE_HIDDEN(裁剪模式)、传入 SurfaceView，最后还需要传入 Activity 的屏幕方向。
        mTTTEngine.setupLocalVideo(new VideoCanvas(0, Constants.RENDER_MODE_HIDDEN, localSurfaceView), getRequestedOrientation());
        mFullScreenShowView.addView(localSurfaceView, 0);
        //配置完毕后，打开相机预览
        mTTTEngine.startPreview();
        //注册回调函数，接收 SDK 发的广播
        mLocalBroadcast = new MyLocalBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MyTTTRtcEngineEventHandler.TAG);
        registerReceiver(mLocalBroadcast, filter);
        LocalConfig.mMyTTTRtcEngineEventHandler.setIsSaveCallBack(false);
        //设置本地视频分辨率为 360P，可选操作，默认的视频质量等级就是 360P
        mTTTEngine.setVideoProfile(Constants.TTTRTC_VIDEOPROFILE_360P, false);
        //创建频道接收 SDK 的异常错误信息信令，弹出提示对话框并退出
        if (mErrorExitDialog == null) {
            //添加确定按钮
            mErrorExitDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.ttt_dialog_exception_title)//设置对话框标题
                    .setCancelable(false)
                    .setPositiveButton(R.string.ttt_comfirm, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            exitRoom();
                        }
                    });
        }
        //创建频道退出按钮的提示对话框
        mExitRoomDialog = new ExitRoomDialog(mContext, R.style.NoBackGroundDialog);
        mExitRoomDialog.setCanceledOnTouchOutside(false);
        mExitRoomDialog.mConfirmBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitRoom();
                mExitRoomDialog.dismiss();
            }
        });

        mExitRoomDialog.mDenyBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExitRoomDialog.dismiss();
            }
        });
    }

    public void exitRoom() {
        //退出房间
        mTTTEngine.leaveChannel();
        //重置全局状态接口
        mTTTEngine.muteLocalAudioStream(false);
        finish();
    }

    public void setTextViewContent(TextView textView, int resourceID, String value) {
        String string = getResources().getString(resourceID);
        String result = String.format(string, value);
        textView.setText(result);
    }

    /**
     * Description: 显示因错误的回调而退出的对话框
     *
     * @param message the message 错误的原因
     */
    public void showErrorExitDialog(String message) {
        if (!TextUtils.isEmpty(message)) {
            mErrorExitDialog.setMessage(getString(R.string.ttt_dialog_exception_message) + ": " + message);//设置显示的内容
            mErrorExitDialog.show();
        }
    }

    private class MyLocalBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MyTTTRtcEngineEventHandler.TAG.equals(action)) {
                JniObjs mJniObjs = intent.getParcelableExtra(MyTTTRtcEngineEventHandler.MSG_TAG);
                switch (mJniObjs.mJniType) {
                    case LocalConstans.CALL_BACK_ON_USER_KICK: //接收 SDK 运行时异常退房间的信令
                        int errorType = mJniObjs.mErrorType;
                        String message = "";
                        if (errorType == Constants.ERROR_KICK_BY_HOST) {
                            message = getResources().getString(R.string.ttt_error_exit_kicked);
                        } else if (errorType == Constants.ERROR_KICK_BY_PUSHRTMPFAILED) {
                            message = getResources().getString(R.string.ttt_error_exit_push_rtmp_failed);
                        } else if (errorType == Constants.ERROR_KICK_BY_SERVEROVERLOAD) {
                            message = getResources().getString(R.string.ttt_error_exit_server_overload);
                        } else if (errorType == Constants.ERROR_KICK_BY_MASTER_EXIT) {
                            message = getResources().getString(R.string.ttt_error_exit_anchor_exited);
                        } else if (errorType == Constants.ERROR_KICK_BY_RELOGIN) {
                            message = getResources().getString(R.string.ttt_error_exit_relogin);
                        } else if (errorType == Constants.ERROR_KICK_BY_NEWCHAIRENTER) {
                            message = getResources().getString(R.string.ttt_error_exit_other_anchor_enter);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOAUDIODATA) {
                            message = getResources().getString(R.string.ttt_error_exit_noaudio_upload);
                        } else if (errorType == Constants.ERROR_KICK_BY_NOVIDEODATA) {
                            message = getResources().getString(R.string.ttt_error_exit_novideo_upload);
                        } else if (errorType == Constants.ERROR_TOKEN_EXPIRED) {
                            message = getResources().getString(R.string.ttt_error_exit_token_expired);
                        }
                        showErrorExitDialog(message);
                        break;
                    case LocalConstans.CALL_BACK_ON_CONNECTLOST: //接收 SDK 断开网络的信令
                        String connectLostMsg = getResources().getString(R.string.ttt_error_network_disconnected);
                        showErrorExitDialog("退出原因: " + connectLostMsg);
                        break;
                    case LocalConstans.CALL_BACK_ON_USER_JOIN: //接收其他用户加入当前频道的信令
                        //创建用户对象
                        UserInfo joinUser = new UserInfo(String.valueOf(LocalConfig.mRoomID), String.valueOf(mJniObjs.mUid), String.valueOf(mJniObjs.mDevID));
                        //显示视频窗口
                        mRemoteWindowManager.showWindow(joinUser);
                        //拿到构建的 sei 信息对象
                        VideoCompositingLayout layout = mRemoteWindowManager.getVideoCompositingLayout();
                        //调用接口，将 sei 信息发送给 SDK，把该主播添加到 CDN 的混屏视频中。
                        mTTTEngine.setVideoCompositingLayout(layout);
                        break;
                    case LocalConstans.CALL_BACK_ON_AUDIO_VOLUME_INDICATION: //接收自己和其他用户的音量大小值
                        if (mJniObjs.mUid == LocalConfig.mUid) {
                            if (mIsMute) {
                                return;
                            }
                            int volumeLevel = mJniObjs.mAudioLevel;
                            if (mIsHeadset) {
                                if (volumeLevel >= 0 && volumeLevel <= 3) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
                                } else if (volumeLevel > 3 && volumeLevel <= 6) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_middle_selector);
                                } else if (volumeLevel > 6 && volumeLevel <= VOLUME_MAX_NUM) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_big_selector);
                                }
                            } else {
                                if (volumeLevel >= 0 && volumeLevel <= 3) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_selector);
                                } else if (volumeLevel > 3 && volumeLevel <= 6) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_middle_selector);
                                } else if (volumeLevel > 6 && volumeLevel <= VOLUME_MAX_NUM) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_big_selector);
                                }
                            }
                        } else {
                            UserInfo userInfo = new UserInfo();
                            userInfo.userID = String.valueOf(mJniObjs.mUid);
                            mRemoteWindowManager.updateVolume(userInfo, mJniObjs.mAudioLevel);
                        }
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_VIDEO_STATE: // 接收 SDK 远端视频下行相关信息，如码率
                        UserInfo videoInfo = new UserInfo();
                        videoInfo.userID = String.valueOf(mJniObjs.mRemoteVideoStats.getUid());
                        mRemoteWindowManager.updateVideoDown(videoInfo, mJniObjs.mRemoteVideoStats.getReceivedBitrate());
                        break;
                    case LocalConstans.CALL_BACK_ON_REMOTE_AUDIO_STATE: // 接收 SDK 远端音频下行相关信息，如码率
                        UserInfo audioInfo = new UserInfo();
                        audioInfo.userID = String.valueOf(mJniObjs.mRemoteAudioStats.getUid());
                        mRemoteWindowManager.updateAudioDown(audioInfo, mJniObjs.mRemoteAudioStats.getReceivedBitrate());
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_VIDEO_STATE: // 接收 SDK 本地视频上行相关信息，如码率
                        setTextViewContent(mVideoSpeedShow, R.string.main_videoups, String.valueOf(mJniObjs.mLocalVideoStats.getSentBitrate()));
                        break;
                    case LocalConstans.CALL_BACK_ON_LOCAL_AUDIO_STATE: // 接收 SDK 本地音频上行相关信息，如码率
                        setTextViewContent(mAudioSpeedShow, R.string.main_audioup, String.valueOf(mJniObjs.mLocalAudioStats.getSentBitrate()));
                        break;
                    case LocalConstans.CALL_BACK_ON_AUDIO_ROUTE: // 接收 SDK 回调的音频路由状态，调整 UI 音量图标。
                        int mAudioRoute = mJniObjs.mAudioRoute;
                        if (LocalConfig.mRole == Constants.CLIENT_ROLE_ANCHOR) {
                            if (mAudioRoute == Constants.AUDIO_ROUTE_SPEAKER) {
                                mIsHeadset = false;
                                if (mIsMute) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_mute_speaker_selector);
                                } else {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_speaker_selector);
                                }
                            } else {
                                mIsHeadset = true;
                                if (mIsMute) {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_muted_headset_selector);
                                } else {
                                    mAudioChannel.setImageResource(R.drawable.mainly_btn_headset_selector);
                                }
                            }
                        }
                        break;
                }
            }
        }
    }
}
