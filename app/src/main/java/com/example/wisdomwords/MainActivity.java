package com.example.wisdomwords;

import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import techpaliyal.com.curlviewanimation.*;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private Gpio mButtonGpio;
    private Gpio mSensorGpio;
    private Boolean mSensorLaunched = Boolean.FALSE;
    private int pageNumber = 1;
    private int pageTotal = 3;
    Boolean dirFlip = Boolean.TRUE;//go forward
    long preTime = 0;
    ImageView imageLeft;
    CurlView curlRightView;
    int currIdx;
    private static final String VIDEO_SAMPLE_01 = "open_book";
    private static final String VIDEO_SAMPLE_02 = "close_book";
    private VideoView mVideoView;
    // Current playback position (in milliseconds).
    private int mCurrentPosition = 0;
    Boolean mOpeningVideoPlaying = Boolean.FALSE;
    Boolean isPlayingClosingVideo = Boolean.FALSE;
    private Handler handler=null;
    private ImageView bookCoverImg,aphorism;
    private int iIsAphorismAnimShowed = 0;
    int identifier;
    String aphorismImgStr;
    private int countToMainMenu = 0;
    private int countToAphorism = 0;
    private int showMainMenuBySensor = 0;
    private int secondsToMainMenu = 1*60;//10*60;
    private int secondsToAphorism = 6;
    private int secondsToAllowButtonIn = 12;//10*60;
    private int msToIgnoreSensor = 10*1000;
    private ButtonInputDriver mButtonInputDriver;

    Animation am;// = AnimationUtils.loadAnimation(this, R.anim.anim);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();

        setMainMenu();

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(Boolean.TRUE){
                    //Log.i(TAG, "detect event of showing aphorism");

                    if(mOpeningVideoPlaying == Boolean.TRUE){
                        Log.i(TAG, "counting to show aphorism, countToAphorism = " + countToAphorism);
                        countToAphorism++;
                    }

                    if(countToAphorism == secondsToAphorism){
                        Log.i(TAG, "handler post :: playAphorismAnim");
                        iIsAphorismAnimShowed = 0;
                        handler.post(playAphorismAnim);
                        countToAphorism = secondsToAphorism + 1;
                        //mVideoPlaying = Boolean.FALSE;
                    }

                    if(countToAphorism > secondsToAphorism){
                        countToAphorism = secondsToAphorism + 1;
                    }

                    if(iIsAphorismAnimShowed == 1){
                        Log.i(TAG, "countToMainMenu::"+countToMainMenu);
                        countToMainMenu++;
                    }

                    if(countToMainMenu == (secondsToMainMenu-21)) {//ready to back main menu which means to go to screen saver.
                        Log.i(TAG, "handler post :: playClosingVideo,countToMainMenu::"+countToMainMenu);
                        //handler.post(playClosingVideo);//use a handle post to play closing video
                        //isPlayingClosingVideo = Boolean.TRUE;
                    }

                    if(countToMainMenu == (secondsToMainMenu-15)) {//ready to back main menu which means to go to screen saver.
                        Log.i(TAG, "handler post :: closeAphorism,countToMainMenu::"+countToMainMenu);
                        handler.post(closeAphorism);//use a handle post to play closing video
                    }

                    if(countToMainMenu == secondsToMainMenu){//ready to back main menu which means to go to screen saver.
                        Log.i(TAG, "handler post :: handleShowMainMenu,countToMainMenu::"+countToMainMenu);
                        showMainMenuBySensor = 0;
                        handler.post(handleShowMainMenu);
                        countToMainMenu = secondsToMainMenu + 1;
                    }

                    if(countToMainMenu > secondsToMainMenu){
                        countToMainMenu = secondsToMainMenu + 1;
                    }

                    try {
                        Thread.sleep( 1000 );
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        gpioControl();//BCM21
        buttonControl();//BCM20
    }

    Runnable   handleShowMainMenu = new  Runnable(){
        @Override
        public void run() {
            setMainMenu();
        }
    };

    private void setMainMenu(){
        setContentView(R.layout.fullscreen);
        iIsAphorismAnimShowed = 0;//to forbid button input
        aphorism = (ImageView)findViewById(R.id.imageView_aphorism_anim_done);
        bookCoverImg = (ImageView)findViewById(R.id.imageView_aphorism);
        mVideoView = (VideoView)findViewById(R.id.videoView01);
        bookCoverImg.setImageResource(R.drawable.cover);
        if(showMainMenuBySensor == 1){
            Log.i(TAG, "skip to show MainMenu because of coming from sensor");
            bookCoverImg.setVisibility(View.INVISIBLE);
        }else{
            bookCoverImg.setVisibility(View.VISIBLE);
        }

    }

    private void gpioControl(){
        try {
            String pinName = "BCM21";//vibrator 1
            mSensorGpio = PeripheralManager.getInstance().openGpio(pinName);
            mSensorGpio.setDirection(Gpio.DIRECTION_IN);
            mSensorGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
            Log.i(TAG, "set gpio :: BCM21");
            mSensorGpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    Log.i(TAG, "GPIO changed, signal coming");
                    if( mSensorLaunched == Boolean.FALSE){
                        if((SystemClock.uptimeMillis()-preTime)> msToIgnoreSensor) {
                            preTime = SystemClock.uptimeMillis();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    mOpeningVideoPlaying = Boolean.FALSE;
                                    Log.i(TAG, "mOpeningVideoPlaying = " + mOpeningVideoPlaying + ",countToAphorism = " + countToAphorism+",countToMainMenu = "+countToMainMenu);
                                    if(mOpeningVideoPlaying == Boolean.FALSE ){
                                        //can't play video here ,
                                        // would cause Only the original thread that created a view hierarchy can touch its views
                                        showMainMenuBySensor = 1;
                                        handler.post(handleShowMainMenu);//just restore context view and not show main menu
                                        handler.post(playFlippingVideo);//use a handle post to play video
                                        mOpeningVideoPlaying = Boolean.TRUE;
                                    }
                                }
                            }).start();
                        }
                    }
                    return true;
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
    }

    private void buttonControl(){
        try {
            String buttonPinName = "BCM20";//vibrator 1
            mButtonGpio = PeripheralManager.getInstance().openGpio(buttonPinName);
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            Log.i(TAG, "set gpio :: BCM20");
            mButtonGpio.setActiveType(Gpio.ACTIVE_HIGH);
            mButtonGpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    try {
                        Log.i(TAG, "button pressed."+ gpio.getValue());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(iIsAphorismAnimShowed == 1){
                        Log.i(TAG, "handler post :: handleShowMainMenu by button");
                        showMainMenuBySensor = 0;
                        handler.post(handleShowMainMenu);
                    }
                    return true;
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
    }

    Runnable   playFlippingVideo=new  Runnable(){
        @Override
        public void run() {
            initializePlayer();
        }
    };

    Runnable   closeAphorism = new  Runnable(){
        @Override
        public void run() {
            hideAphorisms();//aphorism.setVisibility(View.INVISIBLE);
        }
    };

    Runnable   playClosingVideo = new  Runnable(){
        @Override
        public void run() {
            //countToMainMenu = 0;
            closePlayer();
        }
    };

    private void showLeftImg() {
        currIdx = curlRightView.getCurrentIndex() + 2;
        String leftImgStr = "aphorisms0"+currIdx+"_"+"l";
        int identifier = getResources().getIdentifier(leftImgStr, "drawable","com.example.wisdomwords");
        ImageView image = (ImageView) findViewById(R.id.imageView1);
        image.setImageResource(identifier);
        Log.i(TAG, "currIdx:"+currIdx+",leftImgStr="+leftImgStr);
    }

    private void initializePlayer() {

        // Show the "Buffering..." message while the video loads.
        MediaController controller = new MediaController(this);
        controller.setMediaPlayer(mVideoView);
        mVideoView.setMediaController(null);
        // Buffer and decode the video sample.
        Uri videoUri = getMedia(VIDEO_SAMPLE_01);

        Log.i(TAG, "start to play video Uri = "+videoUri);
        mVideoView.setVideoURI(videoUri);

        // Listener for onPrepared() event (runs after the media is prepared).
        mVideoView.setOnPreparedListener(
                new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {

                        // Hide buffering message.
                        //mBufferingTextView.setVisibility(VideoView.INVISIBLE);
                        // Hide book cover
                        bookCoverImg.setVisibility(View.INVISIBLE);
                        aphorism.setVisibility(View.INVISIBLE);
                        mVideoView.setVisibility(VideoView.VISIBLE);
                        // Restore saved position, if available.
                        if (mCurrentPosition > 0) {
                            mVideoView.seekTo(mCurrentPosition);
                        } else {
                            // Skipping to 1 shows the first frame of the video.
                            mVideoView.seekTo(1);
                        }
                        // Start playing!
                        Log.i(TAG, "onPrepared::playing flipping video. set countToAphorism = " + countToAphorism);
                        mVideoView.start();
                        countToAphorism = 0;
                    }
                });

        // Listener for onCompletion() event (runs after media has finished
        // playing).
        mVideoView.setOnCompletionListener(
                new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        Log.i(TAG, "onCompletion::playing flipping video.");
                    }
                });
    }

    private void closePlayer() {
        // Show the "Buffering..." message while the video loads.
        MediaController controller = new MediaController(this);
        controller.setMediaPlayer(mVideoView);
        mVideoView.setMediaController(null);
        // Buffer and decode the video sample.
        Uri videoUri = getMedia(VIDEO_SAMPLE_02);

        Log.i(TAG, "start to play closing video Uri = "+videoUri);
        mVideoView.setVideoURI(videoUri);

        // Listener for onPrepared() event (runs after the media is prepared).
        mVideoView.setOnPreparedListener(
                new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {

                        // Hide buffering message.
                        //mBufferingTextView.setVisibility(VideoView.INVISIBLE);
                        //aphorism.setVisibility(View.INVISIBLE);
                        mVideoView.setVisibility(VideoView.VISIBLE);
                        // Restore saved position, if available.
                        if (mCurrentPosition > 0) {
                            mVideoView.seekTo(mCurrentPosition);
                        } else {
                            // Skipping to 1 shows the first frame of the video.
                            mVideoView.seekTo(1);
                        }
                        // Start playing!
                        Log.i(TAG, "start to play flipping video and start to count to show!!");
                        mVideoView.start();
                    }
                });

        // Listener for onCompletion() event (runs after media has finished
        // playing).
        mVideoView.setOnCompletionListener(
                new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        //Toast.makeText(MainActivity.this,                                R.string.toast_message,                                Toast.LENGTH_SHORT).show();
                        // Return the video position to the start.
                        //mVideoView.seekTo(mVideoView.getDuration());
                        //initializePlayer(Boolean.FALSE);
                        //mVideoPlaying = Boolean.FALSE;
                        isPlayingClosingVideo = Boolean.FALSE;
                        //mVideoView.setVisibility(VideoView.INVISIBLE);
                        //handler.post(playAphorismAnim);
                        Log.i(TAG, "closing book done.");
                    }
                });
    }

    Runnable   playAphorismAnim = new Runnable(){
        @Override
        public void run() {
            if(iIsAphorismAnimShowed == 0){
                Log.i(TAG, "Runnable::showAphorisms.");
                showAphorisms();
            }else{
                Log.i(TAG, "Error::doesn't catch up Aphorisms handle!!");
            }
        }
    };

    private void showAphorisms(){

        am = AnimationUtils.loadAnimation(this, R.anim.anim);
        aphorismImgStr = getAphorismImg();
        identifier = getResources().getIdentifier(aphorismImgStr, "drawable","com.example.wisdomwords");
        Log.i(TAG, "show aphorismImgStr:"+aphorismImgStr+",identifier:"+identifier);
        //identifier=R.drawable.aphorisms_01;
        aphorism.setImageResource(identifier);
        aphorism.setVisibility(VideoView.VISIBLE);
        Log.i(TAG, "start aphorism animation");
        aphorism.startAnimation(am);
        am.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation arg0) {

            }
            @Override
            public void onAnimationRepeat(Animation arg0) {
            }
            @Override
            public void onAnimationEnd(Animation arg0) {
                //aphorism.setVisibility(View.INVISIBLE);
                //aphorism.setVisibility(VideoView.INVISIBLE);
                countToMainMenu = 0;
                iIsAphorismAnimShowed = 1;
                Log.i(TAG, "aphorism animation done and reset countToMainMenu = " + countToMainMenu);
                //aphorism.clearAnimation();
                //am.cancel();
                //am.reset();
                //aphorism.setImageResource(identifier);//backToMainMenu();
            }
        });
    }

    private void hideAphorisms(){
        am = AnimationUtils.loadAnimation(this, R.anim.anim_hide);
        Log.i(TAG, "hide aphorism animation");
        aphorism.startAnimation(am);
        am.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation arg0) {

            }
            @Override
            public void onAnimationRepeat(Animation arg0) {
            }
            @Override
            public void onAnimationEnd(Animation arg0) {
                //aphorism.setVisibility(View.INVISIBLE);
                //aphorism.setVisibility(VideoView.INVISIBLE);
                //countToMainMenu = 0;
                aphorism.setVisibility(VideoView.INVISIBLE);
                Log.i(TAG, "hiding aphorism animation done");
                //aphorism.clearAnimation();
                //am.cancel();
                //am.reset();
                //aphorism.setImageResource(identifier);//backToMainMenu();
            }
        });
    }

    private String getAphorismImg(){
        int min = 1;
        int max = 100;

        Random r = new Random();
        int i1 = r.nextInt(max - min + 1) + min;
        String rtnImgStr = null;
        if(i1<10){
            rtnImgStr = "aphorisms_00"+i1;
        }else if(i1>=10 && i1 < 100){
            rtnImgStr = "aphorisms_0"+i1;
        }else{
            rtnImgStr = "aphorisms_"+i1;
        }

        return rtnImgStr;
    }

    // Get a Uri for the media sample regardless of whether that sample is
    // embedded in the app resources or available on the internet.
    private Uri getMedia(String mediaName) {
        String fileName = null;
        if(mediaName == VIDEO_SAMPLE_01) {
            fileName = "android.resource://" + getPackageName() + "/" + R.raw.video;
        }else if(mediaName == VIDEO_SAMPLE_02) {
            fileName = "android.resource://" + getPackageName() + "/" + R.raw.close_book;
        }
        return Uri.parse(fileName);
    }

    private void flipBook(boolean direction){
        CurlView clvw = findViewById(R.id.curlView);
        // Obtain MotionEvent object
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;
        int metaState = 0;
        Display mdisp = getWindowManager().getDefaultDisplay();
        Point mdispSize = new Point();
        mdisp.getSize(mdispSize);
        int maxX = mdispSize.x-10;
        int maxY = mdispSize.y-10;
        int steps = 60;
        float oneStepX = 0.0f;
        float oneStepY = 0.0f;
        int i = 0;
        float factX = (float)1.0;
        float factY = (float)1.0;
        float x = 0.0f;
        float y = 0.0f;

        if(direction){
            x = maxX;
            y = maxY;
        } else {
            x = 0.0f;
            y = 0.0f+maxY;
        }
// List of meta states found here:     developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
        MotionEvent motionEventDown = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                x,
                y,
                metaState
        );
        // Dispatch touch event to view
        clvw.dispatchTouchEvent(motionEventDown);
        //mInst.sendPointerSync(motionEventDown);
        Log.i(TAG, "Down,Direction:"+direction + "(x:Y)" + x +":"+y);

        oneStepX = maxX*factX/steps;
        oneStepY = maxX*factY/steps;

        if(direction){
            x = maxX;
            y = maxY;
        } else {
            x = 0.0f;
            y = 0.0f+maxY;
        }
        Log.i(TAG, "oneStep::(x,y)="+oneStepX+","+oneStepY);
// List of meta states found here:     developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
        metaState = 0;
        for(i = 0; i < steps ; i++){
            eventTime = SystemClock.uptimeMillis();
            x = x - oneStepX;
            y = y - oneStepY;
            MotionEvent motionEventMove = MotionEvent.obtain(
                    downTime,
                    eventTime,
                    MotionEvent.ACTION_MOVE,
                    x,
                    y,
                    metaState
            );
            // Dispatch touch event to view
            clvw.dispatchTouchEvent(motionEventMove);
            //mInst.sendPointerSync(motionEventMove);
            //Log.i(TAG, "Move,Direction:"+direction + "(x:Y)" + x +":"+y);
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Log.d(TAG,"error");
            }
        }
// List of meta states found here:     developer.android.com/reference/android/view/KeyEvent.html#getMetaState()
        metaState = 0;
        eventTime = SystemClock.uptimeMillis();
        MotionEvent motionEventUp = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                metaState
        );
        // Dispatch touch event to view
        clvw.dispatchTouchEvent(motionEventUp);
        //mInst.sendPointerSync(motionEventUp);
        Log.i(TAG, "Up,Direction:"+direction + "(x:Y)" + x +":"+y);
    }
}
