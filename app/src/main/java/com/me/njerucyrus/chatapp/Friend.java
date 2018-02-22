package com.me.njerucyrus.chatapp;

import java.util.Date;

/**
 * Created by njerucyrus on 2/19/18.
 */

public class Friend {
    private String userId;
    private String date;

    public Friend(){}

    public Friend(String userId, String date) {
        this.userId = userId;
        this.date = date;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
