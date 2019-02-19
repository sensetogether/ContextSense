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
 * Evaluation test on the accuracy of a multi-class model
 */

public class OneClassAccuracy {

    // Main method for evaluating models
    public static void main(String[] args) throws Exception {

        // Run multiple experiments
        int run = 500;

        StringBuilder log_one = new StringBuilder();

        // Loop for multiple runs
        for (int i = 0; i < run; i++) {

            // Load one classifier
            HoeffdingTree classifier_one = (HoeffdingTree) SerializationHelper.read("/Users/yifan/Documents/MySensor/app/src/main/assets/Classifier_one.model");

            // Load data from CSV file
            ConverterUtils.DataSource source_test = new ConverterUtils.DataSource("/Users/yifan/OneDrive/INRIA/ContextSense/Training Data/Redmi-Note4_2_oneclass.csv");
            Instances test = source_test.getDataSet();
            test.randomize(new Random());

            // Only keep used attributes
            Remove remove = new Remove();
            remove.setInvertSelection(true);
            // Convert class to nominal
            NumericToNominal nominal = new NumericToNominal();

            // Keep all possible attributes
            remove.setAttributeIndices("3, 6, 7, 8, 10, 11, 13, 14, 15, 19");
            remove.setInputFormat(test);
            Instances revTest = Filter.useFilter(test, remove);
            nominal.setAttributeIndices(String.valueOf(revTest.numAttributes()));
            nominal.setInputFormat(revTest);
            Instances newTest = Filter.useFilter(revTest, nominal);
            newTest.setClassIndex(newTest.numAttributes() - 1);

            // Parameter for Poisson number
            double lambda = 10d;
            // Wrong inference counter
            int count_err = 0;

            // Limit the feedback amount to 30
            for (int j = 0; j < 30; j++) {
                //System.out.println("Iteration: " + j);
                // Sequential feedback on wrong inference
                if (classifier_one.classifyInstance(newTest.instance(j)) != newTest.instance(j).classValue()) {
                    {
                        int p = AdaBoost.Poisson(lambda);
                        for (int k = 0; k < p; k++) {
                            classifier_one.updateClassifier(newTest.instance(j));
                        }
                        count_err += 1;
                        Evaluation eva = new Evaluation(newTest);
                        eva.evaluateModel(classifier_one, newTest);
                        double acc = eva.pctCorrect();
                        System.out.println(i + "th run, feedback: " + count_err + ", one accuracy: " + acc);
                        log_one.append(count_err).append(", ").append(acc).append("\n");
                    }
                }
            }

            // Save the log file
            String logfile = "/Users/yifan/Documents/MySensor/app/src/main/assets/CA_One_MultiClass_10";
            FileOutputStream output = new FileOutputStream(logfile);
            output.write(log_one.toString().getBytes());
            output.close();
        }
    }
}