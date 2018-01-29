package com.ughouse.facegverify.db;

import android.text.TextUtils;
import android.util.Log;

import com.arcsoft.facerecognition.AFR_FSDKEngine;
import com.arcsoft.facerecognition.AFR_FSDKError;
import com.arcsoft.facerecognition.AFR_FSDKFace;
import com.arcsoft.facerecognition.AFR_FSDKVersion;
import com.guo.android_extend.java.ExtInputStream;
import com.guo.android_extend.java.ExtOutputStream;
import com.ughouse.facegverify.bean.UgFaceDataBean;
import com.ughouse.facegverify.util.MySqlUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gqj3375 on 2017/7/11.
 */

public class FaceDB {
    private final String TAG = this.getClass().toString();

    public static String appid = "GubdA9UGomz6minP7YihR65oQaUdwfUXxXaH2RUJJefv";
    public static String ft_key = "34hmsZrTX2jftMHj3kZWekJUFxmYUnfKfcfB1QB2Ch4M";
    public static String fd_key = "34hmsZrTX2jftMHj3kZWekJbRN2gdic3M3V9BCgRWTij";
    public static String fr_key = "34hmsZrTX2jftMHj3kZWekK64y5LYa6LDgQFU5yRs9KU";
    public static String age_key = "34hmsZrTX2jftMHj3kZWekKLPmbisLkv6RK78jxBjX6g";
    public static String gender_key = "34hmsZrTX2jftMHj3kZWekKTZArtQPNcJGPRERutfqxF";

    String mDBPath;
    public List<FaceRegist> mRegister;
    public List<UgFaceDataBean> faceDataList;
    AFR_FSDKEngine mFREngine;
    AFR_FSDKVersion mFRVersion;
    boolean mUpgrade;

    public class FaceRegist {
        public String mName;
        public List<AFR_FSDKFace> mFaceList;

        public FaceRegist(String name) {
            mName = name;
            mFaceList = new ArrayList<>();
        }
    }

    public FaceDB(String path) {
        mDBPath = path;
        mRegister = new ArrayList<>();
        mFRVersion = new AFR_FSDKVersion();
        mUpgrade = false;
        mFREngine = new AFR_FSDKEngine();
        AFR_FSDKError error = mFREngine.AFR_FSDK_InitialEngine(FaceDB.appid, FaceDB.fr_key);
        if (error.getCode() != AFR_FSDKError.MOK) {
            Log.e(TAG, "AFR_FSDK_InitialEngine fail! error code :" + error.getCode());
        } else {
            mFREngine.AFR_FSDK_GetVersion(mFRVersion);
            Log.d(TAG, "AFR_FSDK_GetVersion=" + mFRVersion.toString());
        }
    }

    public void destroy() {
        if (mFREngine != null) {
            mFREngine.AFR_FSDK_UninitialEngine();
        }
    }

    private boolean saveInfo() {
        try {
            FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt");
            ExtOutputStream bos = new ExtOutputStream(fs);
            bos.writeString(mFRVersion.toString() + "," + mFRVersion.getFeatureLevel());
            bos.close();
            fs.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean loadInfo() {
        if (!mRegister.isEmpty()) {
            return false;
        }
        try {
            FileInputStream fs = new FileInputStream(mDBPath + "/face.txt");
            ExtInputStream bos = new ExtInputStream(fs);
            //load version
            String version_saved = bos.readString();
            if (version_saved.equals(mFRVersion.toString() + "," + mFRVersion.getFeatureLevel())) {
                mUpgrade = true;
            }
            //load all regist name.
            if (version_saved != null) {
                for (String name = bos.readString(); name != null; name = bos.readString()) {
                    if (new File(mDBPath + "/" + name + ".data").exists()) {
                        mRegister.add(new FaceRegist(new String(name)));
                    }
                }
            }
            bos.close();
            fs.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean loadFaces() {
        if (loadInfo()) {
            try {
                for (FaceRegist face : mRegister) {
                    Log.d(TAG, "load name:" + face.mName + "'s face feature data.");
                    FileInputStream fs = new FileInputStream(mDBPath + "/" + face.mName + ".data");
                    ExtInputStream bos = new ExtInputStream(fs);
                    AFR_FSDKFace afr = null;
                    do {
                        if (afr != null) {
                            if (mUpgrade) {
                                //upgrade data.
                            }
                            face.mFaceList.add(afr);
                        }
                        afr = new AFR_FSDKFace();
                    } while (bos.readBytes(afr.getFeatureData()));
                    bos.close();
                    fs.close();
                    Log.d(TAG, "load name: size = " + face.mFaceList.size());
                }
                return true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 读取数据库中所有人脸数据
     */
    public List<UgFaceDataBean> loadFaceData() {
        String sql = "select * from ug_face_data;";
        if (faceDataList != null && faceDataList.size() > 0) {
            faceDataList.clear();
            faceDataList = null;
        }
        faceDataList = MySqlUtil.queryUgFaceData(sql);
        loadToFaceRegist();
        return faceDataList;
    }

    /**
     * 将数据库数据放到mRegister
     */
    private void loadToFaceRegist() {
        if (faceDataList != null && faceDataList.size() > 0) {
            for (UgFaceDataBean faceData : faceDataList) {
                if (!TextUtils.isEmpty(faceData.getFace_user_img_no())) {
                    mRegister.add(new FaceRegist(new String(faceData.getFace_user_img_no())));
                }
            }
            for (int i = 0; i < mRegister.size(); i++) {
                FaceRegist face = mRegister.get(i);
                Log.d(TAG, "load name:" + face.mName + "'s face feature data.");
                AFR_FSDKFace afr = afr = new AFR_FSDKFace();
                afr.setFeatureData(faceDataList.get(i).getFace_data());
                face.mFaceList.add(afr);
                Log.d(TAG, "load name: size = " + face.mFaceList.size());
            }
        }
    }


    public void addFace(String name, AFR_FSDKFace face) {
        try {
            //check if already registered.
            boolean add = true;
            for (FaceRegist frface : mRegister) {
                if (frface.mName.equals(name)) {
                    frface.mFaceList.add(face);
                    add = false;
                    break;
                }
            }
            if (add) { // not registered.
                FaceRegist frface = new FaceRegist(name);
                frface.mFaceList.add(face);
                mRegister.add(frface);
            }

            if (saveInfo()) {
                //update all names
                FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt", true);
                ExtOutputStream bos = new ExtOutputStream(fs);
                for (FaceRegist frface : mRegister) {
                    bos.writeString(frface.mName);
                }
                bos.close();
                fs.close();

                //save new feature
                fs = new FileOutputStream(mDBPath + "/" + name + ".data", true);
                bos = new ExtOutputStream(fs);
                bos.writeBytes(face.getFeatureData());
                bos.close();
                fs.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean delete(String name) {
        try {
            //check if already registered.
            boolean find = false;
            for (FaceRegist frface : mRegister) {
                if (frface.mName.equals(name)) {
                    File delfile = new File(mDBPath + "/" + name + ".data");
                    if (delfile.exists()) {
                        delfile.delete();
                    }
                    mRegister.remove(frface);
                    find = true;
                    break;
                }
            }

            if (find) {
                if (saveInfo()) {
                    //update all names
                    FileOutputStream fs = new FileOutputStream(mDBPath + "/face.txt", true);
                    ExtOutputStream bos = new ExtOutputStream(fs);
                    for (FaceRegist frface : mRegister) {
                        bos.writeString(frface.mName);
                    }
                    bos.close();
                    fs.close();
                }
            }
            return find;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean upgrade() {
        return false;
    }
}
