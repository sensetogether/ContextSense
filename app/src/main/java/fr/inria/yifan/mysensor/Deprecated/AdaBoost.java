package fr.inria.yifan.mysensor.Deprecated;

import java.io.Serializable;

/*
 This class implements the AdaBoost model for binary classification
 */

public class AdaBoost implements Serializable {

    private static final String TAG = "AdaBoost";
    private static final long serialVersionUID = 1000L;

    private int numLearn; // Number of weak learners (decision stump)
    private int[] features; // The indexes of features used for learning
    private int stepNumber; // Number of steps for threshold increasing
    private DecisionStump[] dStumps; // An array storing decision stumps
    private double[] alphas; // Alpha coefficient for all decision stumps

    // Constructor initialization
    AdaBoost(int numLearner, int[] featureInd, int stepLearn) {
        numLearn = numLearner;
        features = featureInd;
        stepNumber = stepLearn;
        dStumps = new DecisionStump[numLearn];
        alphas = new double[numLearn];
    }

    // Public method to generate a Poisson number
    public static int Poisson(double lambda) {
        double L = Math.exp(-lambda);
        double p = 1d;
        int k = 0;
        do {
            k++;
            p *= Math.random();
        } while (p > L);
        return k - 1;
    }

    // Train the model from a array of samples
    void BatchTrain(double[][] samples) {
        // Initial the weights for all samples
        double[] weight = new double[samples.length];
        for (int j = 0; j < weight.length; j++) {
            weight[j] = 1d / samples.length;
        }
        // Train each decision stump
        for (int i = 0; i < dStumps.length; i++) {
            dStumps[i] = new DecisionStump();
            dStumps[i].BatchTrain(samples, weight, features, stepNumber);
            double epsilon = dStumps[i].getError();
            // Calculate the coefficient
            alphas[i] = Math.log((1d - epsilon) / epsilon);
            // Update weights for all samples
            for (int j = 0; j < weight.length; j++) {
                weight[j] = weight[j] * (dStumps[i].Predict(samples[j]) == samples[j][samples[j].length - 1]
                        ? 1d / (2d - 2d * epsilon)
                        : 1d / (2d * epsilon));
            }
        }
    }

    // Online AdaBoost update using only one sample
    void OnlineUpdate(double[] sample) {
        double lambda = 1d;
        // Update each decision stump
        for (int i = 0; i < numLearn; i++) {
            double lambda_right = 0d;
            double lambda_wrong = 0d;
            int k = Poisson(lambda);
            for (int j = 0; j < k; j++) {
                //dStumps[i].PoissonUpdate(sample);
                dStumps[i].UpdateThreshold(sample);
            }
            // Calculate the coefficient
            if (dStumps[i].Predict(sample) == sample[sample.length - 1]) {
                lambda_right = lambda_right + lambda;
                double epsilon = lambda_wrong / (lambda_right + lambda_wrong);
                dStumps[i].setError(epsilon);
                lambda = lambda * (1 / (2 - 2 * epsilon));
                alphas[i] = Math.log((1d - epsilon) / epsilon);
            } else {
                lambda_wrong = lambda_wrong + lambda;
                double epsilon = lambda_wrong / (lambda_right + lambda_wrong);
                dStumps[i].setError(epsilon);
                lambda = lambda * (1 / (2 * epsilon));
                alphas[i] = Math.log((1d - epsilon) / epsilon);
            }
        }
    }

    // Make inference on a new feature vector
    int Predict(double[] feature) {
        double result_1 = 0d;
        double result_0 = 0d;
        // Predict the class using all decision stumps
        for (int i = 0; i < numLearn; i++) {
            if (dStumps[i].Predict(feature) == 1) {
                result_1 += alphas[i];
            } else if (dStumps[i].Predict(feature) == 0) {
                result_0 += alphas[i];
            } else {
                throw new IllegalArgumentException("Illegal prediction.");
            }
        }
        return result_1 > result_0 ? 1 : 0;
    }
}
