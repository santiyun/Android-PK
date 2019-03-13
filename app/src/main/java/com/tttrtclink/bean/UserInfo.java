package com.tttrtclink.bean;

public class UserInfo {

    public String roomID;
    public String userID;
    public String devID;

    public UserInfo() {
    }

    public UserInfo(String roomID, String userID) {
        this.roomID = roomID;
        this.userID = userID;
    }

    public UserInfo(String roomID, String userID, String devID) {
        this.roomID = roomID;
        this.userID = userID;
        this.devID = devID;
    }
}
