package fr.inria.yifan.mysensor.Inference;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import fr.inria.yifan.mysensor.Deprecated.AdaBoost;
import weka.classifiers.trees.HoeffdingTree;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import static fr.inria.yifan.mysensor.Support.Configuration.DATASET_INDOOR;
import static fr.inria.yifan.mysensor.Support.Configuration.DATASET_INPOCKET;
import static fr.inria.yifan.mysensor.Support.Configuration.DATASET_UNDERGROUND;
import static fr.inria.yifan.mysensor.Support.Configuration.LAMBDA;
import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_INDOOR;
import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_INPOCKET;
import static fr.inria.yifan.mysensor.Support.Configuration.MODEL_UNDERGROUND;

/**
 * This class implements the inference helper to detect physical contexts
 */

public class InferenceHelper extends BroadcastReceiver {

    private static final String TAG = "Inference helper";

    private Context mContext;

    // Instances for data set initialization
    private Instances instancesPocket;
    private Instances instancesDoor;
    private Instances instancesGround;
    // H.Tree for classifiers initialization
    private HoeffdingTree classifierPocket;
    private HoeffdingTree classifierDoor;
    private HoeffdingTree classifierGround;
    private int hierarResult; // 1 in-pocket, 2 out-pocket out-door, 3 out-pocket in-door under-ground, 4 out-pocket in-door on-ground

    // Load the trained models and register instances
    public InferenceHelper(Context context) {
        mContext = context;
        loadModels();
    }

    // Inference on the new instance
    public boolean inferPocket(double[] sample) throws Exception {
        // 10 Proximity, 12 temperature, 2 light density
        double[] entry = new double[]{sample[10], sample[12], sample[2]};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesPocket);
        return classifierPocket.classifyInstance(inst) == 1;
    }

    // Inference on the new instance
    public boolean inferDoor(double[] sample) throws Exception {
        // 7 GPS accuracy, 5 RSSI level, 6 RSSI value, 9 Wifi RSSI, 2 light density, 12 temperature
        double[] entry = new double[]{sample[7], sample[5], sample[6], sample[9], sample[2], sample[12]};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesDoor);
        return classifierDoor.classifyInstance(inst) == 1;

    }

    // Inference on the new instance
    public boolean inferGround(double[] sample) throws Exception {
        // 5 RSSI level, 7 GPS accuracy (m), 12 temperature, 6 RSSI value, 13 pressure, 9 Wifi RSSI, 14 humidity
        double[] entry = new double[]{sample[5], sample[7], sample[12], sample[6], sample[13], sample[9], sample[14]};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesGround);
        return classifierGround.classifyInstance(inst) == 1;
    }

    // Update the model by online learning
    public void updatePocket(double[] sample) throws Exception {
        boolean wrong = inferPocket(sample);
        int p;
        // 10 Proximity, 12 temperature, 2 light density
        double[] entry = new double[]{sample[10], sample[12], sample[2], !wrong ? 1 : 0};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesPocket);
        p = AdaBoost.Poisson(LAMBDA);
        for (int k = 0; k < p; k++) {
            classifierPocket.updateClassifier(inst);
        }
    }

    // Update the model by online learning
    public void updateDoor(double[] sample) throws Exception {
        boolean wrong = inferDoor(sample);
        int p;
        // 7 GPS accuracy, 5 RSSI level, 6 RSSI value, 9 Wifi RSSI, 2 light density, 12 temperature
        double[] entry = new double[]{sample[7], sample[5], sample[6], sample[9], sample[2], sample[12], !wrong ? 1 : 0};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesDoor);
        p = AdaBoost.Poisson(LAMBDA);
        for (int k = 0; k < p; k++) {
            classifierDoor.updateClassifier(inst);
        }
    }

    // Update the model by online learning
    public void updateGround(double[] sample) throws Exception {
        boolean wrong = inferGround(sample);
        int p;
        // 5 RSSI level, 7 GPS accuracy (m), 12 temperature, 6 RSSI value, 13 pressure, 9 Wifi RSSI, 14 humidity
        double[] entry = new double[]{sample[5], sample[7], sample[12], sample[6], sample[13], sample[9], sample[14], !wrong ? 1 : 0};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(instancesGround);
        p = AdaBoost.Poisson(LAMBDA);
        for (int k = 0; k < p; k++) {
            classifierGround.updateClassifier(inst);
        }
    }

    // Get a hierarchical inference result from inference
    public String inferHierar(double[] sample) throws Exception {
        if (inferPocket(sample)) {
            hierarResult = 1;
            return "In-Pocket (Do nothing)";
        } else if (!inferDoor(sample)) {
            hierarResult = 2;
            return "Out-Door (Out-Pocket)";
        } else if (inferGround(sample)) {
            hierarResult = 3;
            return "Under-ground (In-Door)";
        } else {
            hierarResult = 4;
            return "On-ground (In-Door)";
        }
    }

    // Update the classifiers hierarchically
    public void updateHierar(double[] sample) throws Exception {
        switch (hierarResult) {
            case 1:
                updatePocket(sample);
                saveModels();
                break;
            case 2:
                updateDoor(sample);
                saveModels();
                break;
            case 3:
                updateGround(sample);
                saveModels();
                break;
            case 4:
                Random ran = new Random();
                if (ran.nextInt(2) == 0) {
                    Log.d(TAG, "Door update");
                    updateDoor(sample);
                    saveModels();
                } else {
                    Log.d(TAG, "Ground update");
                    updateGround(sample);
                    saveModels();
                 }
                break;
            default:
                Log.e(TAG, "Wrong inference result code");
                break;
        }
    }

    // Load models and data set format from files
    private void loadModels() {

        File filePocket = mContext.getFileStreamPath(MODEL_INPOCKET);
        File fileDoor = mContext.getFileStreamPath(MODEL_INDOOR);
        File fileGround = mContext.getFileStreamPath(MODEL_UNDERGROUND);

        FileInputStream fileInputStream;
        ObjectInputStream objectInputStream;
        FileOutputStream fileOutputStream;
        ObjectOutputStream objectOutputStream;

        // Check local models existence
        if (!filePocket.exists() || !fileDoor.exists() || !fileGround.exists()) {
            Log.d(TAG, "Local models do not exist.");
            try {
                // Load models from assets
                fileInputStream = mContext.getAssets().openFd(MODEL_INPOCKET).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierPocket = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = mContext.getAssets().openFd(DATASET_INPOCKET).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesPocket = (Instances) objectInputStream.readObject();

                fileInputStream = mContext.getAssets().openFd(MODEL_INDOOR).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierDoor = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = mContext.getAssets().openFd(DATASET_INDOOR).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesDoor = (Instances) objectInputStream.readObject();

                fileInputStream = mContext.getAssets().openFd(MODEL_UNDERGROUND).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierGround = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = mContext.getAssets().openFd(DATASET_UNDERGROUND).createInputStream();
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesGround = (Instances) objectInputStream.readObject();

                objectInputStream.close();
                fileInputStream.close();
                Log.d(TAG, "Success in loading from assets.");

                // Save models into app data
                fileOutputStream = mContext.openFileOutput(MODEL_INPOCKET, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierPocket);
                fileOutputStream = mContext.openFileOutput(DATASET_INPOCKET, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(instancesPocket);

                fileOutputStream = mContext.openFileOutput(MODEL_INDOOR, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierDoor);
                fileOutputStream = mContext.openFileOutput(DATASET_INDOOR, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(instancesDoor);

                fileOutputStream = mContext.openFileOutput(MODEL_UNDERGROUND, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(classifierGround);
                fileOutputStream = mContext.openFileOutput(DATASET_UNDERGROUND, Context.MODE_PRIVATE);
                objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(instancesGround);

                objectOutputStream.close();
                fileOutputStream.close();
                Log.d(TAG, "Success in saving into file.");
            } catch (Exception e) {
                Log.d(TAG, "Error when loading from file: " + e);
            }
        }
        // Local models already exist
        else {
            Log.d(TAG, "Local models already exist.");
            try {
                // Load models from app data
                fileInputStream = mContext.openFileInput(MODEL_INPOCKET);
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierPocket = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = mContext.openFileInput(DATASET_INPOCKET);
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesPocket = (Instances) objectInputStream.readObject();

                fileInputStream = mContext.openFileInput(MODEL_INDOOR);
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierDoor = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = mContext.openFileInput(DATASET_INDOOR);
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesDoor = (Instances) objectInputStream.readObject();

                fileInputStream = mContext.openFileInput(MODEL_UNDERGROUND);
                objectInputStream = new ObjectInputStream(fileInputStream);
                classifierGround = (HoeffdingTree) objectInputStream.readObject();
                fileInputStream = mContext.openFileInput(DATASET_UNDERGROUND);
                objectInputStream = new ObjectInputStream(fileInputStream);
                instancesGround = (Instances) objectInputStream.readObject();

                objectInputStream.close();
                fileInputStream.close();
                Log.d(TAG, "Success in loading from file.");
            } catch (Exception e) {
                Log.d(TAG, "Error when loading model file: " + e);
            }
        }
    }

    // Save the updated models
    private void saveModels() {
        FileOutputStream fileOutputStream;
        ObjectOutputStream objectOutputStream;

        try {
            // Save models into app data
            fileOutputStream = mContext.openFileOutput(MODEL_INPOCKET, Context.MODE_PRIVATE);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(classifierPocket);

            fileOutputStream = mContext.openFileOutput(MODEL_INDOOR, Context.MODE_PRIVATE);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(classifierDoor);

            fileOutputStream = mContext.openFileOutput(MODEL_UNDERGROUND, Context.MODE_PRIVATE);
            objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(classifierGround);

            objectOutputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            Log.d(TAG, "Error when saving model file: " + e);
        }
    }

    // Receiver for user feedback broadcast
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received: " + intent);
        String type = intent.getStringExtra("infer_type");
        Log.d(TAG, "Inference type is: " + type);
    }
}
