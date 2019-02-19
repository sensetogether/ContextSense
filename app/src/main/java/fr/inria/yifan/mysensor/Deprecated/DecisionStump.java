package fr.inria.yifan.mysensor.Deprecated;

import java.io.Serializable;

/*
 This class implements a basic decision stump model
 */

public class DecisionStump implements Serializable {

    private static final String TAG = "Decision stump";
    private static final long serialVersionUID = 1001L;

    private double error; // Minimal error of prediction
    private int index; // Index of feature attribute
    private char operation; // Operation '>' or '<='
    private double threshold; // Value of threshold
    private double stepValue; // Steps os threshold

    // Constructor initialization
    DecisionStump() {
        error = Double.MAX_VALUE;
        index = -1;
        operation = ' ';
        threshold = 0d;
        stepValue = 0d;
    }

    // Find the best decision stump from a sample set
    void BatchTrain(double[][] samples, double[] weight, int[] featureInd, int stepNum) {
        // Iteration for each candidate feature
        for (int i : featureInd) {
            // Find Max and Min for the feature
            double maxF_i = Double.NEGATIVE_INFINITY;
            double minF_i = Double.POSITIVE_INFINITY;
            for (double[] sample : samples) {
                maxF_i = sample[i] > maxF_i ? sample[i] : maxF_i;
                minF_i = sample[i] < minF_i ? sample[i] : minF_i;
            }
            // Calculate step of threshold for the feature
            double stepValue_i = (maxF_i - minF_i) / stepNum;
            // Iteration on all candidate threshold values
            for (int j = 0; j < stepNum; j++) {
                double threshold_i = minF_i + j * stepValue_i;
                double weightErrorLt = 0d; // Less than operation error
                double weightErrorGt = 0d; // Greater than operation error
                // Iteration on all samples
                for (int k = 0; k < samples.length; k++) {
                    // Weighted sum of prediction error, a right prediction gives 0
                    weightErrorLt += Math.abs(
                            (samples[k][i] <= threshold_i ? 1d : 0d) - samples[k][samples[k].length - 1]) * weight[k];
                    weightErrorGt += Math.abs(
                            (samples[k][i] > threshold_i ? 1d : 0d) - samples[k][samples[k].length - 1]) * weight[k];
                }
                // Update the minimal sum of weighted error
                if (weightErrorLt < error) {
                    error = weightErrorLt;
                    index = i;
                    operation = '(';
                    threshold = threshold_i;
                    stepValue = stepValue_i;
                }
                if (weightErrorGt < error) {
                    error = weightErrorGt;
                    index = i;
                    operation = '>';
                    threshold = threshold_i;
                    stepValue = stepValue_i;
                }
            }
        }
        // Here the best decision stump is found
    }

    // Update the threshold if the prediction is wrong
    void UpdateThreshold(double[] sample) {
        if (Predict(sample) != sample[sample.length - 1]) {
            //System.out.println("Current FE: " + index + ", OP: " + operation + ", TH: " + threshold +
            // ", VA: " + sample[index] + ", HY: " + Predict(sample) + ", TR: " + sample[sample.length - 1]);
            switch (operation) {
                case '(':
                    threshold += stepValue;
                    break;
                case '>':
                    threshold -= stepValue;
                    break;
                default:
                    throw new IllegalArgumentException("Illegal operation: " + operation);
            }
            //System.out.println("New FE: " + index + ", OP: " + operation + ", TH: " + threshold +
            // ", VA: " + sample[index] + ", H: " + Predict(sample) + ", TR: " + sample[sample.length - 1]);
        }
    }

    // Update the threshold with no condition
    public void PoissonUpdate(double[] sample) {
        //System.out.println("Current FE: " + index + ", OP: " + operation + ", TH: " + threshold +
        // ", VA: " + sample[index] + ", HY: " + Predict(sample) + ", TR: " + sample[sample.length - 1]);
        switch (operation) {
            case '(':
                threshold += stepValue;
                break;
            case '>':
                threshold -= stepValue;
                break;
            default:
                throw new IllegalArgumentException("Illegal operation: " + operation);
        }
        //System.out.println("New FE: " + index + ", OP: " + operation + ", TH: " + threshold +
        // ", VA: " + sample[index] + ", H: " + Predict(sample) + ", TR: " + sample[sample.length - 1]);
    }

    // Make inference on a new feature vector
    int Predict(double[] features) {
        switch (operation) {
            case '(':
                return features[index] <= threshold ? 1 : 0;
            case '>':
                return features[index] > threshold ? 1 : 0;
            default:
                throw new IllegalArgumentException("Illegal operation: " + operation);
        }
    }

    // Get the error of this decision stump
    double getError() {
        return error;
    }

    // Update the error of this decision stump
    void setError(double err) {
        error = err;
    }

}
