package com.ughouse.facegverify.bean;

/**
 * 房间进入们位置表
 * Created by qiaobing on 2018/1/19.
 */
public class UgHouseBean {

    private String id;
    private String state;//当前用户操作状态
    private String face_state;//当前人脸采集状态
    private String face_num;//当前人脸数量
    private String face_user_no;//当前用户临时id

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getFace_state() {
        return face_state;
    }

    public void setFace_state(String face_state) {
        this.face_state = face_state;
    }

    public String getFace_num() {
        return face_num;
    }

    public void setFace_num(String face_num) {
        this.face_num = face_num;
    }

    public String getFace_user_no() {
        return face_user_no;
    }

    public void setFace_user_no(String face_user_no) {
        this.face_user_no = face_user_no;
    }
}
