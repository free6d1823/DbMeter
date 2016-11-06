package com.breeze.dbmeter;

import android.graphics.Point;
import android.graphics.RectF;
import android.util.Log;

/**
 * Created by cj on 2014/12/21.
 */
public class GuiMap {
    public class PointF{
        PointF(float x1, float y1){
            x = x1;y=y1;
        }
        float x;
        float y;
    };

    /* window dimension */
    float WinH;
    float WinW;
    private final float PIC_W = 400;
    private final float PIC_H = 716; // PNG size
    //0, 10,20 ~110
    PointF[] PowerMap1;
    PointF[] PowerMap2;
    final float FreqBarW = 37; // Bar width
    final float FreqY0 = 373; //Bar bottom
    final float FreqY1 = 245; //Bar top
    float[] FreqX;
    RectF Peak;
    RectF Average;
    RectF Max;
    RectF Reset;
    protected PointF[] mPowerMap1 = new PointF[12];
    protected PointF[] mPowerMap2 = new PointF[12];
    public float mFreqBarW; // Bar width
    public float mFreqY0; //Bar bottom
    public float mFreqY1; //Bar top
    public float[] mFreqX = new float[8];
    protected RectF mPeak;
    protected RectF mAverage;
    protected RectF mMax;
    protected RectF mReset;
    GuiMap(){

        PowerMap1 = new PointF[]{ //outer
                new PointF(46,122),new PointF(66,101),new PointF(91,84),new PointF(119,71),
                new PointF(150,61),new PointF(180,57),new PointF(210,57),new PointF(242,61),
                new PointF(273,71),new PointF(302,84),new PointF(326,101),new PointF(347,122)};
        PowerMap2 = new PointF[]{ //inner
                new PointF(132,193),new PointF(140,184),new PointF(150,175),new PointF(161,168),
                new PointF(175,163),new PointF(189,160),new PointF(203,160),new PointF(217,163),
                new PointF(230,168),new PointF(242,175),new PointF(251,184),new PointF(259,193)};

        FreqX = new float[]{ 35, 81, 127, 173, 219, 265, 312, 358};

        Peak = new RectF(20,457,111,512);
        Average = new RectF(142,443,251,524);
        Max = new RectF(282,457,373,512);
        Reset = new RectF(135,163,259,193);
    };
    public void setWindow(float w, float h)
    {
        int i;
        WinW = w;
        WinH = h;

        for(i=0;i<PowerMap1.length; i++){
            mPowerMap1[i] = new PointF(PowerMap1[i].x *w / PIC_W, PowerMap1[i].y *h / PIC_H);
            mPowerMap2[i] = new PointF(PowerMap2[i].x *w / PIC_W, PowerMap2[i].y *h / PIC_H);
        }


        mFreqBarW = FreqBarW * w /PIC_W;
        mFreqY0 = FreqY0 * h/PIC_H;
        mFreqY1 = FreqY1 * h/PIC_H;
        for(i=0;i<FreqX.length; i++)
        {
            mFreqX[i] = FreqX[i] *w /PIC_W;
        }
        mPeak = new RectF();
        mPeak.left = Peak.left * w/PIC_W;
        mPeak.top = Peak.top * h/PIC_H;
        mPeak.right = Peak.right * w/PIC_W;
        mPeak.bottom = Peak.bottom * h/PIC_H;


        mAverage = new RectF();

        mAverage.left = Average.left * w/PIC_W;
        mAverage.top = Average.top * h/PIC_H;
        mAverage.right = Average.right * w/PIC_W;
        mAverage.bottom = Average.bottom * h/PIC_H;
        mMax = new RectF();

        mMax.left = Max.left * w/PIC_W;
        mMax.top = Max.top * h/PIC_H;
        mMax.right = Max.right * w/PIC_W;
        mMax.bottom = Max.bottom * h/PIC_H;
        mReset = new RectF();
        mReset.left = Reset.left * w/PIC_W;
        mReset.top = Reset.top * h/PIC_H;
        mReset.right = Reset.right * w/PIC_W;
        mReset.bottom = Reset.bottom * h/PIC_H;

    }
    public RectF getMaxRect(){
        return mMax;
    }
    public RectF getPeakRect(){
        return mPeak;
    }
    public RectF getAverageRect(){
        return mAverage;
    }
    public boolean isInReset(float x, float y){
        if(x > mReset.left && x < mReset.right){
            if(y > mReset.top && y < mReset.bottom)
                return true;
        }
        return false;
    }
    public float[] getNiddleByPower(float db){
        float[] v = new float[4];
        int k = (int) db / 10;
        if(db <= 0) k=0;
        else if(db >=110) k=11;
        else {
            float a = db - 10 * k;

            v[0] = mPowerMap1[k].x + a * (mPowerMap1[k + 1].x - mPowerMap1[k].x) / 10;
            v[1] = mPowerMap1[k].y + a * (mPowerMap1[k + 1].y - mPowerMap1[k].y) / 10;
            v[2] = mPowerMap2[k].x + a * (mPowerMap2[k + 1].x - mPowerMap2[k].x) / 10;
            v[3] = mPowerMap2[k].y + a * (mPowerMap2[k + 1].y - mPowerMap2[k].y) / 10;

             return v;
        }
        v[0] = mPowerMap1[k].x ;
        v[1] = mPowerMap1[k].y;
        v[2] = mPowerMap2[k].x;
        v[3] = mPowerMap2[k].y;

        return v;
    }
}
