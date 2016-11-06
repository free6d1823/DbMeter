package com.breeze.dbmeter;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;


/**
 * TODO: document your custom view class.
 */
public class PicView extends View  implements View.OnTouchListener {
    float mPowerDb = 0;
    double[] mFreqSpect = new double[8];
    double mMaxFreqSpect;

    float mAverage = 0;
    float mMax = 0;
    float mPeak = 0;

    protected GuiMap mGuiMap = new GuiMap();
    protected TextPaint mPaintText1; //x-axis label text
    protected TextPaint mPaintText2; //x-axis label text
    protected Paint mPaintBar; //Y-axis right label and scale
    protected Paint mPaintNeedle;
    protected Paint mPaintShadow;



    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;

    public PicView(Context context) {
        super(context);
        init(null, 0);
    }

    public PicView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public PicView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }
    @Override
    public boolean onTouch(View v, MotionEvent event){

        if(event.getAction() == MotionEvent.ACTION_UP) {
            if (mGuiMap.isInReset(event.getX(), event.getY())) {
                final Intent intent = new Intent();
                intent.setAction(MainActivity.RESET_RECORD);
                getContext().sendBroadcast(intent);
            }
        }
        return true; //True if the listener has consumed the event, false otherwise.
    }
    private void init(AttributeSet attrs, int defStyle) {

        setOnTouchListener(this);

        mPaintText1 = new TextPaint();
        mPaintText1.setColor(0xeeffaa44);
        mPaintText1.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaintText1.setTextAlign(Paint.Align.RIGHT);
        mPaintText2 = new TextPaint();
        mPaintText2.setColor(0xeeffaa44);
        mPaintText2.setFlags(Paint.ANTI_ALIAS_FLAG);
        mPaintText2.setTextAlign(Paint.Align.RIGHT);

        mPaintBar = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintBar.setColor(0xaaffaa00);
        mPaintBar.setStyle(Paint.Style.FILL);

        mPaintNeedle = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintNeedle.setColor(0xeeaa1111);
        mPaintNeedle.setStyle(Paint.Style.FILL);
        mPaintNeedle.setStrokeWidth(4);
        mPaintShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintShadow.setColor(0x80787878);
        mPaintShadow.setStyle(Paint.Style.FILL);
        mPaintShadow.setStrokeWidth(5);


    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawNiddle(canvas);
        drawFreq(canvas);
        drawNumbers(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        //Measure Width
        if (widthMode == MeasureSpec.EXACTLY || widthMode == MeasureSpec.AT_MOST) {
            //Must be this size
            width = widthSize;
        } else {
            width = -1;
        }

        //Measure Height
        if (heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST) {
            //Must be this size
            height = heightSize;
        } else {
            height = -1;
        }

        mGuiMap.setWindow(width, height);
        RectF rc= mGuiMap.getMaxRect();

        mPaintText2.setStrokeWidth(rc.height()*10/100);
        mPaintText2.setTextSize(rc.height()*95/100);
        mPaintText1.setStrokeWidth(rc.height()*10/100);

        rc= mGuiMap.getAverageRect();

        mPaintText1.setTextSize(rc.height()*95/100);

        mPaintBar.setStrokeWidth(mGuiMap.mFreqBarW);

        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    void drawNiddle(Canvas canvas){
        float[] line = mGuiMap.getNiddleByPower(mPowerDb);
        float[] line2 = new float[4]; /* shadow */
        line2[0] = line[0]+ 7;
        line2[1] = line[1] + 3;
        line2[2] = line[2] + 7;
        line2[3] = line[3] + 3;

        canvas.drawLines(line2, mPaintShadow);
        canvas.drawLines(line, mPaintNeedle);

    }

    void drawFreq(Canvas canvas){
        double dy = (mGuiMap.mFreqY1 - mGuiMap.mFreqY0)/mMaxFreqSpect; //neg

        for(int i=0;i<mGuiMap.mFreqX.length; i++){
            canvas.drawLine(mGuiMap.mFreqX[i],mGuiMap.mFreqY0,
                    mGuiMap.mFreqX[i],
                    (float)(mGuiMap.mFreqY0 + mFreqSpect[i]* dy), mPaintBar);
        }

    }

    void drawNumbers(Canvas canvas){
        RectF rc= mGuiMap.getMaxRect();
        float y = rc.bottom;
        canvas.drawText( Integer.toString((int)mMax) , (float) (rc.left + rc.width()*0.8), y, mPaintText2);
        rc= mGuiMap.getAverageRect();
        canvas.drawText( Integer.toString((int)mAverage) , (float) (rc.left + rc.width()*0.8), y, mPaintText1);
        rc= mGuiMap.getPeakRect();
        canvas.drawText( Integer.toString((int)mPeak) , (float) (rc.left + rc.width()*0.8), y, mPaintText2);

    }

    public void setPowerDb(float db){
        mPowerDb = db;
    }
    public void setFreqSeries(double[] freqs){
        mFreqSpect = freqs;


        double ymax = 0;
        for(int i=0;i< freqs.length; i++) {
            if (freqs[i] > ymax) ymax = freqs[i];
        }
        mMaxFreqSpect = findYmaxScale(ymax);
    }

    protected double findYmaxScale(double y){
        int b = 1;
        double t =  y;
        double max;

        while(t>=10)
        {
            b = b*10;
            t = t/10;
        }

        if(t>5)
            max = 10*b;
        else if(t>2)
            max = 5*b;
        else if(t>1)
            max = 2*b;
        else
            max = b;
        if(max < 100000) max = 100000;
        return max;
    }

    public void setAverage(float avg){
        mAverage = avg;
    }
    public void setMax(float max){mMax = max;}
    public void setPeak(float peak){mPeak = peak;}

}
