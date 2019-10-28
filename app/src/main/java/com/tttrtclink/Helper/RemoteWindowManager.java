package com.tttrtclink.Helper;

import android.text.TextUtils;

import com.tttrtclink.LocalConfig;
import com.tttrtclink.R;
import com.tttrtclink.bean.UserInfo;
import com.tttrtclink.ui.MainActivity;
import com.tttrtclink.utils.DensityUtils;
import com.wushuangtech.expansion.bean.VideoCompositingLayout;

import java.util.ArrayList;
import java.util.List;

public class RemoteWindowManager {

    private ArrayList<RemoteWindow> mRemoteWindows = new ArrayList<>();
    private int mScreenWidth;
    private int mScreenHeight;

    public RemoteWindowManager(MainActivity activity) {
        RemoteWindow mRemoteWindow = activity.findViewById(R.id.left_remote_window);
        mRemoteWindow.setRemoteWindowManager(this);
        mRemoteWindows.add(mRemoteWindow);
        //获取屏幕的宽和高
        int[] screenData = DensityUtils.getScreenData(activity);
        mScreenWidth = screenData[0];
        mScreenHeight = screenData[1];
    }

    public boolean isFull() {
        for (int i = 0; i < mRemoteWindows.size(); i++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (TextUtils.isEmpty(info.roomID)) {
                return false;
            }
        }
        return true;
    }

    public boolean isExist(UserInfo userInfo) {
        for (int i = 0; i < mRemoteWindows.size(); i++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (userInfo.roomID.equals(info.roomID) && userInfo.userID.equals(info.userID)) {
                return true;
            }
        }
        return false;
    }

    public int showWindow(UserInfo userInfo) {
        for (int i = 0; i < mRemoteWindows.size(); i++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (TextUtils.isEmpty(info.roomID)) {
                mRemoteWindows.get(i).setInfo(userInfo);
                return 0;
            }
        }
        return -1;
    }

    public void removeWindow(UserInfo userInfo) {
        for (int i = 0; i < mRemoteWindows.size(); i++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (userInfo.roomID.equals(info.roomID) && userInfo.userID.equals(info.userID)) {
                mRemoteWindows.get(i).setInfo(null);
            }
        }
    }

    public void unlinkOtherAnchor(UserInfo userInfo) {
        for (int i = 0; i < mRemoteWindows.size(); i++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (userInfo.roomID.equals(info.roomID) && userInfo.userID.equals(info.userID)) {
                mRemoteWindows.get(i).unlinkOtherAnchor();
                return;
            }
        }
    }

    public void updateVolume(UserInfo userInfo, int level) {
        for (int i = 0; i < mRemoteWindows.size(); i++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (userInfo.userID.equals(info.userID)) {
                mRemoteWindows.get(i).updateVolume(level);
                return;
            }
        }
    }

    public void updateAudioDown(UserInfo userInfo, int audioBitrate) {
        for (int i = 0; i < mRemoteWindows.size(); i++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (userInfo.userID.equals(info.userID)) {
                mRemoteWindows.get(i).updateAudioDown(audioBitrate);
                return;
            }
        }
    }

    public void updateVideoDown(UserInfo userInfo, int videoBitrate) {
        for (int i = 0; i < mRemoteWindows.size(); i++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (userInfo.userID.equals(info.userID)) {
                mRemoteWindows.get(i).updateVideoDown(videoBitrate);
                return;
            }
        }
    }

    // Demo演示的PK布局，是两个主播左右分屏，宽度各占布局一般
    public VideoCompositingLayout getVideoCompositingLayout() {
        List<VideoCompositingLayout.Region> tempList = new ArrayList<>();
        // 混屏画布的大小是通过VideoCompositingLayout对象的mCanvasWidth和mCanvasHeight属性设置的，可以不设置，默认大小是视频编码大小。
        // 先构建自己的 Region 对象，
        VideoCompositingLayout.Region mRegion = new VideoCompositingLayout.Region();
        // 用户ID
        mRegion.mUserID = LocalConfig.mUid;
        //x坐标是视频窗口左上角相对画布的比例。0代表起始点，是画布的左上角，这里0代表视频左边是从画布左边开始的。取值范围是0~1
        mRegion.x = 0f;
        //y坐标是视频窗口左上角相对画布的比例，0代表起始点，是画布的左上角，这里取0.25目的是让视频上下居中。取值范围是0~1
        mRegion.y = 0.25f;
        //视频窗口的宽度是相对于画布的比例，这里0.5代表视频宽度是画布宽度的一半。
        mRegion.width = 0.5f;
        //视频窗口的高度是相对于画布的比例，这里0.5代表视频宽度是画布高度的一半。
        mRegion.height = 0.5f;
        //视频窗口的层级，默认从0开始，数字大的覆盖数字小的，即 zOrder 值为1的窗口，在混流视频中，会覆盖 zOrder 值为 0 的窗口。
        mRegion.zOrder = 0;
        tempList.add(mRegion);

        for (int i = 0; i < mRemoteWindows.size(); i++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            //若用户ID不为空，说明是个真实用户
            if (!TextUtils.isEmpty(info.userID)) {
                //创建远端用户的 Region 对象
                VideoCompositingLayout.Region remoteUser = new VideoCompositingLayout.Region();
                // 用户ID
                remoteUser.mUserID = Long.parseLong(info.userID);
                //x坐标是视频窗口左上角相对画布的比例。0代表起始点，是画布的左上角，这里0.5代表视频左边是从画布中间开始的。取值范围是0~1
                remoteUser.x = 0.5f;
                //y坐标是视频窗口左上角相对画布的比例，0代表起始点，是画布的左上角，这里取0.25目的是让视频上下居中。取值范围是0~1
                remoteUser.y = 0.25f;
                //视频窗口的宽度是相对于画布的比例，这里0.5代表视频宽度是画布宽度的一半。
                remoteUser.width = 0.5f;
                //视频窗口的高度是相对于画布的比例，这里0.5代表视频宽度是画布高度的一半。
                remoteUser.height = 0.5f;
                //视频窗口的层级，默认从0开始，数字大的覆盖数字小的，即 zOrder 值为1的窗口，在混流视频中，会覆盖 zOrder 值为 0 的窗口。
                remoteUser.zOrder = 0;
                tempList.add(remoteUser);
            }
        }
        VideoCompositingLayout.Region[] mRegions = new VideoCompositingLayout.Region[tempList.size()];
        for (int k = 0; k < tempList.size(); k++) {
            mRegions[k] = tempList.get(k);
        }
        //构建 VideoCompositingLayout 对象，传递给 SDK
        //这里要注意的是x+width的值不能大于1，同理y+height的值也不能大于1，否则视频显示不出来
        VideoCompositingLayout layout = new VideoCompositingLayout();
        layout.regions = mRegions;
        return layout;
    }
}
