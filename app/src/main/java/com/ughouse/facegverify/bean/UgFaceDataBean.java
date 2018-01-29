package com.ughouse.facegverify.bean;

/**
 * 人脸样本表
 * Created by qiaobing on 2018/1/19.
 */
public class UgFaceDataBean {

    private int id;
    private byte[] img;//人脸图片
    private byte[] face_data;//人脸样本
    private String create_time;//创建时间
    private int state;
    private String face_user_img_no;//当前用户临时id
    private String face_user_no;//当前用户临时id

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public byte[] getImg() {
        return img;
    }

    public void setImg(byte[] img) {
        this.img = img;
    }

    public byte[] getFace_data() {
        return face_data;
    }

    public void setFace_data(byte[] face_data) {
        this.face_data = face_data;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getFace_user_no() {
        return face_user_no;
    }

    public void setFace_user_no(String face_user_no) {
        this.face_user_no = face_user_no;
    }

    public String getCreate_time() {
        return create_time;
    }

    public void setCreate_time(String create_time) {
        this.create_time = create_time;
    }

    public String getFace_user_img_no() {
        return face_user_img_no;
    }

    public void setFace_user_img_no(String face_user_img_no) {
        this.face_user_img_no = face_user_img_no;
    }

}
