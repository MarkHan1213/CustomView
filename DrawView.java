package com.mark.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义的画图View
 * Created by Mark.Han on 2017/4/18.
 */
public class DrawView extends View {

    private Paint mPaint;//画笔
    private Paint ePaint;//橡皮擦画笔
    private Path mPath;//路径
    private Canvas mCanves;//画布
    private Bitmap mBitmap;
    private Point downPoint;//按下时的点
    private Point movePoint;//移动到最后的点

    private DrawType dt;//绘制类型

    private float mStartX = 0, mStartY = 0;
    private List<DrawPath> drawPaths;//绘制记录
    private int mWidth, mHeight;//宽和高
    private int index;//计数 0，1 三角形两次点击
    private DrawPath dp;

    private long firstTime;

    public DrawView(Context context) {
        this(context, null);
    }

    public DrawView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        drawPaths = new ArrayList<>();
        init();
        initCanvas();
    }

    public void setDt(DrawType dt) {
        this.dt = dt;
        index = 0;
    }

    /**
     * 初始化数据
     */
    private void init() {
        mPath = new Path();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(5.0f);
        //设置为边框而非填充
        mPaint.setStyle(Paint.Style.STROKE);

        ePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ePaint.setColor(Color.WHITE);
        ePaint.setStrokeWidth(20.0f);
        //设置为边框而非填充
        ePaint.setStyle(Paint.Style.STROKE);
        downPoint = new Point();
        movePoint = new Point();
        //设置默认为画直线
        dt = DrawType.LINE;
    }

    /**
     * 初始化画布
     */
    private void initCanvas() {
        //获取屏幕大小
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
        mWidth = metrics.widthPixels;
        mHeight = metrics.heightPixels;
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mBitmap.eraseColor(0);
        mCanves = new Canvas();
        mCanves.setBitmap(mBitmap);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int act = event.getAction();
        float x = event.getX();
        float y = event.getY();
        if (dt == DrawType.FREE) {//涂鸦，随便画
            switch (act) {
                case MotionEvent.ACTION_DOWN:
                    mPath = new Path();
                    dp = new DrawPath(mPaint, mPath);
                    mPath.moveTo(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    mPath.quadTo(mStartX, mStartY, x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    mCanves.drawPath(mPath, mPaint);
                    drawPaths.add(dp);
                    mPath = null;
                    break;
            }
            invalidate();
            mStartX = x;
            mStartY = y;
            return true;
        } else if (dt == DrawType.LINE) {//画直线
            switch (act) {
                case MotionEvent.ACTION_DOWN:
                    mPath = new Path();
                    downPoint.set((int) x, (int) y);
                    mPath.moveTo(x, y);
                    dp = new DrawPath(mPaint, mPath);
                    break;
                case MotionEvent.ACTION_MOVE://画直线时，固定上一个点为按下的点。连接移动点，就变成了直线
                    movePoint.set((int) x, (int) y);
                    mPath.setLastPoint(downPoint.x, downPoint.y);
                    mPath.lineTo(x, y);
                    break;
                case MotionEvent.ACTION_UP://抬起时绘制，并重置路径
                    mCanves.drawPath(mPath, mPaint);
                    drawPaths.add(dp);
                    mPath = null;
                    break;
            }
            invalidate();
            return true;
        } else if (dt == DrawType.ROUND) {//画圆
            switch (act) {
                case MotionEvent.ACTION_DOWN:
                    mPath = new Path();
                    downPoint.set((int) x, (int) y);
                    dp = new DrawPath(mPaint, mPath);
                    break;
                case MotionEvent.ACTION_MOVE:
                    movePoint.set((int) x, (int) y);
                    //因为是Path添加一个圆，所以需要重置路径
                    mPath.reset();
                    float radius = (float) Math.sqrt((Math.pow((movePoint.x - downPoint.x), 2)
                            + Math.pow((movePoint.y - downPoint.y), 2)));
                    //添加一个圆
                    mPath.addCircle(downPoint.x, downPoint.y, radius, Path.Direction.CCW);
                    break;
                case MotionEvent.ACTION_UP:
                    mCanves.drawPath(mPath, mPaint);
                    drawPaths.add(dp);
                    mPath = null;
                    break;
            }
            invalidate();
            return true;
        } else if (dt == DrawType.RECT) {//画矩形
            switch (act) {
                case MotionEvent.ACTION_DOWN:
                    mPath = new Path();
                    downPoint.set((int) x, (int) y);
                    dp = new DrawPath(mPaint, mPath);
                    break;
                case MotionEvent.ACTION_MOVE:
                    movePoint.set((int) x, (int) y);
                    mPath.reset();
                    drawRect();
                    break;
                case MotionEvent.ACTION_UP:
                    mCanves.drawPath(mPath, mPaint);
                    drawPaths.add(dp);
                    mPath = null;
                    break;
            }
            invalidate();
            return true;
        } else if (dt == DrawType.TRIANGLE) {//画三角形
            if (index == 0) {//第一次按下时，画直线，第二次按下并移动时画三角形
                switch (act) {
                    case MotionEvent.ACTION_DOWN:
                        mPath = new Path();
                        downPoint.set((int) x, (int) y);
                        mPath.moveTo(x, y);
                        dp = new DrawPath(mPaint, mPath);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        movePoint.set((int) x, (int) y);
                        mPath.setLastPoint(downPoint.x, downPoint.y);
                        mPath.lineTo(x, y);
                        break;
                    case MotionEvent.ACTION_UP:
                        mCanves.drawPath(mPath, mPaint);
                        drawPaths.add(dp);
                        index++;
                        mPath = null;
                        break;
                }
            } else if (index == 1) {//当为第二次点击时，完成对三角形的绘制。
                switch (act) {
                    case MotionEvent.ACTION_DOWN://第二次按下时，自动连接第一次触摸时按下的点和抬起时的点;
                        mPath = new Path();
                        mPath.moveTo(downPoint.x, downPoint.y);
                        mPath.lineTo(movePoint.x, movePoint.y);
                        mPath.lineTo(x, y);
                        mPath.lineTo(downPoint.x, downPoint.y);
                        dp = new DrawPath(mPaint, mPath);
                        break;
                    case MotionEvent.ACTION_MOVE://第二次移动时，需要重绘两条线（能力有限，所以全部重新绘制了）
                        mPath.reset();
                        mPath.moveTo(downPoint.x, downPoint.y);
                        mPath.lineTo(movePoint.x, movePoint.y);
                        mPath.lineTo(x, y);
                        mPath.lineTo(downPoint.x, downPoint.y);
                        break;
                    case MotionEvent.ACTION_UP:
                        mCanves.drawPath(mPath, mPaint);
                        drawPaths.add(dp);
                        mPath = null;
                        index = 0;
                        break;
                }
            }
            invalidate();
            return true;
        } else if (dt == DrawType.ERASER) {//橡皮擦
            switch (act) {
                case MotionEvent.ACTION_DOWN:
                    mPath = new Path();
                    mPath.moveTo(x, y);
                    dp = new DrawPath(ePaint, mPath);
                    break;
                case MotionEvent.ACTION_MOVE:
                    mPath.quadTo(mStartX, mStartY, x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    mCanves.drawPath(mPath, ePaint);
                    drawPaths.add(dp);
                    mPath = null;
                    break;
            }
            invalidate();
            mStartX = x;
            mStartY = y;
            return true;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, 0f, 0f, null);
        if (mPath != null) {
            if (dt == DrawType.ERASER) {
                canvas.drawPath(mPath, ePaint);
            } else {
                canvas.drawPath(mPath, mPaint);
            }
        }
    }

    /**
     * 根据移动的点画矩形
     */
    private void drawRect() {
        if (movePoint.x > downPoint.x && movePoint.y > downPoint.y) {
            mPath.addRect(downPoint.x, downPoint.y, movePoint.x, movePoint.y, Path.Direction.CCW);
        } else if (movePoint.x > downPoint.x && movePoint.y < downPoint.y) {
            mPath.addRect(downPoint.x, movePoint.y, movePoint.x, downPoint.y, Path.Direction.CCW);
        } else if (movePoint.x < downPoint.x && movePoint.y > downPoint.y) {
            mPath.addRect(movePoint.x, downPoint.y, downPoint.x, movePoint.y, Path.Direction.CCW);
        } else if (movePoint.x < downPoint.x && movePoint.y < downPoint.y) {
            mPath.addRect(movePoint.x, movePoint.y, downPoint.x, downPoint.y, Path.Direction.CCW);
        }
    }

    /**
     * 撤回上一步
     * 这里采用删除最后一步，并重新绘制其他所有步骤
     * 应该可以改进
     */
    public void cancel() {
        if (System.currentTimeMillis() - firstTime < 500) {
            return;
        }
        firstTime = System.currentTimeMillis();
        initCanvas();
        invalidate();
        if (!drawPaths.isEmpty() && drawPaths.size() > 0) {//判断之前是否有操作
            drawPaths.remove(drawPaths.size() - 1);
            for (int i = 0; i < drawPaths.size(); i++) {
                DrawPath drawPath = drawPaths.get(i);
                dp.paint.setColor(dp.color);
                dp.paint.setMaskFilter(dp.mf);
                dp.paint.setStrokeWidth(dp.strokeWidth);
                mCanves.drawPath(drawPath.path, drawPath.paint);
            }
            invalidate();
        }
    }

    /**
     * 清空
     */
    public void clearAll() {
        drawPaths.clear();
        initCanvas();
        invalidate();
    }

    /**
     * 设置画笔颜色
     *
     * @param color 颜色
     */
    public void setPaintColor(int color) {
        if (mPaint != null) {
            mPaint.setColor(color);
        }
    }

    /**
     * 设置画笔宽度
     *
     * @param strokeWidth 宽度
     */
    public void setPaintStrokeWidth(float strokeWidth) {
        if (mPaint != null) {
            mPaint.setStrokeWidth(strokeWidth);
        }
    }

    /**
     * 自定义了图形
     */
    public enum DrawType {
        /**
         * 画圆形
         */
        ROUND,
        /**
         * 画直线
         */
        LINE,
        /**
         * 涂鸦，随便画线
         */
        FREE,
        /**
         * 画矩形
         */
        RECT,
        /**
         * 画三角形
         */
        TRIANGLE,
        /**
         * 橡皮擦功能
         */
        ERASER,
    }

    class DrawPath {
        private int color;
        private MaskFilter mf;
        private Paint paint;
        private Path path;
        private float strokeWidth;

        public DrawPath(Paint paint, Path path) {
            this.color = paint.getColor();
            this.mf = paint.getMaskFilter();
            this.paint = paint;
            this.path = path;
            this.strokeWidth = paint.getStrokeWidth();
        }
    }
}
