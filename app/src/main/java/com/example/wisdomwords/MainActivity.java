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
    private Boolean mSensorLaunched = Boolean.FALSE;
    private int pageNumber = 1;
    private int pageTotal = 3;
    Boolean dirFlip = Boolean.TRUE;//go forward
    long preTime = 0;
    ImageView imageLeft;
    CurlView curlRightView;
    int currIdx;
    private static final String VIDEO_SAMPLE_01 = "video_01.mp4";
    private VideoView mVideoView;
    // Current playback position (in milliseconds).
    private int mCurrentPosition = 0;
    Boolean mVideoPlaying = Boolean.FALSE;
    private Handler handler=null;
    private ImageView bookCoverImg,aphorism;
    private int iIsAphorismAnimShowed = 0;
    int identifier;
    String aphorismImgStr;
    private int countToMainMenu = 0;
    private int countToAphorism = 0;
    private int showMainMenuBySensor = 0;
    Animation am;// = AnimationUtils.loadAnimation(this, R.anim.anim);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setMainMenu();
        handler=new Handler();

        //ArrayList<Integer> arrImages=new ArrayList<Integer>();
        //arrImages.add(R.drawable.aphorisms01_r);
        //arrImages.add(R.drawable.aphorisms01);
        //arrImages.add(R.drawable.aphorisms02_r);
        //arrImages.add(R.drawable.aphorisms03_r);
        //curlRightView = (CurlView)findViewById(R.id.curlView);
        //new CurlActivity(this).load(curlRightView,arrImages);
        //Log.i(TAG, "showing images.");
        //mVideoView = (VideoView)findViewById(R.id.videoView01);
        // Set up the media controller widget and attach it to the video view.
        //MediaController controller = new MediaController(this);
        //controller.setMediaPlayer(mVideoView);
        //mVideoView.setMediaController(null);
        gpioControl();//BCM21

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(Boolean.TRUE){
                    //Log.i(TAG, "detect event of showing aphorism");
                    if(iIsAphorismAnimShowed == 1){
                        Log.i(TAG, "countToMainMenu::"+countToMainMenu);
                        countToMainMenu++;
                    }

                    if(countToMainMenu >= 60){//ready to back main menu which means to go to screen saver.
                        showMainMenuBySensor = 0;
                        handler.post(handleShowMainMenu);
                        countToMainMenu = 0;
                        iIsAphorismAnimShowed = 0;
                    }
                    if(mVideoPlaying == Boolean.TRUE){
                        countToAphorism++;
                    }

                    if(countToAphorism >= 6){
                        Log.i(TAG, "Ready to send Aphorisms");
                        iIsAphorismAnimShowed = 0;
                        handler.post(playAphorismAnim);
                        countToAphorism = 0;
                    }

                    try {
                        Thread.sleep( 1000 );
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    Runnable   handleShowMainMenu=new  Runnable(){
        @Override
        public void run() {
            setMainMenu();
        }
    };

    private void setMainMenu(){
        setContentView(R.layout.fullscreen);
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
            mButtonGpio = PeripheralManager.getInstance().openGpio(pinName);
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
            Log.i(TAG, "set gpio :: BCM21");
            mButtonGpio.registerGpioCallback(new GpioCallback() {
                @Override
                public boolean onGpioEdge(Gpio gpio) {
                    Log.i(TAG, "GPIO changed, signal coming");
                    if( mSensorLaunched == Boolean.FALSE){
                        if((SystemClock.uptimeMillis()-preTime)>2000) {
                            preTime = SystemClock.uptimeMillis();
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Log.i(TAG, "Sensor is launched and flip::" + pageNumber + "," + dirFlip+","+preTime);
                                    if(mVideoPlaying == Boolean.FALSE){
                                        mVideoPlaying = Boolean.TRUE;
                                        //can't play video here ,
                                        // would cause Only the original thread that created a view hierarchy can touch its views
                                        showMainMenuBySensor = 1;
                                        countToAphorism= 0;
                                        handler.post(handleShowMainMenu);
                                        handler.post(playFlippingVideo);//use a handle post to play video
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

    Runnable   playFlippingVideo=new  Runnable(){
        @Override
        public void run() {
            initializePlayer();
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
                        //iIsAphorismAnimShowed = 0;
                        mVideoPlaying = Boolean.FALSE;
                        //mVideoView.setVisibility(VideoView.INVISIBLE);
                        //handler.post(playAphorismAnim);
                        Log.i(TAG, "Send playing aphorism and Waiting for next playing.");
                    }
                });
    }

    Runnable   playAphorismAnim=new  Runnable(){
        @Override
        public void run() {
            if(iIsAphorismAnimShowed == 0){
                Log.i(TAG, "Runnable::showAphorisms.");
                showAphorisms();
                iIsAphorismAnimShowed = 1;
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
                Log.i(TAG, "aphorism animation done, countToMainMenu="+countToMainMenu);
                //aphorism.clearAnimation();
                //am.cancel();
                //am.reset();
                //aphorism.setImageResource(identifier);//backToMainMenu();
            }
        });
    }

    private String getAphorismImg(){
        int min = 1;
        int max = 3;

        Random r = new Random();
        int i1 = r.nextInt(max - min + 1) + min;
        String rtnImgStr = null;
        if(i1<10){
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
