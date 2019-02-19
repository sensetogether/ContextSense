package fr.inria.yifan.mysensor.Inference;

import java.io.FileOutputStream;
import java.util.Random;

import fr.inria.yifan.mysensor.Deprecated.AdaBoost;
import weka.classifiers.trees.HoeffdingTree;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Evaluation test on the runtime of hierarchical models
 */

public class HierarchicalRuntime {

    // Main method for evaluating models
    public static void main(String[] args) throws Exception {

        // Run multiple experiments
        int run = 500;

        StringBuilder log_time = new StringBuilder();

        // Loop for multiple runs
        for (int i = 0; i < run; i++) {

            // Load three classifiers
            HoeffdingTree classifier_pocket = (HoeffdingTree) SerializationHelper.read("/Users/yifan/Documents/MySensor/app/src/main/assets/Classifier_pocket.model");
            HoeffdingTree classifier_door = (HoeffdingTree) SerializationHelper.read("/Users/yifan/Documents/MySensor/app/src/main/assets/Classifier_door.model");
            HoeffdingTree classifier_ground = (HoeffdingTree) SerializationHelper.read("/Users/yifan/Documents/MySensor/app/src/main/assets/Classifier_ground.model");

            // Load data from CSV file
            ConverterUtils.DataSource source_test = new ConverterUtils.DataSource("/Users/yifan/OneDrive/INRIA/ContextSense/Training Data/Redmi-Note4_2.csv");
            Instances test = source_test.getDataSet();
            test.randomize(new Random());

            // Only keep used attributes
            Remove remove = new Remove();
            remove.setInvertSelection(true);
            // Convert class to nominal
            NumericToNominal nominal = new NumericToNominal();

            // Instances for in-pocket classification
            remove.setAttributeIndices("11, 13, 3, 16");
            remove.setInputFormat(test);
            Instances test_pocket = Filter.useFilter(test, remove);
            nominal.setAttributeIndices(String.valueOf(test_pocket.numAttributes()));
            nominal.setInputFormat(test_pocket);
            test_pocket = Filter.useFilter(test_pocket, nominal);
            test_pocket.setClassIndex(test_pocket.numAttributes() - 1);

            // Instances for in-door classification
            remove.setAttributeIndices("8, 6, 7, 10, 3, 13, 17");
            remove.setInputFormat(test);
            Instances test_door = Filter.useFilter(test, remove);
            nominal.setAttributeIndices(String.valueOf(test_door.numAttributes()));
            nominal.setInputFormat(test_door);
            test_door = Filter.useFilter(test_door, nominal);
            test_door.setClassIndex(test_door.numAttributes() - 1);

            // Instances for under-ground classification
            remove.setAttributeIndices("6, 8, 13, 7, 14, 10, 15, 18");
            remove.setInputFormat(test);
            Instances test_ground = Filter.useFilter(test, remove);
            nominal.setAttributeIndices(String.valueOf(test_ground.numAttributes()));
            nominal.setInputFormat(test_ground);
            test_ground = Filter.useFilter(test_ground, nominal);
            test_ground.setClassIndex(test_ground.numAttributes() - 1);

            // Parameter for Poisson number
            double lambda = 10d;

            // Runtime evaluation
            long startTime;
            long endTime;
            long totalTime;

            startTime = System.nanoTime();
            if (classifier_pocket.classifyInstance(test_pocket.instance(99)) == 1) {
                if (test_pocket.instance(99).classValue() == 0) {
                    int p = AdaBoost.Poisson(lambda);
                    for (int k = 0; k < p; k++) {
                        classifier_pocket.updateClassifier(test_pocket.instance(99));
                    }
                }
            } else if (classifier_door.classifyInstance(test_door.instance(99)) == 0) {
                if (test_door.instance(99).classValue() == 1) {
                    int p = AdaBoost.Poisson(lambda);
                    for (int k = 0; k < p; k++) {
                        classifier_door.updateClassifier(test_door.instance(99));
                    }
                }
            } else if (classifier_ground.classifyInstance(test_ground.instance(99)) == 1) {
                if (test_ground.instance(99).classValue() == 0) {
                    int p = AdaBoost.Poisson(lambda);
                    for (int k = 0; k < p; k++) {
                        classifier_ground.updateClassifier(test_ground.instance(99));
                    }
                }
            } else {
                if (test_ground.instance(99).classValue() == 1) {
                    int p = AdaBoost.Poisson(lambda);
                    for (int k = 0; k < p; k++) {
                        classifier_ground.updateClassifier(test_ground.instance(99));
                    }
                } else if (test_door.instance(99).classValue() == 0) {
                    int p = AdaBoost.Poisson(lambda);
                    for (int k = 0; k < p; k++) {
                        classifier_door.updateClassifier(test_door.instance(99));
                    }
                }
            }
            endTime = System.nanoTime();

            totalTime = (endTime - startTime);
            System.out.println("Runtime ms: " + totalTime / 1000000d);
            log_time.append(i).append(", ").append(totalTime / 1000000d).append("\n");
        }

        // Save the runtime log file
        String logfile = "/Users/yifan/Documents/MySensor/app/src/main/assets/UpdateTime_Hierarchical_10";
        FileOutputStream output = new FileOutputStream(logfile);
        output.write(log_time.toString().getBytes());
        output.close();
    }
}
