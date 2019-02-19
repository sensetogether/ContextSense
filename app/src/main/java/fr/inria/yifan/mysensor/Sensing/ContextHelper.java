package fr.inria.yifan.mysensor.Sensing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;

import java.util.Calendar;

import fr.inria.yifan.mysensor.Support.SlideWindow;

import static fr.inria.yifan.mysensor.Support.Configuration.ENABLE_REQUEST_LOCATION;
import static fr.inria.yifan.mysensor.Support.Configuration.LOCATION_UPDATE_DISTANCE;
import static fr.inria.yifan.mysensor.Support.Configuration.LOCATION_UPDATE_TIME;
import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_NUM_WINDOW;
import static fr.inria.yifan.mysensor.Support.Configuration.SAMPLE_WINDOW_MS;
import static java.lang.System.currentTimeMillis;

/**
 * This class implements the context helper for a sensing device.
 */

public class ContextHelper extends BroadcastReceiver {

    private static final String TAG = "Device context";

    // Thread running flag
    private boolean isSensingRun;

    // Threshold for out-of-range
    private static final int WIFI_RSSI_OUT = -150;
    private static final int GSM_RSSI_OUT = -150;
    private static final int GPS_ACC_OUT = 1000;

    // Declare references and managers
    private Activity mActivity;
    private TelephonyManager mTelephonyManager;
    private LocationManager mLocationManager;
    private ConnectivityManager mConnectManager;
    private WifiManager mWifiManager;

    // Declare all contexts
    private int mGSMFlag;
    private SlideWindow mRssiLevel;
    private SlideWindow mRssiValue;
    private SlideWindow mAccuracy;
    private SlideWindow mSpeed;
    private SlideWindow mWifiRssi;
    //private float hasBattery;
    //private long localTime;
    //private boolean inPocket;
    //private boolean inDoor;
    //private boolean underGround;
    //private ArrayMap<Sensor, Boolean> sensorArray;
    private Location mLocation;
    private String userActivity;

    // Declare GSM RSSI state listener
    private PhoneStateListener mListenerPhone = new PhoneStateListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mGSMFlag = signalStrength.isGsm() ? 1 : 0;
            Log.d(TAG, "CDMA: " + String.valueOf(signalStrength.getCdmaDbm()));
            Log.d(TAG, "Evdo: " + String.valueOf(signalStrength.getEvdoDbm()));
            Log.d(TAG, "Is GSM: " + String.valueOf(signalStrength.isGsm()));
            Log.d(TAG, "GSM: " + (signalStrength.getGsmSignalStrength() * 2 - 113));
            mRssiValue.putValue(signalStrength.getGsmSignalStrength() * 2 - 113); // -> dBm
            mRssiLevel.putValue(signalStrength.getLevel());
        }
    };

    // Declare location service listener
    private LocationListener mListenerLoc = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;
            mAccuracy.putValue(location.getAccuracy());
            mSpeed.putValue(location.getSpeed());
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            mLocation = mLocationManager.getLastKnownLocation(provider);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onProviderEnabled(String provider) {
            mLocation = mLocationManager.getLastKnownLocation(provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            //PASS
        }
    };

    // Constructor initialization
    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public ContextHelper(Activity activity) {
        mActivity = activity;

        mTelephonyManager = (TelephonyManager) mActivity.getSystemService(Context.TELEPHONY_SERVICE);
        mLocationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);
        mConnectManager = (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) mActivity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        /* Searching neighbor cells does not work
        List<NeighboringCellInfo> neighborList = mTelephonyManager.getNeighboringCellInfo();
        List<CellInfo> cellList = mTelephonyManager.getAllCellInfo();

        Log.d(TAG, "Cell Information: " + cellList + " " + neighborList);
        for(CellInfo cellInfo: cellList){
            Log.d(TAG, "Cell Info: " + String.valueOf(cellInfo));
        }
        */

        //IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //Intent batteryStatus = mActivity.registerReceiver(null, filter);

        // Are we charging / charged?
        //assert batteryStatus != null;
        //int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        //boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
        //int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        //boolean acCharge = (chargePlug == BatteryManager.BATTERY_PLUGGED_AC);

        // Get the current battery capacity
        //int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        //int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        //float batteryPct = level / (float) scale;
    }

    // Start the context service
    @SuppressLint("MissingPermission")
    public void startService() {

        mRssiLevel = new SlideWindow(SAMPLE_NUM_WINDOW, 0);
        mRssiValue = new SlideWindow(SAMPLE_NUM_WINDOW, GSM_RSSI_OUT);
        mAccuracy = new SlideWindow(SAMPLE_NUM_WINDOW, GPS_ACC_OUT);
        mSpeed = new SlideWindow(SAMPLE_NUM_WINDOW, 0);
        mWifiRssi = new SlideWindow(SAMPLE_NUM_WINDOW, WIFI_RSSI_OUT);
        //hasBattery = 0;
        //localTime = 0;
        //inPocket = false;
        //inDoor = false;
        //underGround =false;
        userActivity = null;
        mLocation = new Location("null");

        mTelephonyManager.listen(mListenerPhone, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        // Check GPS enable switch
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Start GPS and location service
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_TIME, LOCATION_UPDATE_DISTANCE, mListenerLoc);
        } else {
            Toast.makeText(mActivity, "Please enable the GPS", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            mActivity.startActivityForResult(intent, ENABLE_REQUEST_LOCATION);
        }

        // Google activity recognition API
        PendingIntent pendingIntent = PendingIntent.getBroadcast(mActivity, 1000,
                new Intent("ActivityRecognitionResult"), PendingIntent.FLAG_CANCEL_CURRENT);
        ActivityRecognitionClient activityRecognitionClient = ActivityRecognition.getClient(mActivity);
        activityRecognitionClient.requestActivityUpdates(LOCATION_UPDATE_TIME, pendingIntent);
        mActivity.registerReceiver(this, new IntentFilter("ActivityRecognitionResult"));

        // Start the loop thread for synchronised sensing window
        final int delay = SAMPLE_WINDOW_MS / SAMPLE_NUM_WINDOW;
        isSensingRun = true;
        new Thread() {
            public void run() {
                while (isSensingRun) try {
                    updateManual();
                    Thread.sleep(delay);
                    Log.d(TAG, currentTimeMillis() + " Update window");
                } catch (InterruptedException e) {
                    Log.e(TAG, "Local thread error: ", e);
                }
            }
        }.start();
    }

    // Unregister the listeners
    public void stopService() {
        try {
            mLocationManager.removeUpdates(mListenerLoc);
            mTelephonyManager.listen(mListenerPhone, PhoneStateListener.LISTEN_NONE);
            mActivity.unregisterReceiver(this);
        } catch (Exception e) {
            //Pass
        }
        isSensingRun = false;
    }

    // Get the most recent GSM RSSI
    public int isGSMLink() {
        return mGSMFlag;
    }

    // Get the most recent signal strength level
    public float getRssiLevel() {
        return mRssiLevel.getMean();
    }

    // Get the most recent signal strength level
    public float getRssiValue() {
        return mRssiValue.getMean();
    }

    // Get location information from GPS
    @SuppressLint("MissingPermission")
    public Location getLocation() {
        for (GpsSatellite satellite : mLocationManager.getGpsStatus(null).getSatellites()) {
            Log.d(TAG, satellite.toString());
        }
        //Log.d(TAG, "Location information: " + mLocation);
        return mLocation;
    }

    // Get the most recent GPS accuracy
    public float getGPSAccuracy() {
        return mAccuracy.getMean();
    }

    // Get the most recent GPS speed
    public float getGPSSpeed() {
        return mSpeed.getMean();
    }

    // Get the most recent user activity
    public String getUserActivity() {
        return userActivity;
    }

    // Detection in daytime or night
    public int isDaytime() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return (hour > 6 && hour < 18) ? 1 : 0;
    }

    // Detection on Wifi access
    public int isWifiLink() {
        NetworkInfo info = mConnectManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return info.isConnected() ? 1 : 0;
    }

    // Detection on Wifi RSSI
    public float getWifiRSSI() {
        return mWifiRssi.getMean();
    }

    // Manually updateByLabel sliding window
    private void updateManual() {
        mRssiLevel.updateWindow();
        mRssiValue.updateWindow();
        mAccuracy.updateWindow();
        mSpeed.updateWindow();
        // Wifi RSSI has no callback listener
        if (mWifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                mWifiRssi.putValue(wifiInfo.getRssi());
            } else {
                mWifiRssi.putValue(WIFI_RSSI_OUT);
            }
        } else {
            mWifiRssi.putValue(WIFI_RSSI_OUT);
        }
    }

    // Intent receiver for activity recognition result callback
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            userActivity = result.getMostProbableActivity().toString();
            //Log.e(TAG, "Received intent: " + result.getMostProbableActivity().toString());
        }
    }

}