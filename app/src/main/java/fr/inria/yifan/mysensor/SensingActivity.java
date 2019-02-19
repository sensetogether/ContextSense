package fr.inria.yifan.mysensor;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

import fr.inria.yifan.mysensor.Sensing.ContextHelper;
import fr.inria.yifan.mysensor.Sensing.SensorsHelper;
import fr.inria.yifan.mysensor.Support.FilesIOHelper;

import static fr.inria.yifan.mysensor.Support.Configuration.DST_MAIL_ADDRESS;
import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_LOCATION;
import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_WINDOW_MS;
import static java.lang.System.currentTimeMillis;

/*
 * This activity provides functions including labeling contexts and storing/sending sensing data
 */

public class SensingActivity extends AppCompatActivity {

    private static final String TAG = "Sensing activity";

    private final Object mLock; // Thread locker
    private boolean isGetSenseRun; // Running flag
    private int mSenseRound; // Sensing round
    private PowerManager.WakeLock mWakeLock; // Awake locker

    // Declare all related views in UI
    private Button mStartButton;
    private Button mStopButton;
    private Switch mSwitchLog;
    private Switch mSwitchMail;
    private ArrayAdapter<String> mAdapterSensing;

    private FilesIOHelper mFilesIOHelper; // File helper
    private ArrayList<String> mSensingData; // Sensing data
    private int mPocketLabel; // In-pocket binary label
    private int mDoorLabel; // In-door binary label
    private int mGroundLabel; // Under-ground binary label

    // Helpers for sensors and context
    private SensorsHelper mSensorHelper;
    private ContextHelper mContextHelper;

    // Constructor initializes locker
    public SensingActivity() {
        mLock = new Object();
    }

    // Initially bind all views
    private void bindViews() {
        TextView mWelcomeView = findViewById(R.id.welcome_view);
        mWelcomeView.setText(R.string.hint_sensing);
        mStartButton = findViewById(R.id.start_button);
        mStopButton = findViewById(R.id.stop_button);
        mStopButton.setVisibility(View.INVISIBLE);
        mSwitchLog = findViewById(R.id.switch_log);
        mSwitchMail = findViewById(R.id.switch_mail);
        mSwitchMail.setVisibility(View.INVISIBLE);

        // Build an adapter to feed the list with the content of a string array
        mSensingData = new ArrayList<>();
        mAdapterSensing = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mSensingData);
        // Then attache the adapter to the list view
        ListView listView = findViewById(R.id.list_view);
        listView.setAdapter(mAdapterSensing);

        // Radio group for scene selection
        final RadioGroup mPocketRadioGroup = findViewById(R.id.pocket_radio);
        mPocketRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId != -1) {
                    mPocketLabel = getSceneLabel(checkedId);
                    //Log.d(TAG, "Scene: " + mSenseScene + ", label: " + mSceneLabel);
                }
            }
        });

        // Radio group for scene selection
        final RadioGroup mDoorRadioGroup = findViewById(R.id.door_radio);
        mDoorRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId != -1) {
                    mDoorLabel = getSceneLabel(checkedId);
                    //Log.d(TAG, "Scene: " + mSenseScene + ", label: " + mSceneLabel);
                }
            }
        });

        // Radio group for scene selection
        final RadioGroup mGroundRadioGroup = findViewById(R.id.ground_radio);
        mGroundRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId != -1) {
                    mGroundLabel = getSceneLabel(checkedId);
                    //Log.d(TAG, "Scene: " + mSenseScene + ", label: " + mSceneLabel);
                }
            }
        });

        mPocketRadioGroup.check(R.id.outpocket_radio);
        mDoorRadioGroup.check(R.id.indoor_radio);
        mGroundRadioGroup.check(R.id.onground_radio);

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAdapterSensing.clear();
                mAdapterSensing.add("0 timestamp, 1 daytime (b), 2 light density (lx), 3 magnetic strength (μT), " +
                        "4 GSM active (b), 5 RSSI level, 6 RSSI value (dBm), 7 GPS accuracy (m), 8 Wifi active (b), " +
                        "9 Wifi RSSI (dBm), 10 proximity (b), 11 sound level (dBA), 12 temperature (C), 13 pressure (hPa), " +
                        "14 humidity (%), 15 in-pocket label, 16 in-door label, 17 under-ground label");
                startRecord();
                mStartButton.setVisibility(View.INVISIBLE);
                mStopButton.setVisibility(View.VISIBLE);
            }
        });
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecord();
                mStartButton.setVisibility(View.VISIBLE);
                mStopButton.setVisibility(View.INVISIBLE);
            }
        });
        mSwitchLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSwitchLog.isChecked()) {
                    mSwitchMail.setVisibility(View.VISIBLE);
                } else {
                    mSwitchMail.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    // Main activity initialization
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensing);
        bindViews();

        mSensorHelper = new SensorsHelper(this);
        mContextHelper = new ContextHelper(this);
        mFilesIOHelper = new FilesIOHelper(this);

        //acquireWakeLock();
    }

    // Stop thread when exit!
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSwitchLog.isChecked() && isGetSenseRun) {
            try {
                mFilesIOHelper.autoSave(arrayToString(mSensingData));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //releaseWakeLock();
        isGetSenseRun = false;
        mSensorHelper.stopService();
        mContextHelper.stopService();
    }

    // Start the sensing thread
    private void startRecord() {
        if (isGetSenseRun) {
            Log.e(TAG, "Still in sensing and recording");
            return;
        }
        mSensorHelper.startService();
        mContextHelper.startService();
        isGetSenseRun = true;
        mSenseRound = 0;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (isGetSenseRun) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapterSensing.add(currentTimeMillis() + ", " +
                                    mContextHelper.isDaytime() + ", " +
                                    mSensorHelper.getLightDensity() + ", " +
                                    mSensorHelper.getMagnet() + ", " +
                                    mContextHelper.isGSMLink() + ", " +
                                    mContextHelper.getRssiLevel() + ", " +
                                    mContextHelper.getRssiValue() + ", " +
                                    mContextHelper.getGPSAccuracy() + ", " +
                                    mContextHelper.isWifiLink() + ", " +
                                    mContextHelper.getWifiRSSI() + ", " +
                                    mSensorHelper.getProximity() + ", " +
                                    mSensorHelper.getSoundLevel() + ", " +
                                    mSensorHelper.getTemperature() + ", " +
                                    mSensorHelper.getPressure() + ", " +
                                    mSensorHelper.getHumidity() + ", " +
                                    mPocketLabel + ", " +
                                    mDoorLabel + ", " +
                                    mGroundLabel);
                            mSenseRound += 1;
                            //Log.d(TAG, String.valueOf(mSenseRound));
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

    // Stop the sensing
    @SuppressLint("SetTextI18n")
    private void stopRecord() {
        isGetSenseRun = false;
        mSensorHelper.stopService();
        mContextHelper.stopService();
        if (mSwitchLog.isChecked()) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            final EditText editName = new EditText(this);
            editName.setText(android.os.Build.MODEL + "_" + currentTimeMillis());
            dialog.setTitle("Enter file name: ");
            dialog.setView(editName);
            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //Log.d(TAG, "Now is " + time);
                    try {
                        String filename = editName.getText() + ".csv";
                        mFilesIOHelper.saveFile(filename, arrayToString(mSensingData));
                        if (mSwitchMail.isChecked()) {
                            Log.d(TAG, "File path is : " + mFilesIOHelper.getFileUri(filename));
                            mFilesIOHelper.sendFile(DST_MAIL_ADDRESS, getString(R.string.email_title), mFilesIOHelper.getFileUri(filename));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //Pass
                }
            });
            dialog.show();
        }
    }

    // Go to the detection activity
    public void goDetection(View view) {
        Intent goToDetection = new Intent();
        goToDetection.setClass(this, DetectionActivity.class);
        startActivity(goToDetection);
        finish();
    }

    // Go to the GPS activity
    public void goGPS(View view) {
        Intent goToGPS = new Intent();
        goToGPS.setClass(this, GPSActivity.class);
        startActivity(goToGPS);
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

    // Turn on the awake locker
    private void acquireWakeLock() {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            assert pm != null;
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getCanonicalName());
            mWakeLock.acquire(100 * 60 * 1000L /*100 minutes*/);
        }
    }

    // Turn off the awake locker
    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    // Get the binary label for this scene
    private int getSceneLabel(int radioId) {
        switch (radioId) {
            case R.id.inpocket_radio:
                return 1;
            case R.id.outpocket_radio:
                return 0;
            case R.id.indoor_radio:
                return 1;
            case R.id.outdoor_radio:
                return 0;
            case R.id.underground_radio:
                return 1;
            case R.id.onground_radio:
                return 0;
            default:
                return -1;
        }
    }

    // Convert string array to single string
    private String arrayToString(ArrayList<String> array) {
        StringBuilder content = new StringBuilder();
        for (String line : array) {
            content.append(line).append("\n");
        }
        return content.toString();
    }

}