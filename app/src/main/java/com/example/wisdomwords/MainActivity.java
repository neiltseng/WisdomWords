package com.example.wisdomwords;

import android.app.Activity;
import android.app.Instrumentation;
import android.graphics.Point;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.widget.ImageView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.twopages);
        setContentView(R.layout.activity_main);
        gpioControl();//BCM21
        ArrayList<Integer> arrImages=new ArrayList<Integer>();
        //arrImages.add(R.drawable.aphorisms01_r);
        arrImages.add(R.drawable.aphorisms01);
        arrImages.add(R.drawable.aphorisms02);
        arrImages.add(R.drawable.aphorisms03);
        Log.i(TAG, "showing images.");
        CurlView crulLeft = (CurlView)findViewById(R.id.curlView);
        //ImageView simpleImageView=(ImageView) findViewById(R.id.imageView_word);
        //simpleImageView.setImageResource(R.drawable.aphorisms01);
        new CurlActivity(this).load(crulLeft,arrImages);
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
                                    flipBook(dirFlip);
                                }
                            }).start();
                        }
                    }
                    // Return true to continue listening to events
                    return true;
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
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
        int maxX = mdispSize.x;
        int maxY = mdispSize.y;
        int steps = 60;
        float oneStepX = 0.0f;
        float oneStepY = 0.0f;
        int i = 0;
        float factX = (float)0.7;
        float factY = (float)0.5;
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
                Thread.sleep(50);
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
