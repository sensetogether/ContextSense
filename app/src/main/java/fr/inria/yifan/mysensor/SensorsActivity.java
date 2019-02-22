package fr.inria.yifan.mysensor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.List;

/*
 * This activity shows the sensor list of current device on the layout
 */

public class SensorsActivity extends AppCompatActivity {

    // Declare all views used
    private TextView mTextTitle;
    private TextView mTextMessage;

    // Declare sensors and managers
    private SensorManager mSensorManager;

    // Initially bind views
    private void bindViews() {
        mTextTitle = findViewById(R.id.title);
        mTextMessage = findViewById(R.id.message);
    }

    // Clear all views content
    @SuppressLint("SetTextI18n")
    private void initialViews() {
        mTextTitle.setText("Available sensors list:");
        mTextMessage.setText("...");
    }

    // Main activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensors);
        bindViews();
        initialViews();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        showSensorList();
    }

    // Show all available sensors in a list
    private void showSensorList() {
        List<Sensor> allSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        StringBuilder sb = new StringBuilder();
        sb.append("This device has ").append(allSensors.size()).append(" sensors (include uncalibrated), listed as:\n\n");
        int i = 0;
        for (Sensor s : allSensors) {
            i += 1;
            switch (s.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    sb.append(i).append(": Accelerometer sensor ").append(s.getType()).append(".\n");
                    break;
                case Sensor.TYPE_AMBIENT_TEMPERATURE:
                    sb.append(i).append(": Ambient temperature sensor ").append(s.getType()).append(".\n");
                    break;
                case Sensor.TYPE_GAME_ROTATION_VECTOR:
                    sb.append(i).append(": Game rotation vector sensor ").append(s.getType()).append(".\n");
                    break;
                case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                    sb.append(i).append(": Geomagnetic rotation vector sensor ").append(s.getType()).append(".\n");
                    break;
                case Sensor.TYPE_GRAVITY:
                    sb.append(i).append(": Gravity sensor ").append(s.getType()).append(".\n");
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    sb.append(i).append(":Gyroscope sensor ").append(s.getType()).append(".\n");
                    break;
                case Sensor.TYPE_LIGHT:
                    sb.append(i).append(":Light sensor ").append(s.getType()).append(".\n");
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    sb.append(i).append(": Linear acceleration sensor ").append(s.getType()).append(".\n");
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    sb.append(i).append(": Magnetic field sensor ").append(s.getType()).append(".\n");
                    break;
                case Sensor.TYPE_ORIENTATION:
                    sb.append(i).append(": Orientation sensor ").append(s.getType()).append(".\n");
                    break;
                case Sensor.TYPE_PRESSURE:
                    sb.append(i).append(": Pressure sensor ").append(s.getType()).append(".\n");
                    break;
                case Sensor.TYPE_PROXIMITY:
                    sb.append(i).append(": Proximity sensor ").append(s.getType()).append(".\n");
                    break;
                case Sensor.TYPE_RELATIVE_HUMIDITY:
                    sb.append(i).append(": Relative humidity sensor ").append(s.getType()).append(".\n");
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    sb.append(i).append(": Rotation vector sensor ").append(s.getType()).append(".\n");
                    break;
                case Sensor.TYPE_TEMPERATURE:
                    sb.append(i).append(": Temperature sensor ").append(s.getType()).append(".\n");
                    break;
                default:
                    sb.append(i).append(": Other sensor ").append(s.getType()).append(".\n");
                    break;
            }
            sb.append(" - Device name: ").append(s.getName()).append("\n - Device version: ").append(s.getVersion()).append("\n - Manufacturer: ").append(s.getVendor()).append("\n - Power consumption: ").append(s.getPower()).append("\n\n");
        }
        mTextMessage.setText(sb.toString());
    }

}