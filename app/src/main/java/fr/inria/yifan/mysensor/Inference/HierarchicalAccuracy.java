package fr.inria.yifan.mysensor.Inference;

import java.io.FileOutputStream;
import java.util.Random;

import fr.inria.yifan.mysensor.Deprecated.AdaBoost;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.HoeffdingTree;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Evaluation test on the accuracy of hierarchical models
 */

public class HierarchicalAccuracy {

    // Main method for evaluating models
    public static void main(String[] args) throws Exception {

        // Run multiple experiments
        int run = 500;

        StringBuilder log_pocket = new StringBuilder();
        StringBuilder log_door = new StringBuilder();
        StringBuilder log_ground = new StringBuilder();

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

            // Evaluate classifier on data set
            Evaluation eval = new Evaluation(test_pocket);
            eval.evaluateModel(classifier_pocket, test_pocket);
            System.out.println(eval.fMeasure(0));
            Evaluation eva2 = new Evaluation(test_door);
            eva2.evaluateModel(classifier_door, test_door);
            System.out.println(eva2.fMeasure(0));
            Evaluation eva3 = new Evaluation(test_ground);
            eva3.evaluateModel(classifier_ground, test_ground);
            System.out.println(eva3.fMeasure(0));

            // Parameter for Poisson number
            double lambda = 10d;
            // Wrong inference counter
            int count_err = 0;

            // Limit the feedback amount to 30
            for (int j = 0; j < 30; j++) {
                //System.out.println("Iteration: " + j);
                // Hierarchical feedback on wrong inference
                if (classifier_pocket.classifyInstance(test_pocket.instance(j)) == 1) {
                    if (test_pocket.instance(j).classValue() == 0) {
                        int p = AdaBoost.Poisson(lambda);
                        for (int k = 0; k < p; k++) {
                            classifier_pocket.updateClassifier(test_pocket.instance(j));
                        }
                        count_err += 1;
                        Evaluation eva = new Evaluation(test_pocket);
                        eva.evaluateModel(classifier_pocket, test_pocket);
                        double acc = eva.pctCorrect();
                        //double acc = eva.fMeasure(0);
                        System.out.println(i + "th run, feedback: " + count_err + ", pocket accuracy: " + acc);
                        log_pocket.append(count_err).append(", ").append(acc).append("\n");
                    }
                } else if (classifier_door.classifyInstance(test_door.instance(j)) == 0) {
                    if (test_door.instance(j).classValue() == 1) {
                        int p = AdaBoost.Poisson(lambda);
                        for (int k = 0; k < p; k++) {
                            classifier_door.updateClassifier(test_door.instance(j));
                        }
                        count_err += 1;
                        Evaluation eva = new Evaluation(test_door);
                        eva.evaluateModel(classifier_door, test_door);
                        double acc = eva.pctCorrect();
                        //double acc = eva.fMeasure(0);
                        System.out.println(i + "th run, feedback: " + count_err + ", door accuracy: " + acc);
                        log_door.append(count_err).append(", ").append(acc).append("\n");
                    }
                } else if (classifier_ground.classifyInstance(test_ground.instance(j)) == 1) {
                    if (test_ground.instance(j).classValue() == 0) {
                        int p = AdaBoost.Poisson(lambda);
                        for (int k = 0; k < p; k++) {
                            classifier_ground.updateClassifier(test_ground.instance(j));
                        }
                        count_err += 1;
                        Evaluation eva = new Evaluation(test_ground);
                        eva.evaluateModel(classifier_ground, test_ground);
                        double acc = eva.pctCorrect();
                        //double acc = eva.fMeasure(0);
                        System.out.println(i + "th run, feedback: " + count_err + ", ground accuracy: " + acc);
                        log_ground.append(count_err).append(", ").append(acc).append("\n");
                    }
                } else {
                    if (test_ground.instance(j).classValue() == 1) {
                        int p = AdaBoost.Poisson(lambda);
                        for (int k = 0; k < p; k++) {
                            classifier_ground.updateClassifier(test_ground.instance(j));
                        }
                        count_err += 1;
                        Evaluation eva = new Evaluation(test_ground);
                        eva.evaluateModel(classifier_ground, test_ground);
                        double acc = eva.pctCorrect();
                        //double acc = eva.fMeasure(0);
                        System.out.println(i + "th run, feedback: " + count_err + ", ground accuracy: " + acc);
                        log_ground.append(count_err).append(", ").append(acc).append("\n");
                    } else if (test_door.instance(j).classValue() == 0) {
                        int p = AdaBoost.Poisson(lambda);
                        for (int k = 0; k < p; k++) {
                            classifier_door.updateClassifier(test_door.instance(j));
                        }
                        count_err += 1;
                        Evaluation eva = new Evaluation(test_door);
                        eva.evaluateModel(classifier_door, test_door);
                        double acc = eva.pctCorrect();
                        //double acc = eva.fMeasure(0);
                        System.out.println(i + "th run, feedback: " + count_err + ", door accuracy: " + acc);
                        log_door.append(count_err).append(", ").append(acc).append("\n");
                    }
                }
            }

            // Save the accuracy log files
            String logfile = "/Users/yifan/Documents/MySensor/app/src/main/assets/CA_pocket_Hierarchical_10";
            FileOutputStream output = new FileOutputStream(logfile);
            output.write(log_pocket.toString().getBytes());
            logfile = "/Users/yifan/Documents/MySensor/app/src/main/assets/CA_door_Hierarchical_10";
            output = new FileOutputStream(logfile);
            output.write(log_door.toString().getBytes());
            logfile = "/Users/yifan/Documents/MySensor/app/src/main/assets/CA_ground_Hierarchical_10";
            output = new FileOutputStream(logfile);
            output.write(log_ground.toString().getBytes());
            output.close();
        }
    }
}
