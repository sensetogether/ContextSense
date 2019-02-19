package fr.inria.yifan.mysensor.Inference;

import java.util.Random;

import weka.classifiers.trees.HoeffdingTree;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Training and exportation of binary learning models
 */

public class TrainModelBinary {

    // Main method for generating original models
    public static void main(String[] args) throws Exception {

        // Load data from CSV files
        DataSource source_train = new DataSource("/Users/yifan/OneDrive/INRIA/ContextSense/Training Data/GT-I9505.csv");
        DataSource source_test = new DataSource("/Users/yifan/OneDrive/INRIA/ContextSense/Training Data/Redmi-Note4_2.csv");
        Instances train = source_train.getDataSet();
        Instances test = source_test.getDataSet();

        /*
        1 timestamp, 2 daytime (b), 3 light density (lx), 4 magnetic strength (μT), 5 GSM active (b),
        6 RSSI level, 7 RSSI value (dBm), 8 GPS accuracy (m), 9 Wifi active (b), 10 Wifi RSSI (dBm),
        11 proximity (b), 12 sound level (dBA), 13 temperature (C), 14 pressure (hPa), 15 humidity (%),
        16 in-pocket label, 17 in-door label, 18 under-ground label
        */

        // Only keep used feature attributes
        Remove remove = new Remove();
        remove.setAttributeIndices("11, 13, 3, 16");
        //remove.setAttributeIndices("8, 6, 7, 10, 3, 13, 17");
        //remove.setAttributeIndices("6, 8, 13, 7, 14, 10, 15, 18");

        remove.setInvertSelection(true);
        remove.setInputFormat(train);
        Instances revTrain = Filter.useFilter(train, remove);
        Instances revTest = Filter.useFilter(test, remove);

        // Convert class to nominal
        NumericToNominal nominal = new NumericToNominal();
        nominal.setAttributeIndices(String.valueOf(revTrain.numAttributes()));
        nominal.setInputFormat(revTrain);
        Instances newTrain = Filter.useFilter(revTrain, nominal);
        Instances newTest = Filter.useFilter(revTest, nominal);

        // Set data class label index
        newTrain.setClassIndex(newTrain.numAttributes() - 1);
        newTest.setClassIndex(newTest.numAttributes() - 1);
        newTrain.randomize(new Random());
        newTest.randomize(new Random());

        // Show all attributes
        System.out.print("Features:");
        for (int i = 0; i < newTrain.numAttributes() - 1; i++) {
            System.out.print(newTrain.attribute(i).name());
        }
        System.out.println(" Target:" + newTrain.classAttribute().name());
        System.out.print("Features:");
        for (int i = 0; i < newTest.numAttributes() - 1; i++) {
            System.out.print(newTest.attribute(i).name());
        }
        System.out.println(" Target:" + newTest.classAttribute().name());

        // Build the classifier
        HoeffdingTree classifier = new HoeffdingTree();
        classifier.buildClassifier(newTrain);

        // Save and load classifier
        SerializationHelper.write("/Users/yifan/Documents/MySensor/app/src/main/assets/Classifier_pocket.model", classifier);
        Instances dataSet = new Instances(newTrain, 0);
        SerializationHelper.write("/Users/yifan/Documents/MySensor/app/src/main/assets/Dataset_pocket.model", dataSet);

        //SerializationHelper.write("/Users/yifan/Documents/MySensor/app/src/main/assets/Classifier_door.model", classifier);
        //Instances dataSet = new Instances(newTrain, 0);
        //SerializationHelper.write("/Users/yifan/Documents/MySensor/app/src/main/assets/Dataset_door.model", dataSet);

        //SerializationHelper.write("/Users/yifan/Documents/MySensor/app/src/main/assets/Classifier_ground.model", classifier);
        //Instances dataSet = new Instances(newTrain, 0);
        //SerializationHelper.write("/Users/yifan/Documents/MySensor/app/src/main/assets/Dataset_ground.model", dataSet);

        // Classify new instances
        classifier = (HoeffdingTree) SerializationHelper.read("/Users/yifan/Documents/MySensor/app/src/main/assets/Classifier_pocket.model");
        dataSet = (Instances) SerializationHelper.read("/Users/yifan/Documents/MySensor/app/src/main/assets/Dataset_door.model");

        // Show all attributes
        System.out.print("Features:");
        for (int i = 0; i < dataSet.numAttributes() - 1; i++) {
            System.out.print(dataSet.attribute(i).name());
        }
        System.out.println(" Target:" + dataSet.classAttribute().name());

        double[] entry = new double[]{1, 2, 3, 4};
        Instance inst = new DenseInstance(1, entry);
        inst.setDataset(dataSet);
        int result = (int) classifier.classifyInstance(inst);
        System.out.println("Sample: " + inst + ", Inference: " + result);
    }
}
