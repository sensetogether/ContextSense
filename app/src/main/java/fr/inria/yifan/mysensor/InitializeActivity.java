package fr.inria.yifan.mysensor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import static fr.inria.yifan.mysensor.Support.Configuration.PERMS_REQUEST_LOCATION;
import static fr.inria.yifan.mysensor.Support.Configuration.PERMS_REQUEST_RECORD;
import static fr.inria.yifan.mysensor.Support.Configuration.PERMS_REQUEST_STORAGE;

/**
 * This activity has to be started in the beginning of the application to ensure all user permissions are enabled
 */

public class InitializeActivity extends AppCompatActivity {

    private static final String TAG = "Initialization";

    // Declare microphone permissions
    private static final String[] RECORD_PERMS = {Manifest.permission.RECORD_AUDIO};

    // Declare file storage permissions
    @SuppressLint("InlinedApi")
    private static final String[] STORAGE_PERMS = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    // Declare GPS permissions
    private static final String[] LOCATION_PERMS = {Manifest.permission.ACCESS_FINE_LOCATION};

    // Main activity initialization
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize);

        TextView welcomeView = findViewById(R.id.welcome_view);
        welcomeView.setText("Please select a function below to startService after permission checked.");

        checkPermission();
    }

    // Check related user permissions
    private void checkPermission() {
        // Check user permission for microphone
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this, "Requesting microphone permission", Toast.LENGTH_SHORT).show();
            // Request permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(RECORD_PERMS, PERMS_REQUEST_RECORD);
            } else {
                Toast.makeText(this, "Please give microphone permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        // Check user permission for file storage
        else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this, "Requesting storage permission", Toast.LENGTH_SHORT).show();
            // Request permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(STORAGE_PERMS, PERMS_REQUEST_STORAGE);
            } else {
                Toast.makeText(this, "Please give storage permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        // Check user permission for GPS location
        else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this, "Requesting location permission", Toast.LENGTH_SHORT).show();
            // Request permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(LOCATION_PERMS, PERMS_REQUEST_LOCATION);
            } else {
                Toast.makeText(this, "Please give location permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Permission checked OK", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    // Callback for user allowing permission
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMS_REQUEST_RECORD:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, String.valueOf(grantResults[0]));
                    Toast.makeText(this, "Please give microphone permission", Toast.LENGTH_LONG).show();
                }
                checkPermission();
                break;
            case PERMS_REQUEST_STORAGE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, String.valueOf(grantResults[0]));
                    Toast.makeText(this, "Please give storage permission", Toast.LENGTH_LONG).show();
                }
                checkPermission();
                break;
            case PERMS_REQUEST_LOCATION:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, String.valueOf(grantResults[0]));
                    Toast.makeText(this, "Please give location permission", Toast.LENGTH_LONG).show();
                }
                checkPermission();
                break;
        }
    }

    // Go to the sensor list activity
    public void goSensors(View view) {
        Intent goToSensors = new Intent();
        goToSensors.setClass(this, SensorsActivity.class);
        startActivity(goToSensors);
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

    // Go to the GPS activity
    public void goGPS(View view) {
        Intent goToGPS = new Intent();
        goToGPS.setClass(this, GPSActivity.class);
        startActivity(goToGPS);
        finish();
    }

}
