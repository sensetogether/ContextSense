package fr.inria.yifan.mysensor;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import fr.inria.yifan.mysensor.Inference.InferenceHelper;
import fr.inria.yifan.mysensor.Sensing.ContextHelper;
import fr.inria.yifan.mysensor.Sensing.SensorsHelper;

import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_WINDOW_MS;
import static java.lang.System.currentTimeMillis;

/*
 * This activity provides functions including in-pocket/in-door/under-ground detection and feedback
 */

public class DetectionActivity extends AppCompatActivity {

    private static final String TAG = "Detection activity";

    // Thread locker and running flag
    private final Object mLock;
    private boolean isSensingRun;

    // Declare all used views
    private TextView mPocketView;
    private TextView mDoorView;
    private TextView mGroundView;
    private TextView mHierarView;
    private TextView mActivityView;
    private Button mPocketButton;
    private Button mDoorButton;
    private Button mGroundButton;
    private Button mHierarButton;
    private Button mStartButton;
    private Button mStopButton;
    private NotificationCompat.Builder mNotifyBuilder;
    private NotificationManagerCompat mNotificationManager;

    // Sensors helper for sensor and context
    private SensorsHelper mSensorHelper;
    private ContextHelper mContextHelper;
    private InferenceHelper mInferHelper;
    private double[] mSample;
    private boolean mInPocket;
    private boolean mInDoor;
    private boolean mUnderGround;

    // Constructor initializes locker
    public DetectionActivity() {
        mLock = new Object();
    }

    // Initially bind all views
    private void bindViews() {
        TextView mWelcomeView = findViewById(R.id.welcome_view);
        mWelcomeView.setText(R.string.hint_detect);

        mPocketView = findViewById(R.id.pocket_text);
        mDoorView = findViewById(R.id.door_text);
        mGroundView = findViewById(R.id.ground_text);
        mHierarView = findViewById(R.id.hierar_text);
        mActivityView = findViewById(R.id.activity_view);
        mPocketButton = findViewById(R.id.pocket_button);
        mDoorButton = findViewById(R.id.door_button);
        mGroundButton = findViewById(R.id.ground_button);
        mHierarButton = findViewById(R.id.hierar_button);
        mStartButton = findViewById(R.id.start_button);
        mStopButton = findViewById(R.id.stop_button);
        mStopButton.setVisibility(View.INVISIBLE);

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSensing();
                mPocketButton.setVisibility(View.VISIBLE);
                mDoorButton.setVisibility(View.VISIBLE);
                mGroundButton.setVisibility(View.VISIBLE);
                mHierarButton.setVisibility(View.VISIBLE);
                mStartButton.setVisibility(View.INVISIBLE);
                mStopButton.setVisibility(View.VISIBLE);
            }
        });
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSensing();
                cleanView();
                mStartButton.setVisibility(View.VISIBLE);
                mStopButton.setVisibility(View.INVISIBLE);
            }
        });

        mPocketButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mInferHelper.updatePocket(mSample);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mDoorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mInferHelper.updateDoor(mSample);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mGroundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mInferHelper.updateGround(mSample);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mHierarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mInferHelper.updateHierar(mSample);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // Clean all text views
    @SuppressLint("SetTextI18n")
    private void cleanView() {
        mPocketView.setText(null);
        mDoorView.setText(null);
        mGroundView.setText(null);
        mActivityView.setText(null);
        mHierarView.setText(null);
        mPocketButton.setVisibility(View.INVISIBLE);
        mDoorButton.setVisibility(View.INVISIBLE);
        mGroundButton.setVisibility(View.INVISIBLE);
        mHierarButton.setVisibility(View.INVISIBLE);
    }

    // Notification bar initialization
    private void notifyView() {
        mNotifyBuilder = new NotificationCompat.Builder(this, "Inference")
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentTitle(getString(R.string.notify_title))
                .setContentText(getString(R.string.notify_content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true);
        // Set the intent that will fire when the user taps the notification
        //.setContentIntent(pendingIntent)
        //.setAutoCancel(true);
        mNotificationManager = NotificationManagerCompat.from(this);
        // notificationId is a unique int for each notification that you must define
        mNotificationManager.notify(1001, mNotifyBuilder.build());
    }

    // Main activity initialization
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detection);
        bindViews();
        cleanView();
        mSensorHelper = new SensorsHelper(this);
        mContextHelper = new ContextHelper(this);
        mInferHelper = new InferenceHelper(this);
        this.registerReceiver(mInferHelper, new IntentFilter(Intent.ACTION_MAIN));
    }

    // Stop thread when exit!
    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mInferHelper);
        mNotificationManager.cancel(1001);
        isSensingRun = false;
        mSensorHelper.stopService();
        mContextHelper.stopService();
    }

    // Start the sensing detection
    @SuppressLint("SetTextI18n")
    private void startSensing() {
        if (isSensingRun) {
            Log.e(TAG, "Still in sensing state");
            return;
        }
        notifyView();
        mSensorHelper.startService();
        mContextHelper.startService();
        isSensingRun = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isSensingRun) {
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.N)
                        @Override
                        public void run() {
                            /*
                            0 timestamp, 1 daytime (b), 2 light density (lx), 3 magnetic strength (μT), 4 GSM active (b),
                            5 RSSI level, 6 RSSI value (dBm), 7 GPS accuracy (m), 8 Wifi active (b), 9 Wifi RSSI (dBm),
                            10 proximity (b), 11 sound level (dBA), 12 temperature (C), 13 pressure (hPa), 14 humidity (%),
                            */
                            mSample = new double[]{currentTimeMillis(),
                                    mContextHelper.isDaytime(),
                                    mSensorHelper.getLightDensity(),
                                    mSensorHelper.getMagnet(),
                                    mContextHelper.isGSMLink(),
                                    mContextHelper.getRssiLevel(),
                                    mContextHelper.getRssiValue(),
                                    mContextHelper.getGPSAccuracy(),
                                    mContextHelper.isWifiLink(),
                                    mContextHelper.getWifiRSSI(),
                                    mSensorHelper.getProximity(),
                                    mSensorHelper.getSoundLevel(),
                                    mSensorHelper.getTemperature(),
                                    mSensorHelper.getPressure(),
                                    mSensorHelper.getHumidity()};
                            //Log.d(TAG, Arrays.toString(mSample));

                            /*
                            long startTime;
                            long endTime;
                            try {
                                startTime = System.nanoTime();
                                mInferHelper.infer("Pocket", mSample);
                                endTime = System.nanoTime();
                                System.out.println("Pocket infer (ms): " + (endTime - startTime) / 1000000d);

                                startTime = System.nanoTime();
                                mInferHelper.infer("Door", mSample);
                                endTime = System.nanoTime();
                                System.out.println("Door infer (ms): " + (endTime - startTime) / 1000000d);

                                startTime = System.nanoTime();
                                mInferHelper.infer("Ground", mSample);
                                endTime = System.nanoTime();
                                System.out.println("Ground infer (ms): " + (endTime - startTime) / 1000000d);

                                startTime = System.nanoTime();
                                mInferHelper.updateByLabel("Pocket", mSample, 1);
                                endTime = System.nanoTime();
                                System.out.println("Pocket updateByLabel (ms): " + (endTime - startTime) / 1000000d);

                                startTime = System.nanoTime();
                                mInferHelper.updateByLabel("Door", mSample, 1);
                                endTime = System.nanoTime();
                                System.out.println("Door updateByLabel (ms): " + (endTime - startTime) / 1000000d);

                                startTime = System.nanoTime();
                                mInferHelper.updateByLabel("Ground", mSample, 1);
                                endTime = System.nanoTime();
                                System.out.println("Ground updateByLabel (ms): " + (endTime - startTime) / 1000000d);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            */

                            try {
                                if (mInPocket = mInferHelper.inferPocket(mSample)) {
                                    mPocketView.setText("In-pocket");
                                } else {
                                    mPocketView.setText("Out-pocket");
                                }
                                //Log.d(TAG, String.valueOf(mInferHelper.InferIndoor(sample)));
                                if (mInDoor = mInferHelper.inferDoor(mSample)) {
                                    mDoorView.setText("In-door");
                                } else {
                                    mDoorView.setText("Out-door");
                                }
                                //Log.d(TAG, String.valueOf(mInferHelper.InferUnderground(sample)));
                                if (mUnderGround = mInferHelper.inferGround(mSample)) {
                                    mGroundView.setText("Under-ground");
                                } else {
                                    mGroundView.setText("On-ground");
                                }

                                String result = mInferHelper.inferHierar(mSample);
                                mHierarView.setText(result);
                                mNotifyBuilder.setContentText(result);
                                mNotificationManager.notify(1001, mNotifyBuilder.build());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            mActivityView.setText(mContextHelper.getUserActivity());
                        }
                    });
                    // Sampling time delay
                    synchronized (mLock) {
                        try {
                            mLock.wait(SAMPLE_WINDOW_MS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    // Stop the sensing detection
    private void stopSensing() {
        isSensingRun = false;
        mSensorHelper.stopService();
        mContextHelper.stopService();
        mNotificationManager.cancel(1001);
    }

    // Go to the sensing activity
    public void goSensing(View view) {
        Intent goToSensing = new Intent();
        goToSensing.setClass(this, SensingActivity.class);
        startActivity(goToSensing);
        finish();
    }

    // Go to the GPS activity
    public void goGPS(View view) {
        Intent goToGPS = new Intent();
        goToGPS.setClass(this, GPSActivity.class);
        startActivity(goToGPS);
        finish();
    }

}