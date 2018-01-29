package com.ughouse.facegverify.view;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.ughouse.facegverify.R;

/**
 * 扫描框
 * Created by qiaobing on 2018/1/25.
 */
public class ViewfinderView extends View {
    private static final String TAG = ViewfinderView.class.getSimpleName();

    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    public static final int MAX_FRAME_WIDTH = 1200; // = 5/8 * 1920
    public static final int MAX_FRAME_HEIGHT = 675; // = 5/8 * 1080


    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int POINT_SIZE = 6;

    private int resultColor = 0xb0000000;//扫描成功后扫描框以外区域白色
    private int frameOutsideColor = 0x60000000;//扫描框以外区域半透明黑色
    private int frameCornerColor = 0xff00ff00;//扫描框以外区域半透明黑色

    private Context context;
    // 屏幕分辨率
    private Point screenResolution;
    private Rect framingRect;
    private int laserFrameTopMargin;
    private int statusBarHeight;
    private Paint paint;
    private Bitmap resultBitmap;
    private Bitmap laserLineBitmap;
    private int laserLineTop;// 扫描线最顶端位置
    private int animationDelay = 0;

    private int laserLineHeight;//扫描线默认高度
    private int frameCornerWidth = 4;//扫描框4角宽
    private int frameCornerLength = 30;//扫描框4角高

    public ViewfinderView(Context context) {
        super(context, null);
    }

    public ViewfinderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Rect frame = getFramingRect();//取扫描框
        //全屏不绘制扫描框以外4个区域
        drawMask(canvas, frame);
        drawFrameCorner(canvas, frame);//绘制扫描框4角
        // 如果有二维码结果的Bitmap，在扫取景框内绘制不透明的result Bitmap
      /*  if (resultBitmap != null) {
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {
            drawFrame(canvas, frame);//绘制扫描框
            drawFrameCorner(canvas, frame);//绘制扫描框4角

            //全屏移动扫描线
            moveLaserSpeedFullScreen(getScreenResolution());//计算全屏移动位置
            drawLaserLineFullScreen(canvas, getScreenResolution());//绘制全屏扫描线
        }*/
    }

    private void moveLaserSpeedFullScreen(Point point) {
        //初始化扫描线起始点为顶部位置
        int laserMoveSpeed = 6;
        // 每次刷新界面，扫描线往下移动 LASER_VELOCITY
        laserLineTop += laserMoveSpeed;
        if (laserLineTop >= point.y) {
            laserLineTop = 0;
        }
        if (animationDelay == 0) {
            animationDelay = (int) ((1.0f * 1000 * laserMoveSpeed) / point.y);
        }
        postInvalidateDelayed(animationDelay);
    }

    /**
     * 画全屏宽扫描线
     *
     * @param canvas
     * @param point
     */
    private void drawLaserLineFullScreen(Canvas canvas, Point point) {
        if (laserLineBitmap == null)//图片资源文件转为 Bitmap
            laserLineBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.zfb_grid_scan_line);
        int height = laserLineBitmap.getHeight();//取原图高
        //网格图片
        int dstRectFTop = 0;
        if (laserLineTop >= height) {
            dstRectFTop = laserLineTop - height;
        }
        RectF dstRectF = new RectF(0, dstRectFTop, point.x, laserLineTop);
        Rect srcRect = new Rect(0, (int) (height - dstRectF.height()), laserLineBitmap.getWidth(), height);
        canvas.drawBitmap(laserLineBitmap, srcRect, dstRectF, paint);

    }


    /**
     * 画扫描框外区域
     *
     * @param canvas
     * @param frame
     */
    private void drawMask(Canvas canvas, Rect frame) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        paint.setColor(resultBitmap != null ? resultColor : frameOutsideColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);
    }

    /**
     * 绘制扫描框4角
     *
     * @param canvas
     * @param frame
     */
    private void drawFrameCorner(Canvas canvas, Rect frame) {
        paint.setColor(frameCornerColor);
        paint.setStyle(Paint.Style.FILL);
        // 左上角
        canvas.drawRect(frame.left - frameCornerWidth, frame.top, frame.left, frame.top + frameCornerLength, paint);
        canvas.drawRect(frame.left - frameCornerWidth, frame.top - frameCornerWidth, frame.left + frameCornerLength, frame.top, paint);
        // 右上角
        canvas.drawRect(frame.right, frame.top, frame.right + frameCornerWidth, frame.top + frameCornerLength, paint);
        canvas.drawRect(frame.right - frameCornerLength, frame.top - frameCornerWidth, frame.right + frameCornerWidth, frame.top, paint);
        // 左下角
        canvas.drawRect(frame.left - frameCornerWidth, frame.bottom - frameCornerLength, frame.left, frame.bottom, paint);
        canvas.drawRect(frame.left - frameCornerWidth, frame.bottom, frame.left + frameCornerLength, frame.bottom + frameCornerWidth, paint);
        // 右下角
        canvas.drawRect(frame.right, frame.bottom - frameCornerLength, frame.right + frameCornerWidth, frame.bottom, paint);
        canvas.drawRect(frame.right - frameCornerLength, frame.bottom, frame.right + frameCornerWidth, frame.bottom + frameCornerWidth, paint);
    }

    /**
     * 画扫描框
     *
     * @param canvas
     * @param frame
     */
    private void drawFrame(Canvas canvas, Rect frame) {
        paint.setColor(Color.TRANSPARENT);//扫描边框白色
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(frame, paint);
    }

    public synchronized Rect getFramingRect() {
        if (framingRect == null) {
            Point screenResolution = getScreenResolution();
            if (screenResolution == null) {
                // Called early, before init even finished
                return null;
            }
            int height;
            int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH, MAX_FRAME_WIDTH);
            //竖屏则为正方形
            if (isPortrait()) {
                height = width;
            } else {
                height = findDesiredDimensionInRange(screenResolution.y, MIN_FRAME_HEIGHT, MAX_FRAME_HEIGHT);
            }
            createFramingRect(width, height, screenResolution);
        }
        return framingRect;
    }

    public boolean isPortrait() {
        return context.getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_PORTRAIT;
    }

    private static int findDesiredDimensionInRange(int resolution, int hardMin, int hardMax) {
        int dim = 6 * resolution / 8; // Target 5/8 of each dimension
        if (dim < hardMin) {
            return hardMin;
        }
        if (dim > hardMax) {
            return hardMax;
        }
        return dim;
    }


    private void createFramingRect(int width, int height, Point screenResolution) {
        int leftOffset = (screenResolution.x - width) / 2;
        int topOffset = (screenResolution.y - height) / 2;
        int top = laserFrameTopMargin;
        if (top == 0)
            top = topOffset - statusBarHeight;
        else {
            top += statusBarHeight;
        }
        framingRect = new Rect(leftOffset, top, leftOffset + width, top + height);
        Log.d(TAG, "Calculated framing rect: " + framingRect);
    }

    /**
     * 屏幕分辨率
     *
     * @return
     */
    private Point getScreenResolution() {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point theScreenResolution = new Point();
        display.getSize(theScreenResolution);
        screenResolution = theScreenResolution;
        return screenResolution;
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }
}
