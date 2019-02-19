package fr.inria.yifan.mysensor;

/*
 * This activity provides functions related to the GPS service
 */

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import fr.inria.yifan.mysensor.Sensing.ContextHelper;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_LOCATION;
import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_WINDOW_MS;

public class GPSActivity extends AppCompatActivity {

    private static final String TAG = "GPS activity";

    // Thread locker and running flag
    private final Object mLock;
    private boolean isSensingRun;

    // Declare all used views
    private TextView mLocationView;
    private Button mStartButton;
    private Button mStopButton;

    // Sensors helper for sensor and context
    private ContextHelper mContextHelper;

    // Constructor initializes locker
    public GPSActivity() {
        mLock = new Object();
    }

    // Initially bind all views
    private void bindViews() {
        TextView welcomeView = findViewById(R.id.welcome_view);
        welcomeView.setText(R.string.hint_gps);

        mLocationView = findViewById(R.id.location_view);
        mStartButton = findViewById(R.id.start_button);
        mStopButton = findViewById(R.id.stop_button);
        mStopButton.setVisibility(View.INVISIBLE);

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSensing();
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
    }

    // Clean all text views
    @SuppressLint("SetTextI18n")
    private void cleanView() {
        mLocationView.setText(null);
    }

    // Main activity initialization
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);
        bindViews();
        cleanView();

        mContextHelper = new ContextHelper(this);
    }

    // Stop thread when exit!
    @Override
    protected void onDestroy() {
        super.onDestroy();
        isSensingRun = false;
        mContextHelper.stopService();
    }

    // Start the sensing detection
    @SuppressLint("SetTextI18n")
    private void startSensing() {
        if (isSensingRun) {
            Log.e(TAG, "Still in sensing state");
            return;
        }
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
                            Location location = mContextHelper.getLocation();

                            String loc = "Current location information：\n" +
                                    " - Longitude：" + location.getLongitude() + "\n" +
                                    " - Latitude：" + location.getLatitude() + "\n" +
                                    " - Altitude：" + location.getAltitude() + "\n" +
                                    " - Speed：" + location.getSpeed() + "\n" +
                                    " - Bearing：" + location.getBearing() + "\n" +
                                    " - Accuracy：" + location.getAccuracy() + "\n" +
                                    " - Time：" + location.getTime();
                            mLocationView.setText(loc);
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
        mContextHelper.stopService();
    }

    // Go to the detection activity
    public void goDetection(View view) {
        Intent goToDetection = new Intent();
        goToDetection.setClass(this, DetectionActivity.class);
        startActivity(goToDetection);
        finish();
    }

    // Go to the sensing activity
    public void goSensing(View view) {
        Intent goToSensing = new Intent();
        goToSensing.setClass(this, SensingActivity.class);
        startActivity(goToSensing);
        finish();
    }

    // Callback for user enabling GPS switch
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ENABLE_REQUEST_LOCATION: {
                mContextHelper.startService();
            }
        }
    }
}
