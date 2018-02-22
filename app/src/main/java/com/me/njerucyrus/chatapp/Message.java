package com.me.njerucyrus.chatapp;

/**
 * Created by njerucyrus on 2/20/18.
 */

public class Message {
    private String message;
    private String type;
    private String from;
    private String to;
    private String messageKey;
    private long time;
    private boolean seen;



    public Message(){}

    public Message(String message, String type, String from, String to, String messageKey, long time, boolean seen) {
        this.message = message;
        this.type = type;
        this.from = from;
        this.to = to;
        this.messageKey = messageKey;
        this.time = time;
        this.seen = seen;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }


    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }
}
