package fr.inria.yifan.mysensor.Inference;

import java.util.Random;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.HoeffdingTree;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Training and exportation of multi-class learning model
 */

public class TrainModelOne {

    // Main method for generating original model
    public static void main(String[] args) throws Exception {

        // Load data from CSV file
        DataSource source_train = new DataSource("/Users/yifan/OneDrive/INRIA/ContextSense/Training Data/GT-I9505_oneclass.csv");
        DataSource source_test = new DataSource("/Users/yifan/OneDrive/INRIA/ContextSense/Training Data/Redmi-Note4_2_oneclass.csv");
        Instances train = source_train.getDataSet();
        Instances test = source_test.getDataSet();

        /*
        1 timestamp, 2 daytime (b), 3 light density (lx), 4 magnetic strength (μT), 5 GSM active (b),
        6 RSSI level, 7 RSSI value (dBm), 8 GPS accuracy (m), 9 Wifi active (b), 10 Wifi RSSI (dBm),
        11 proximity (b), 12 sound level (dBA), 13 temperature (C), 14 pressure (hPa), 15 humidity (%),
        16 in-pocket label, 17 in-door label, 18 under-ground label, 19 one-class label
        */

        // Keep all possible attributes
        Remove remove = new Remove();
        remove.setAttributeIndices("3, 6, 7, 8, 10, 11, 13, 14, 15, 19");

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

        HoeffdingTree classifier = new HoeffdingTree();

        // 10-fold cross validation
        Evaluation cross = new Evaluation(newTrain);
        cross.crossValidateModel(classifier, newTrain, 10, new Random());
        System.out.println(cross.toSummaryString());

        // Build the classifier
        classifier.buildClassifier(newTrain);

        // Save and load
        SerializationHelper.write("/Users/yifan/Documents/MySensor/app/src/main/assets/Classifier_one.model", classifier);
        Instances dataSet = new Instances(newTrain, 0);
        SerializationHelper.write("/Users/yifan/Documents/MySensor/app/src/main/assets/Dataset_one.model", dataSet);

        // Evaluate classifier on data set
        Evaluation eva = new Evaluation(newTest);
        eva.evaluateModel(classifier, newTest);
        System.out.println(eva.pctCorrect());
    }
}
