package com.tttrtclink.Helper;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tttrtclink.LocalConfig;
import com.tttrtclink.R;
import com.tttrtclink.bean.UserInfo;
import com.wushuangtech.library.Constants;
import com.wushuangtech.wstechapi.TTTRtcEngine;
import com.wushuangtech.wstechapi.model.VideoCanvas;

public class RemoteWindow extends RelativeLayout implements View.OnClickListener{

    private Context mContext;
    private TTTRtcEngine mTTTEngine;
    private SurfaceView mSurfaceView;
    private View mView;
    private String mRoomID = "";
    private String mUserID = "";
    private String mDevID = "";
    private TextView mVideoDown, mAudioDown;
    private RemoteWindowManager mRemoteWindowManager;

    public RemoteWindow(Context context) {
        this(context, null);
    }

    public RemoteWindow(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RemoteWindow(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mTTTEngine = TTTRtcEngine.getInstance();
        mView = LayoutInflater.from(context).inflate(R.layout.remote_window, null);
        mView.findViewById(R.id.stop_link).setOnClickListener(this);
        mVideoDown = mView.findViewById(R.id.remote_video_down);
        mAudioDown = mView.findViewById(R.id.remote_audio_down);
        addView(mView, new LayoutParams(-1, -1));
    }

    private void setTextViewContent(TextView textView, int resourceID, String value) {
        String string = getResources().getString(resourceID);
        String result = String.format(string, value);
        textView.setText(result);
    }

    public void setRemoteWindowManager(RemoteWindowManager remoteWindowManager) {
        this.mRemoteWindowManager = remoteWindowManager;
    }

    public void setInfo(UserInfo userInfo) {
        if (userInfo == null) {
            mRoomID = "";
            mUserID = "";
            mDevID = "";
            removeView(mSurfaceView);
            mSurfaceView = null;
            this.setVisibility(View.INVISIBLE);
        } else {
            mRoomID = userInfo.roomID;
            mUserID = userInfo.userID;
            mDevID = userInfo.devID;

            mSurfaceView = mTTTEngine.CreateRendererView(mContext);
            mSurfaceView.setZOrderMediaOverlay(true);
            mTTTEngine.setupRemoteVideo(new VideoCanvas(Long.parseLong(mUserID), Constants.RENDER_MODE_HIDDEN, mSurfaceView));
            addView(mSurfaceView, 0);

            ((TextView) (mView.findViewById(R.id.remote_room_id))).setText("房间ID:" + mRoomID);
            ((TextView) (mView.findViewById(R.id.remote_user_id))).setText("用户ID:" + mUserID);
            this.setVisibility(View.VISIBLE);
        }
    }

    public UserInfo getInfo() {
        return new UserInfo(mRoomID, mUserID, mDevID);
    }

    public void unlinkOtherAnchor() {
        mTTTEngine.unSubscribeOtherChannel(LocalConfig.mLinkRoomID);
        UserInfo mUserInfo = new UserInfo(mRoomID, mUserID);
        mRemoteWindowManager.removeWindow(mUserInfo);
    }

    public void updateVolume (int volumeLevel) {
        ImageView volume = findViewById(R.id.remote_voice);
        if (volumeLevel <= 3) {
            volume.setImageResource(R.drawable.mainly_btn_speaker_selector);
        } else if (volumeLevel > 3 && volumeLevel <= 6) {
            volume.setImageResource(R.drawable.mainly_btn_speaker_middle_selector);
        } else if (volumeLevel > 6) {
            volume.setImageResource(R.drawable.mainly_btn_speaker_big_selector);
        }
    }

    public void updateVideoDown(int videoBitrate) {
        setTextViewContent(mVideoDown, R.string.videoly_videodown, String.valueOf(videoBitrate));
    }

    public void updateAudioDown(int audioBitrate) {
        setTextViewContent(mAudioDown, R.string.videoly_audiodown, String.valueOf(audioBitrate));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.stop_link:
                unlinkOtherAnchor();
                break;
        }
    }
}
