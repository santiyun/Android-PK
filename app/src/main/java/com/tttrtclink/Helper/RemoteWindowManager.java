package com.tttrtclink.Helper;

import android.text.TextUtils;

import com.tttrtclink.R;
import com.tttrtclink.bean.UserInfo;
import com.tttrtclink.ui.MainActivity;
import com.tttrtclink.utils.DensityUtils;
import com.wushuangtech.bean.VideoCompositingLayout;

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
        for (int i = 0; i < mRemoteWindows.size(); i ++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (TextUtils.isEmpty(info.roomID)) {
                return false;
            }
        }
        return true;
    }

    public boolean isExist(UserInfo userInfo) {
        for (int i = 0; i < mRemoteWindows.size(); i ++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (userInfo.roomID.equals(info.roomID) && userInfo.userID.equals(info.userID)) {
                return true;
            }
        }
        return false;
    }

    public int showWindow(UserInfo userInfo) {
        for (int i = 0; i < mRemoteWindows.size(); i ++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (TextUtils.isEmpty(info.roomID)) {
                mRemoteWindows.get(i).setInfo(userInfo);
                return 0;
            }
        }
        return -1;
    }

    public void removeWindow(UserInfo userInfo) {
        for (int i = 0; i < mRemoteWindows.size(); i ++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (userInfo.roomID.equals(info.roomID) && userInfo.userID.equals(info.userID)) {
                mRemoteWindows.get(i).setInfo(null);
            }
        }
    }

    public void unlinkOtherAnchor(UserInfo userInfo) {
        for (int i = 0; i < mRemoteWindows.size(); i ++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (userInfo.roomID.equals(info.roomID) && userInfo.userID.equals(info.userID)) {
                mRemoteWindows.get(i).unlinkOtherAnchor();
                return;
            }
        }
    }

    public void updateVolume(UserInfo userInfo, int level) {
        for (int i = 0; i < mRemoteWindows.size(); i ++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (userInfo.userID.equals(info.userID)) {
                mRemoteWindows.get(i).updateVolume(level);
                return;
            }
        }
    }

    public void updateAudioDown(UserInfo userInfo, int audioBitrate) {
        for (int i = 0; i < mRemoteWindows.size(); i ++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (userInfo.userID.equals(info.userID)) {
                mRemoteWindows.get(i).updateAudioDown(audioBitrate);
                return;
            }
        }
    }

    public void updateVideoDown(UserInfo userInfo, int videoBitrate) {
        for (int i = 0; i < mRemoteWindows.size(); i ++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            if (userInfo.userID.equals(info.userID)) {
                mRemoteWindows.get(i).updateVideoDown(videoBitrate);
                return;
            }
        }
    }

    public VideoCompositingLayout getVideoCompositingLayout() {
        //遍历远端用户集合(在 PK 场景中，一般是两个人 PK，所以这个集合其实只会有一个用户)
        List<VideoCompositingLayout.Region> tempList = new ArrayList<>();
        for (int i = 0; i < mRemoteWindows.size(); i ++) {
            UserInfo info = mRemoteWindows.get(i).getInfo();
            //若用户ID不为空，说明是个真实用户
            if (!TextUtils.isEmpty(info.userID)) {
                //创建Region信息封装类对象
                VideoCompositingLayout.Region mRegion = new VideoCompositingLayout.Region();
                //获取当前视频窗口相对屏幕的坐标位置，即左上角的x和y的坐标
                int[] location = new int[2];
                mRemoteWindows.get(i).getLocationOnScreen(location);
                //赋值
                mRegion.mUserID = Long.parseLong(info.userID);
                //x坐标是视频窗口左上角相对屏幕的比例，起始点是屏幕的左上角，取值范围是0~1
                mRegion.x = location[0] * 1.0f / mScreenWidth;
                //y坐标是视频窗口左上角相对屏幕的比例，起始点是屏幕的左上角，取值范围是0~1
                mRegion.y = location[1] * 1.0f / mScreenHeight;
                //视频窗口的宽度是相对于屏幕的比例。
                mRegion.width = mRemoteWindows.get(i).getWidth() * 1.0f / mScreenWidth;
                //视频窗口的高度是相对于屏幕的比例。
                mRegion.height = (mRemoteWindows.get(i).getHeight() * 1.0f / mScreenHeight) * 0.998f;
                //视频窗口的层级，默认从0开始，数字大的覆盖数字小的，即 zOrder 值为1的窗口，在混流视频中，会覆盖 zOrder 值为0的窗口。
                mRegion.zOrder = 1;
                tempList.add(mRegion);
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
