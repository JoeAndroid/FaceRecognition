package com.ughouse.facegverify.util;


import android.content.Context;
import android.util.Log;

import com.ughouse.facegverify.bean.UgFaceDataBean;
import com.ughouse.facegverify.bean.UgHouseBean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作数据库
 * Created by qiaobing on 2018/1/16.
 */
public class MySqlUtil {

    public static PreparedStatement ps;

    public static Connection conn;

    //openConnection是连接数据库
    public static Connection openConnection(String url, String user,
                                            String password) {
        try {
            final String DRIVER_NAME = "com.mysql.jdbc.Driver";
            Class.forName(DRIVER_NAME);
            conn = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            conn = null;
        } catch (SQLException e) {
            conn = null;
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
        }
        System.out.println("conn---" + conn);
        return conn;
    }

    //查询数据库
    public static UgHouseBean queryUgHouse(Connection conn, String sql) {

        if (conn == null) {
            return null;
        }

        Statement statement = null;
        ResultSet result = null;
        UgHouseBean houseBean = null;
        try {
            statement = conn.createStatement();
            result = statement.executeQuery(sql);
            if (result != null && result.first()) {
                int idColumnIndex = result.findColumn("id");
                int user_noColumnIndex = result.findColumn("face_user_no");
                int stateColumnIndex = result.findColumn("state");
                int face_stateColumnIndex = result.findColumn("face_state");
                int face_numColumnIndex = result.findColumn("face_num");
                while (!result.isAfterLast()) {
                    System.out.println("------------------");
                    System.out.println("id " + result.getString(idColumnIndex) + "\t");
                    System.out.println("face_user_no " + result.getString(user_noColumnIndex));
                    System.out.println("state " + result.getString(stateColumnIndex));
                    System.out.println("face_state " + result.getString(face_stateColumnIndex));
                    System.out.println("face_num " + result.getString(face_numColumnIndex));
                    houseBean = new UgHouseBean();
                    houseBean.setId(result.getString(idColumnIndex));
                    houseBean.setFace_user_no(result.getString(user_noColumnIndex));
                    houseBean.setState(result.getString(stateColumnIndex));
                    houseBean.setFace_state(result.getString(face_stateColumnIndex));
                    houseBean.setFace_num(result.getString(face_numColumnIndex));
                    result.next();
                }
            }
            return houseBean;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
        } finally {
            try {
                if (result != null) {
                    result.close();
                    result = null;
                }
                if (statement != null) {
                    statement.close();
                    statement = null;
                }

            } catch (SQLException sqle) {

            }
            return houseBean;
        }
    }

    //查询数据库
    public static List<UgFaceDataBean> queryUgFaceData(String sql) {

        if (conn == null) {
            return null;
        }
        Statement statement = null;
        ResultSet result = null;
        List<UgFaceDataBean> faceDataList = new ArrayList<>();
        try {
            statement = conn.createStatement();
            result = statement.executeQuery(sql);
            if (result != null && result.first()) {
                int idColumnIndex = result.findColumn("id");
                int user_img_noColumnIndex = result.findColumn("face_user_img_no");
                int imgColumnIndex = result.findColumn("img");
                int face_dataColumnIndex = result.findColumn("face_data");
                int create_timeColumnIndex = result.findColumn("create_time");
                int user_noColumnIndex = result.findColumn("face_user_no");
                int stateColumnIndex = result.findColumn("state");
                while (!result.isAfterLast()) {
                    System.out.println("------------------");
                    System.out.println("id " + result.getString(idColumnIndex) + "\t");
                    System.out.println("face_user_img_no " + result.getString(user_img_noColumnIndex));
                    System.out.println("img " + result.getString(imgColumnIndex));
                    System.out.println("face_data " + result.getString(face_dataColumnIndex));
                    System.out.println("create_time " + result.getString(create_timeColumnIndex));
                    System.out.println("face_user_no " + result.getString(user_noColumnIndex));
                    System.out.println("state " + result.getString(stateColumnIndex));
                    UgFaceDataBean faceDataBean = new UgFaceDataBean();
                    faceDataBean.setId(result.getInt(idColumnIndex));
                    faceDataBean.setFace_user_no(result.getString(user_noColumnIndex));
                    faceDataBean.setFace_user_img_no(result.getString(user_img_noColumnIndex));
                    faceDataBean.setCreate_time(result.getString(create_timeColumnIndex));
                    faceDataBean.setState(result.getInt(stateColumnIndex));
                    faceDataBean.setImg(result.getBytes(imgColumnIndex));
                    faceDataBean.setFace_data(result.getBytes(face_dataColumnIndex));
                    faceDataList.add(faceDataBean);
                    result.next();
                }
            }
            return faceDataList;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
        } finally {
            try {
                if (result != null) {
                    result.close();
                    result = null;
                }
                if (statement != null) {
                    statement.close();
                    statement = null;
                }

            } catch (SQLException sqle) {

            }
            return faceDataList;
        }
    }

    //执行MySQL语句的函数
    public static boolean execSQL(Connection conn, String sql) {
        boolean execResult = false;
        if (conn == null) {
            return execResult;
        }

        Statement statement = null;

        try {
            statement = conn.createStatement();
            if (statement != null) {
                execResult = statement.execute(sql);
            }
        } catch (SQLException e) {
            execResult = false;
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("VendorError: " + e.getErrorCode());
        }

        return execResult;
    }

    public void onInsert() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String sql = "insert into users values(15, 'xiaoming')";
                MySqlUtil.execSQL(conn, sql);
                Log.i("onInsert", "onInsert");
            }
        }).start();
    }


    public void onDelete() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String sql = "delete from users where name='hanmeimei'";
                MySqlUtil.execSQL(conn, sql);
                Log.i("onDelete", "onDelete");
            }
        }).start();
    }

    public void onUpdate() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String sql = "update users set name='lilei' where name='liyanzhen'";
                MySqlUtil.execSQL(conn, sql);
                Log.i("onUpdate", "onUpdate");
            }
        }).start();
    }

    public void onQuery() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MySqlUtil.execSQL(conn, "select * from users");
                Log.i("onQuery", "onQuery");
            }
        }).start();
    }

    /**
     * 将指定路径的文件(比如：图片，word文档等)存储到数据库
     *
     * @param strFile 要存放到数据库的文件路径，如D:\\a.jpg
     */
    public static void storeImg(String strFile) throws Exception {
        int id = 0;
        File file = new File(strFile);
        FileInputStream fis = new FileInputStream(file);
        try {
            ps = conn.prepareStatement("insert "
                    + "into PIC values (?,?,?)");
            ps.setInt(1, id);
            ps.setString(2, file.getName());
            ps.setBinaryStream(3, fis, (int) file.length());
            ps.executeUpdate();
            System.out.println("file insert success ");
        } catch (SQLException e) {
            System.out.println("SQLException: "
                    + e.getMessage());
            System.out.println("SQLState: "
                    + e.getSQLState());
            System.out.println("VendorError: "
                    + e.getErrorCode());
            e.printStackTrace();
        } finally {
            ps.close();
            fis.close();
            conn.close();
        }
    }

    /**
     * 将指定路径的文件(比如：图片，word文档等)存储到数据库
     *
     * @param faceByte 要存放到数据库的文件路径，如D:\\a.jpg
     */
    public static void storeByteArray(byte[] faceByte) throws Exception {
        int id = 0;
        try {
            ps = conn.prepareStatement("update "
                    + "ug_face_data set face_data=? where id=1");
            ps.setBytes(1, faceByte);
            ps.executeUpdate();
            System.out.println("file insert success ");
        } catch (SQLException e) {
            System.out.println("SQLException: "
                    + e.getMessage());
            System.out.println("SQLState: "
                    + e.getSQLState());
            System.out.println("VendorError: "
                    + e.getErrorCode());
            e.printStackTrace();
        } finally {
            ps.close();
            conn.close();
        }
    }

    /**
     * 将指定路径的文件(比如：图片，word文档等)存储到数据库
     *
     * @param faceByte 要存放到数据库的文件路径，如D:\\a.jpg
     */
    public static void storeImageByteArray(byte[] faceByte) throws Exception {
        int id = 0;
        try {
            ps = conn.prepareStatement("update "
                    + "ug_face_data set img=? where id=1");
            ps.setBytes(1, faceByte);
            ps.executeUpdate();
            System.out.println("file insert success ");
        } catch (SQLException e) {
            System.out.println("SQLException: "
                    + e.getMessage());
            System.out.println("SQLState: "
                    + e.getSQLState());
            System.out.println("VendorError: "
                    + e.getErrorCode());
            e.printStackTrace();
        } finally {
            ps.close();
            conn.close();
        }
    }

    /**
     * 将存储在数据库中的文件(比如：图片，word文档等)读取到指定路径
     *
     * @param id 数据库里记录的id
     */
    public static void readImg(int id) throws Exception {
        byte[] buffer = new byte[4096];
        FileOutputStream outputImage = null;
        InputStream is = null;
        try {
            ps = conn.prepareStatement("select face_data from ug_face_data where id =?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            rs.next();
            File file = new File(FileUtils.getFilePath() + "/image/", "a.data");
            if (!file.exists()) {
                file.createNewFile();
            }
            outputImage = new FileOutputStream(file);
            Blob blob = rs.getBlob("face_data");   //img为数据库存放图片字段名称
            is = blob.getBinaryStream();
            int size = 0;
            while ((size = is.read(buffer)) != -1) {
                outputImage.write(buffer, 0, size);
            }
            System.out.println("file read success ");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            is.close();
            outputImage.close();
            ps.close();
            conn.close();
        }
    }

    /**
     * 读取数据库文件（.sql），并执行sql语句
     */
    public static void executeAssetsSQL(Context mContext, String schemaName) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(mContext.getAssets()
                    .open(schemaName + ".sql")));

            System.out.println("路径:" + schemaName + ".sql");
            String line;
            String buffer = "";
            while ((line = in.readLine()) != null) {
                buffer += line;
                if (line.trim().endsWith(";")) {
                    execSQL(conn, buffer.replace(";", ""));
                    buffer = "";
                }
            }
        } catch (IOException e) {
            Log.e("db-error", e.toString());
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                Log.e("db-error", e.toString());
            }
        }
    }
}
