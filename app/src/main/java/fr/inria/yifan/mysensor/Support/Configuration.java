package fr.inria.yifan.mysensor.Support;

import java.io.Serializable;

/**
 * This class stores global configuration parameters for the application.
 */

public class Configuration implements Serializable {

    // Parameters for sensing sampling
    public static final int SAMPLE_WINDOW_MS = 1000;
    public static final int SAMPLE_NUM_WINDOW = 1;
    // Permission request indicator code
    public static final int PERMS_REQUEST_RECORD = 1000;
    public static final int PERMS_REQUEST_STORAGE = 1001;
    public static final int PERMS_REQUEST_LOCATION = 1002;
    public static final int ENABLE_REQUEST_LOCATION = 1003;
    public static final int ENABLE_REQUEST_WIFI = 1004;
    // Wifi Direct network parameters
    public static final int SERVER_PORT = 8888;
    // Parameters for audio sound signal sampling
    public static final int SAMPLE_RATE_IN_HZ = 44100;
    // Minimum time interval between location updates (milliseconds)
    public static final int LOCATION_UPDATE_TIME = 100;
    // Minimum distance between location updates (meters)
    public static final int LOCATION_UPDATE_DISTANCE = 1;
    // Email destination for the sensing data
    public static final String DST_MAIL_ADDRESS = "yifan.du@polytechnique.edu";
    // For sound level calibration, initially 0, 1
    public static final double INTERCEPT = 0;
    public static final double SLOPE = 1;
    // Load the learning model file from this path
    public static final String MODEL_INPOCKET = "Classifier_pocket.model";
    public static final String MODEL_INDOOR = "Classifier_door.model";
    public static final String MODEL_UNDERGROUND = "Classifier_ground.model";
    public static final String DATASET_INPOCKET = "Dataset_pocket.model";
    public static final String DATASET_INDOOR = "Dataset_door.model";
    public static final String DATASET_UNDERGROUND = "Dataset_ground.model";
    // The lambda parameter for learning model updateByLabel
    public static final int LAMBDA = 10;
    // Storage path for the sensing data file
    static final String STORAGE_FILE_PATH = "/Download/MySensor";

}