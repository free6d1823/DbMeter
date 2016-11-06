package com.breeze.dbmeter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.media.MediaRecorder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;


public class MainActivity extends Activity{
    static final boolean mAdMob = true; /* TRUE for release version*/

    final int FFT_ORDER = 1024*4;

    final int mMinBuffer = FFT_ORDER;

    final double VOICE_ACTIVE_LEVEL = 60;
    PicView mPicView = null;
    private AudioRecord mRecorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    private double mFreqBuffer[] = null;
    private double mDummyBuffer[] = null;
    private FFT mFft = null;
    int mSampleRate;
    public double mDbAmp = 0;
    short mBuffer[];
    private final int POST_START_COUNT = 13;
    private int postStartCount = POST_START_COUNT; /* delay record after reset */




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUi();
    }

    @Override
    public void onPause() {
        super.onPause();

        stopRecording();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume(){
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UPDATE_WAVE);
        filter.addAction(RESET_RECORD);
        registerReceiver(mReceiver, filter);
        startRecording();

    }
    void initUi(){
        mPicView = (PicView) findViewById(R.id.picView);
        initCallbacks();
        /************************/

        AdView mAdView = (AdView) findViewById(R.id.adView2);

        if(mAdView != null) {
            AdRequest adRequest;
            //
            if (!mAdMob) {
                adRequest = new AdRequest.Builder().
                        addTestDevice("905709C5D9C17913FB3B6A40C19C0640")
                        .build();
            }
            else{
                adRequest = new AdRequest.Builder().
                        build();
            }

            mAdView.loadAd(adRequest);
        }
    }
    private boolean createAudioRecord(){
//        for (int sampleRate : new int[] { 48000, 44100, 32000, 16000, 8000 }) {
        for (int sampleRate : new int[] { 32000, 16000, 8000 }) {
            // Try to initialize
            try {

                mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        mMinBuffer);

                if (mRecorder.getState() == AudioRecord.STATE_INITIALIZED) {
                    /* take mMinBuffer*/
                    mFft = new FFT(mMinBuffer);

                    mBuffer = new short[mMinBuffer];
                    mDummyBuffer = new double[mMinBuffer];
                    mFreqBuffer = new double[mMinBuffer];
                    for( int n=0; n < mDummyBuffer.length; n++){
                        mDummyBuffer[n]=0;
                        mFreqBuffer[n] = 0;
                    }

                    mSampleRate = sampleRate;

                    return true;
                }
                mRecorder.release();
                mRecorder = null;
            } catch (Exception e) {
                // Do nothing
            }
        }
        return false;
    }
    double mMainFreq = 0;
    double mMaxAmp = 0;
    double mPeakAmp = 0;
    double mAvgAmp = 0;
    long   mAvgCount = 0;
     double mLatchAmp = 0;

    private void startRecording() {
        Log.d("Main", "startRecording");

        if(mRecorder == null) {
            if (!createAudioRecord()) {
                Toast.makeText(this, "Cannot create recorder!!", Toast.LENGTH_LONG).show();
                return;
            }
        }
        postStartCount = POST_START_COUNT;

        mRecorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
Log.d("Main", "recordingThread");
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    boolean mMainFreqChanged = false;
    private void writeAudioDataToFile() {

        while (isRecording) {
            // gets the voice output from microphone to byte format

            mRecorder.read(mBuffer, 0, mMinBuffer);

            int max=0;
            int total = 0;
            int value;
            double sqr = 0;
            for(int i=0;i<mMinBuffer; i++){
                value = mBuffer[i];
                if(value <0) value = -value;
                total += value;
                sqr += (value*value);
                if(value > max) max = value;
                mFreqBuffer[i] = (double)mBuffer[i]/655.0;
                //mDummyBuffer[i] = Math.cos(2*Math.PI* (i%48)/48);
                //mFreqBuffer[i] = Math.cos(2*Math.PI* (i%48)/48);

            }

            //avg = total/mMinBuffer;
            sqr = sqr/mMinBuffer;


            mDbAmp = calabrate ( Math.log(sqr)*2);
            if( mDbAmp > mLatchAmp) {
                /* delay first MAX count, could be fingure noise after start */
                if(postStartCount > 0)
                    postStartCount --;
                else {
                    mLatchAmp = mDbAmp;
                    mMaxAmp = mDbAmp;
                    if (mMaxAmp > mPeakAmp)
                        mPeakAmp = mMaxAmp;
                }
            }
            else
                mLatchAmp = mLatchAmp*0.99;
            mAvgCount ++;
            mAvgAmp = (mAvgAmp * (mAvgCount-1) + mDbAmp )/mAvgCount;
            /*calculate FFT */
            //mFft.fft(mFreqBuffer, mDummyBuffer);
            mFft.dct(mFreqBuffer);
            int iMax = -1;
            double th = VOICE_ACTIVE_LEVEL;
            mDummyBuffer[0]= 0; /* for band spectrum */
            for(int i=0; i<mMinBuffer/2; i++){
                mDummyBuffer[i] = mFreqBuffer[i];
                if(Math.abs(mFreqBuffer[i]) > th){
                    th = Math.abs(mFreqBuffer[i]);
                    iMax = i;
                }
            }
            mMainFreq = (mSampleRate *iMax/mMinBuffer);


            final Intent intent = new Intent(UPDATE_WAVE);
            sendBroadcast(intent);


        }

Log.d("Main", "exit thread");
    }

    private void stopRecording() {
        // stops the recording activity

        if (null != mRecorder) {
            isRecording = false;
            mRecorder.stop();
            mRecorder.release();
            Log.d("Main", "stopRecording");
            mRecorder = null;

            recordingThread = null;
        }
    }
    BroadcastReceiver mReceiver = null;
    static final String UPDATE_WAVE = "update_view";
    static final String RESET_RECORD = "reset_record";


    void initCallbacks(){
        mReceiver = new BroadcastReceiver(){

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (UPDATE_WAVE.equals(action)) {

                    mPicView.setPowerDb((float) mDbAmp);

                    mPicView.setAverage((float) mAvgAmp);
                    mPicView.setPeak((float) mPeakAmp);
                    mPicView.setMax((float) mMaxAmp);
                    double[] bands = getBandSpectrum(mDummyBuffer);
                    mPicView.setFreqSeries(bands);


                    //stopRecording();
                    mPicView.invalidate();

                }
                if (RESET_RECORD.equals(action)) {
                    mMainFreq = 0;
                    mMaxAmp = 0;
                    mPeakAmp = 0;
                    mAvgAmp = 0;
                    mAvgCount = 0;
                    mLatchAmp = 0;
                    if(isRecording ==false)
                        return;
                    stopRecording();
Log.d("Main", "Reset");


                    startRecording();
                     mPicView.invalidate();
                }

            }

        };
    }
    final double[] mBandUpper = {44.19, 88.39, 176.78, 353.55, 707.11, 1414.21, 2828.43, 24000};
    final int FREQ_BANDS = 8;
    private int[] mBandUpperX = null;
    private void initBounds(){
        mBandUpperX = new int[FREQ_BANDS];
        int i;
        for( i=0;i < FREQ_BANDS-1; i++) {
            mBandUpperX[i] = (int) mBandUpper[i] * 2 * mMinBuffer/ mSampleRate;

        }
        mBandUpperX[i]= mMinBuffer; /* the other belong to last band */
    }

    private double[] getBandSpectrum(double[] x){
        if(mBandUpperX == null)
            initBounds();
        double[] y = new double[FREQ_BANDS];
        int j=0;
        for(int i=0;i<FREQ_BANDS; i++){
            y[i] = 0;
            while(j<mBandUpperX[i]) {
                y[i] += (x[j]*x[j]);
                j++;
            }
            //mPowerBands[i] = Math.log(mPowerBands[i]);
            if(y[i]<0)y[i] = 0;
            //mPowerBands[i] *= mScaleDb;

        }
        return y;
    }
    ////////////////////////////////////////////////
    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }
    ////////////////////////////////////////////////
    class MatchNote{
        int note; //0=C
        int scale; //0=C0
        double error; //frequency error %
    }

    String[] NoteName = {"C", "C#/Db", "D", "D#/Eb", "E","F",
            "F#/Gb", "G", "G#/Ab", "A", "A#/Bb", "B"};
    double[] FreqTable = {16.35, 17.32, 18.35, 19.45, 20.60, 21.83,
            23.12, 24.5, 25.96, 27.5, 29.14, 30.87, 32.70	};
    String getNoteName(MatchNote m){
        return NoteName[m.note]+m.scale;
    }
    MatchNote findMatchNote(double freq){
        MatchNote m = new MatchNote();
        final double C1 = 32.70;
        if (freq < FreqTable[0])
            return null;
        int scale = 0;
        while(freq >= C1){
            scale ++;
            freq /= 2;
        }

        System.out.print("fREQ = "+ freq);

        m.scale = scale; //C0 is the lowest
        double e;
        int i;
        for (i=1; i< 11; i++)
        {
            if(freq <= FreqTable[i])
                break;
        }
        e = (freq - FreqTable[i])*100/(FreqTable[i]- FreqTable[i-1]);
        if( e < -50.0){
            i --;
            e = 100 + e;
        }else if(e>50){
            e = 100 - e; //inc one scale
            i++;
            if(i>=12) {
                m.scale++;
                i = 0;
            }
        }

        m.note = i;
        m.error = e;
        return m;
    }

    /* maps x(0~50) to y(0~110) */
    double mCal[] = null;
    void initCal(){
        mCal = new double[6];
        mCal[0] = 0;
        mCal[1] = 20;
        mCal[2] = 40;
        mCal[3] = 70;
        mCal[4] = 105;
        mCal[5] = 110;

    }
    double calabrate(double x){
        double y;
        int k;
        if(mCal == null)
            initCal();

        if(x <= 0) k=0;
        else if(x >=110) k=5;
        else {
            k = (int)(x/10);
            double a = x - 10 * k;
            y = mCal[k] + a * (mCal[k + 1] - mCal[k]) / 10;
            return y;
        }
        return mCal[k];



    }
}
