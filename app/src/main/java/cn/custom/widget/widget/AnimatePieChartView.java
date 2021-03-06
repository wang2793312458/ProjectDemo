package cn.custom.widget.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import cn.custom.widget.model.PieData;
import cn.project.demo.com.R;

/**
 * Created by chawei on 2018/5/13.
 */

public class AnimatePieChartView extends View {

    public static final String TAG = "ez";

    private final float DEFAULT_START_ANGLE = 180;
    //某块饼图平移的距离
    public static final int TRANS_DIS = -20;
    private Paint mPaintOuter;
    private Paint mPaintCenter;
    private Paint mPaintShadow;

    private int mPaddingTop;
    private int mPaddingBottom;
    private int mPaddingStart;
    private int mPaddingEnd;

    //圆心
    private int mCenterX;
    private int mCenterY;

    //开始角度
    private float mStartAngle;
    private float mMaxValue;
    //文字大小
    private float mTextSize;
    //阴影大小
    private float mShaderSize;
    //当前颜色
    private int mCurrentColor;

    private int mIndex;

    //颜色
    private List<Integer> mPieColorList;
    //颜色占比
    private List<Float> mPieValueList;
    //饼图文字
    private List<String> mPieStringList;

    //选中角度
    private List<Float> angleList;

    //半径
    private int mRadius;

    //中心圆半径
    private int circleRadius;


    //中心圆文字
    private String text;

    //中心圆颜色
    private int centerColor;

    public AnimatePieChartView(Context context) {
        super(context, null);
    }

    public AnimatePieChartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnimatePieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        //自定义一些属性
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PieChartView);

        mRadius = a.getDimensionPixelSize(R.styleable.PieChartView_pie_radius,
                getResources().getDimensionPixelSize(R.dimen.pie_default_radius));
        circleRadius = a.getDimensionPixelSize(R.styleable.PieChartView_centerCircle_radius,
                getResources().getDimensionPixelSize(R.dimen.pie_center_radius));
        mTextSize = a.getDimension(R.styleable.PieChartView_textSize,
                getResources().getDimension(R.dimen.pie_text_size));
        mShaderSize = a.getDimension(R.styleable.PieChartView_shaderSize,
                getResources().getDimension(R.dimen.pie_shader_size));

        centerColor = getResources().getColor(R.color.color_window_background);

        a.recycle();

        mPaddingTop = getPaddingTop();
        mPaddingBottom = getPaddingBottom();
        mPaddingStart = getPaddingLeft();
        mPaddingEnd = getPaddingRight();

        initPaint();
    }

    /**
     * 初始化Paint
     */
    private void initPaint() {
        mPieColorList = new ArrayList<>();
        mPieValueList = new ArrayList<>();
        mPieStringList = new ArrayList<>();
        angleList = new ArrayList<>();


        mPaintOuter = new Paint();
        mPaintOuter.setStyle(Paint.Style.FILL);
        mPaintOuter.setAntiAlias(true);

        mPaintCenter = new Paint();
        mPaintCenter.setColor(centerColor);
        mPaintCenter.setStyle(Paint.Style.FILL);
        mPaintCenter.setAntiAlias(true);
        mPaintCenter.setTextSize(mTextSize);
        mPaintCenter.setTextAlign(Paint.Align.CENTER);

        mPaintShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintShadow.setAntiAlias(true);
    }

    /**
     * 获取饼图每一部分的旋转角度
     */
    private float getRotationAngle(int i) {
        float angleR;
        float angleT = angleList.get(i);
        if (angleT <= 270f && angleT >= 90f) {
            angleR = 90f - angleT;
        } else if (angleT > 270f && angleT <= 360f) {
            angleR = 360f - angleT + 90f;
        } else if (angleT >= 0 && angleT < 90) {
            angleR = 90 - angleT;
        } else {
            angleR = 0;
        }

        for (int id = 0; id < angleList.size(); id++) {
            float temp = angleList.get(id) + angleR;
            if (temp > 360f) {
                temp -= 360f;
            } else if (temp < 0) {
                temp += 360f;
            }
            angleList.set(id, temp);
        }
        return angleR;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int mWidth = mRadius * 2 + mPaddingStart + mPaddingEnd;
        int mHeight = mRadius * 2 + mPaddingTop + mPaddingBottom;

        if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT &&
                getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(mWidth, mHeight);
        } else if (getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(mWidth, heightSize);
        } else if (getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(widthSize, mHeight);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = mPaddingStart + (w - mPaddingStart - mPaddingEnd) / 2;
        mCenterY = mPaddingTop + (h - mPaddingTop - mPaddingBottom) / 2;
        mPaintShadow.setShader(new RadialGradient(mCenterX, mCenterY,
                circleRadius + mShaderSize,
                Color.TRANSPARENT, Color.TRANSPARENT, Shader.TileMode.CLAMP));
    }

    /**
     * 旋转前的饼图
     */
    private void drawPie(Canvas canvas, float amount,int i) {
        mPaintOuter.setColor(mCurrentColor);
        float mAngle = 360  * amount / mMaxValue;
        RectF oval = new RectF(mCenterX - mRadius, mCenterY - mRadius,mCenterX + mRadius,mCenterY + mRadius);
        canvas.drawArc(oval, mStartAngle, mAngle, true, mPaintOuter);
        mStartAngle += mAngle;
    }

    /**
     * 旋转后的饼图
     */
    private void drawPieTouch(Canvas canvas, float amount,int i) {
        mPaintOuter.setColor(mCurrentColor);
        float mAngle = 360  * amount / mMaxValue;
        float mRadiusTemp = mRadius ;
        RectF oval = new RectF(mCenterX - mRadiusTemp, mCenterY - mRadiusTemp,mCenterX + mRadiusTemp,mCenterY + mRadiusTemp);
        canvas.drawArc(oval, mStartAngle + mAngle , mAngle - mAngle  * 2, true, mPaintOuter);
        mStartAngle += mAngle;
        canvas.drawText(mPieStringList.get(i),getMeasuredWidth()/2,getMeasuredHeight()/4,mPaintCenter);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < mPieValueList.size(); i++) {
            mCurrentColor = mPieColorList.get(i);
            if (i == mIndex) {
                canvas.save();
                canvas.translate(0,TRANS_DIS);
                drawPieTouch(canvas, mPieValueList.get(i),i);
                canvas.restore();
            } else {
                drawPie(canvas, mPieValueList.get(i),i);
            }
        }
        canvas.drawCircle(mCenterX, mCenterY,
                circleRadius + mShaderSize, mPaintShadow);
        mPaintCenter.setColor(centerColor);
        //中心圆
//        canvas.drawCircle(mCenterX, mCenterY, circleRadius, mPaintCenter);
//        if (text != null) {
//            mPaintCenter.setColor(Color.BLACK);
//            canvas.drawText(text, mCenterX, mCenterY + mTextSize / 2, mPaintCenter);
//        }
    }

    public void setCenterText(String text){
        this.text=text;
    }

    /**
     *
     * 旋转选中的某一项
     */
    public void onTouchPie(int i) {
        mIndex = i;
        float angle = getRotationAngle(i);
        ValueAnimator animatorRotation;
        animatorRotation = ValueAnimator.ofFloat(mStartAngle, mStartAngle + angle);
        animatorRotation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mStartAngle = (Float) animation.getAnimatedValue();
                invalidate();
            }
        });

        int time = (int) (1000 * Math.abs(angle) / 360);

        animatorRotation.setDuration(time);
        animatorRotation.start();
    }

    /**
     * 饼图的各个属性
     *
     */
    public void setPie(List<PieData> pieList) {
        mMaxValue = 0;
        mPieColorList = new ArrayList<>();
        mPieStringList = new ArrayList<>();
        mPieValueList = new ArrayList<>();
        angleList = new ArrayList<>();

        for (PieData pie : pieList) {
            mPieColorList.add(pie.pieColor);
            mPieValueList.add(pie.pieValue);
            mMaxValue += pie.pieValue;
            mPieStringList.add(pie.pieString);
        }

        float angleTemp;
        float startAngleTemp = DEFAULT_START_ANGLE;

        for (float v : mPieValueList) {
            angleTemp = 360 * v / mMaxValue;

            angleList.add(startAngleTemp + angleTemp / 2);

            startAngleTemp += angleTemp;
        }
    }

}
