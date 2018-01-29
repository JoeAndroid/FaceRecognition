package com.ughouse.facegverify;

/**
 * eventbus事件
 * Created by qiaobing on 2018/1/21.
 */
public class MessageEvent {
    public String message;

    public MessageEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}