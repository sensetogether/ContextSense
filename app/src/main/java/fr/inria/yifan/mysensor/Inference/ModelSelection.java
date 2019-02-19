package fr.inria.yifan.mysensor.Inference;

import java.io.FileOutputStream;
import java.util.Random;

import fr.inria.yifan.mysensor.Deprecated.AdaBoost;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.HoeffdingTree;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Comparison of different online learning models
 */

public class ModelSelection {

    // Main method for comparing learning models
    public static void main(String[] args) throws Exception {

        // Load data from CSV file
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

        // Learning model selection
        HoeffdingTree classifier = new HoeffdingTree();
        //IBk classifier = new IBk();
        //KStar classifier = new KStar();
        //LWL classifier = new LWL();
        //NaiveBayesUpdateable classifier = new NaiveBayesUpdateable();
        //SGD classifier = new SGD();

        // 10-fold cross validation
        Evaluation cross = new Evaluation(newTrain);
        cross.crossValidateModel(classifier, newTrain, 10, new Random());
        System.out.println(cross.toSummaryString());

        // Build the classifier
        classifier.buildClassifier(newTrain);

        // Evaluate classifier on data set
        Evaluation eva = new Evaluation(newTest);
        eva.evaluateModel(classifier, newTest);
        System.out.println(eva.toSummaryString());

        // Multiply runs for evaluation
        int run = 100;
        // For generating Poisson number
        double lambda = 10d;

        // Runtime evaluation
        long startTime;
        long endTime;
        long totalTime_b;
        long totalTime_o;
        long totalTime_i;

        totalTime_b = 0;
        totalTime_o = 0;
        totalTime_i = 0;
        for (int i = 0; i < run; i++) {
            classifier = new HoeffdingTree();
            //classifier = new IBk();
            //classifier = new KStar();
            //classifier = new LWL();
            //classifier = new NaiveBayesUpdateable();
            //classifier = new SGD();

            startTime = System.nanoTime();
            classifier.buildClassifier(newTrain);
            endTime = System.nanoTime();
            totalTime_b += (endTime - startTime);
            //System.out.println((endTime - startTime) / 1000000d);

            startTime = System.nanoTime();
            classifier.updateClassifier(newTest.instance(i));
            endTime = System.nanoTime();
            totalTime_o += (endTime - startTime);
            //System.out.println((endTime - startTime) / 1000000d);

            startTime = System.nanoTime();
            classifier.classifyInstance(newTest.instance(i));
            endTime = System.nanoTime();
            totalTime_i += (endTime - startTime);
            //System.out.println((endTime - startTime) / 1000000d);
        }
        System.out.println("Training time (batch) ms: " + (totalTime_b / run) / 1000000d);
        System.out.println("Updating time (online) ms: " + (totalTime_o / run) / 1000000d);
        System.out.println("Classification time ms: " + (totalTime_i / run) / 1000000d);

        // Accuracy evaluation
        int count_err;
        int count_max;
        double acc_max;
        StringBuilder log = new StringBuilder();

        // Loop for multiple runs
        for (int i = 0; i < run; i++) {
            // Randomize each run!
            Random random = new Random();
            newTest.randomize(random);

            // New classifier each run
            classifier = new HoeffdingTree();
            //classifier = new IBk();
            //classifier = new KStar();
            //classifier = new LWL();
            //classifier = new NaiveBayesUpdateable();
            //classifier = new SGD();
            classifier.buildClassifier(newTrain);

            count_err = 0;
            count_max = 0;
            // Evaluate classifier on data set
            Evaluation eva1 = new Evaluation(newTest);
            eva1.evaluateModel(classifier, newTest);
            //System.out.println(eva1.toSummaryString());
            acc_max = eva1.pctCorrect();

            // Limit the feedback amount to 30
            for (int j = 0; j < 30; j++) {
                // Sequential feedback on wrong inference
                //if (classifier.classifyInstance(newTest.instance(j)) != newTest.instance(j).classValue()) {
                // Generate Poisson number
                int p = AdaBoost.Poisson(lambda);
                //System.out.println("K value = " + k);
                for (int k = 0; k < p; k++) {
                    classifier.updateClassifier(newTest.instance(j));
                }
                count_err++;
                Evaluation eva2 = new Evaluation(newTest);
                eva2.evaluateModel(classifier, newTest);
                double acc = eva2.pctCorrect();
                //System.out.println("Accuracy: " + acc);
                // Record max accuracy and feedback count
                if (acc > acc_max) {
                    acc_max = acc;
                    count_max = count_err;
                }
                //}
            }
            System.out.println(i + "th run, number of feedback: " + count_max + ", max accuracy: " + acc_max);
            log.append(count_max).append(", ").append(acc_max).append("\n");
        }

        // Save the log file
        String logfile = "/Users/yifan/Documents/MySensor/app/src/main/assets/CA_HTree_Pocket_10";
        FileOutputStream output = new FileOutputStream(logfile);
        output.write(log.toString().getBytes());
        output.close();

    }
}
