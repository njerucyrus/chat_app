package com.me.njerucyrus.chatapp;

/**
 * Created by njerucyrus on 2/16/18.
 */

public class User {
    private String userUid;
    private String displayName;
    private String status;
    private String image;
    private String imageThumbnail;
    private String deviceTokenId;
    private String online;


    public User(){}

    public User(String userUid, String displayName, String status, String image, String imageThumbnail, String deviceTokenId, String online) {
        this.userUid = userUid;
        this.displayName = displayName;
        this.status = status;
        this.image = image;
        this.imageThumbnail = imageThumbnail;
        this.deviceTokenId = deviceTokenId;
        this.online = online;
    }

    public String getUserUid() {
        return userUid;
    }

    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImageThumbnail() {
        return imageThumbnail;
    }

    public void setImageThumbnail(String imageThumbnail) {
        this.imageThumbnail = imageThumbnail;
    }

    public String getDeviceTokenId() {
        return deviceTokenId;
    }

    public void setDeviceTokenId(String deviceTokenId) {
        this.deviceTokenId = deviceTokenId;
    }

    public String getOnline() {
        return online;
    }

    public void setOnline(String online) {
        this.online = online;
    }
}
