package com.tttrtclink;

import com.tttrtclink.callback.MyTTTRtcEngineEventHandler;

import java.util.ArrayList;

/**
 * Created by wangzhiguo on 17/6/15.
 */

public class LocalConfig {

    public static ArrayList<Long> mUserEnterOrder = new ArrayList<>();

    public static long mUid;

    public static String mRoomID;

    public static int mRole;

    public static long mLinkRoomID;

    public static MyTTTRtcEngineEventHandler mMyTTTRtcEngineEventHandler;
}
