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
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;
import java.util.ArrayList;
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
    private ImageView bookCoverImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.twopages);
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.animation);

        handler=new Handler();

        imageLeft = (ImageView) findViewById(R.id.imageView1);

        ArrayList<Integer> arrImages=new ArrayList<Integer>();
        arrImages.add(R.drawable.aphorisms01_r);
        //arrImages.add(R.drawable.aphorisms01);
        arrImages.add(R.drawable.aphorisms02_r);
        arrImages.add(R.drawable.aphorisms03_r);
        //Log.i(TAG, "showing images.");
        //curlRightView = (CurlView)findViewById(R.id.curlView);
        //mVideoView = (VideoView)findViewById(R.id.videoView01);
        bookCoverImg = (ImageView)findViewById(R.id.imageView01_v2);
        // Set up the media controller widget and attach it to the video view.
        //MediaController controller = new MediaController(this);
        //controller.setMediaPlayer(mVideoView);
        //mVideoView.setMediaController(null);
        gpioControl();//BCM21
        //ImageView simpleImageView=(ImageView) findViewById(R.id.imageView_word);
        //simpleImageView.setImageResource(R.drawable.aphorisms01);
        //new CurlActivity(this).load(curlRightView,arrImages);
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
                                    //flipBook(dirFlip);
                                    if(mVideoPlaying == Boolean.FALSE){
                                        mVideoPlaying = Boolean.TRUE;
                                        //can't play video here ,
                                        // would cause Only the original thread that created a view hierarchy can touch its views
                                        //initializePlayer();
                                        handler.post(playFlippingVideo);
                                    }
                                }
                            }).start();
                        }
                    }
                    //showLeftImg();
                    // Return true to continue listening to events
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
        bookCoverImg.setVisibility(VideoView.INVISIBLE);
        setContentView(R.layout.fullscreen);
        mVideoView = (VideoView)findViewById(R.id.videoView01);
        MediaController controller = new MediaController(this);
        controller.setMediaPlayer(mVideoView);
        mVideoView.setMediaController(null);
        // Buffer and decode the video sample.
        Uri videoUri = getMedia(VIDEO_SAMPLE_01);

        Log.i(TAG, "video Uri = "+videoUri);
        mVideoView.setVideoURI(videoUri);

        // Listener for onPrepared() event (runs after the media is prepared).
        mVideoView.setOnPreparedListener(
                new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {

                        // Hide buffering message.
                        //mBufferingTextView.setVisibility(VideoView.INVISIBLE);

                        // Restore saved position, if available.
                        if (mCurrentPosition > 0) {
                            mVideoView.seekTo(mCurrentPosition);
                        } else {
                            // Skipping to 1 shows the first frame of the video.
                            mVideoView.seekTo(1);
                        }

                        // Start playing!
                        Log.i(TAG, "start to play flipping video");
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
                        mVideoView.seekTo(0);
                        //initializePlayer(Boolean.FALSE);
                        Log.i(TAG, "regular video is playing , again.");
                    }
                });
    }

    // Get a Uri for the media sample regardless of whether that sample is
    // embedded in the app resources or available on the internet.
    private Uri getMedia(String mediaName) {
        String fileName = null;
        if(mediaName == VIDEO_SAMPLE_01) {
            fileName = "android.resource://" + getPackageName() + "/" + R.raw.video_01;
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
