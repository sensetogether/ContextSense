package fr.inria.yifan.mysensor.Sensing;

/**
 * This class implements the A-weighting for noise measurement.
 */

class AWeighting {

    private int order; //always = aCoef.length - 1 = bCoef.length (but we keep it in a variable for convenience)
    private double[] aCoef;
    private double[] bCoef;
    private double[] conditions;

    // Constructor
    AWeighting(int sampleRate) {
        switch (sampleRate) {
            //8000 Hz
            case 8000:
                aCoef = new double[]{1.0d, -2.1284671930091217D,
                        0.29486689801012067D, 1.8241838307350515D,
                        -0.80566289431197835D, -0.39474979828429368D,
                        0.20985485460803321D};
                bCoef = new double[]{0.63062094682387282D, -1.2612418936477434D,
                        -0.63062094682387637D, 2.5224837872954899D,
                        -0.6306209468238686D, -1.2612418936477467D,
                        0.63062094682387237D};
                break;
            //16000 Hz
            case 16000:
                aCoef = new double[]{1.0d, -2.867832572992166100D,
                        2.221144410202319500D, 0.455268334788656860D,
                        -0.983386863616282910D, 0.055929941424134225D,
                        0.118878103828561270D};
                bCoef = new double[]{0.53148982982355708D, -1.0629796596471122D,
                        -0.53148982982356319D, 2.1259593192942332D,
                        -0.53148982982355686D, -1.0629796596471166D,
                        0.53148982982355797D};
                break;
            //22050 Hz
            case 22050:
                aCoef = new double[]{1.0d, -3.2290788052250736D,
                        3.3544948812360302D, -0.73178436806573255D,
                        -0.6271627581807262D, 0.17721420050208803D,
                        0.056317166973834924D};
                bCoef = new double[]{0.44929985042991927D, -0.89859970085984164D,
                        -0.4492998504299115D, 1.7971994017196726D,
                        -0.44929985042992043D, -0.89859970085983754D,
                        0.44929985042991943D};
                break;
            //24000 Hz
            case 24000:
                aCoef = new double[]{1.0d, -3.3259960042,
                        3.6771610793, -1.1064760768,
                        -0.4726706735, 0.1861941760,
                        0.0417877134};
                bCoef = new double[]{0.4256263893, -0.8512527786,
                        -0.4256263893, 1.7025055572,
                        -0.4256263893, -0.8512527786,
                        0.4256263893};
                break;
            //32000 Hz
            case 32000:
                aCoef = new double[]{1.0d, -3.6564460432,
                        4.8314684507, -2.5575974966,
                        0.2533680394, 0.1224430322,
                        0.0067640722};
                bCoef = new double[]{0.3434583387, -0.6869166774,
                        -0.3434583387, 1.3738333547,
                        -0.3434583387, -0.6869166774,
                        0.3434583387};
                break;
            //44100 Hz
            case 44100:
                aCoef = new double[]{1.0d, -4.0195761811158315D,
                        6.1894064429206921D, -4.4531989035441155D,
                        1.4208429496218764D, -0.14182547383030436D,
                        0.0043511772334950787D};
                bCoef = new double[]{0.2557411252042574D, -0.51148225040851436D,
                        -0.25574112520425807D, 1.0229645008170318D,
                        -0.25574112520425918D, -0.51148225040851414D,
                        0.25574112520425729D};
                break;
            //48000 Hz
            case 48000:
                aCoef = new double[]{1.0d, -4.113043408775872D,
                        6.5531217526550503D, -4.9908492941633842D,
                        1.7857373029375754D, -0.24619059531948789D,
                        0.011224250033231334D};
                bCoef = new double[]{0.2343017922995132D, -0.4686035845990264D,
                        -0.23430179229951431D, 0.9372071691980528D,
                        -0.23430179229951364D, -0.46860358459902524D,
                        0.23430179229951273D};
                break;
        }
        order = aCoef.length - 1; //= Bcoef.length - 1
        conditions = new double[order]; //initialize conditions (all 0.0d)
    }

    // Apply the A-weighting filter
    short[] apply(short input[]) {
        short[] output = new short[input.length];
        for (int i = 0; i < input.length; i++) {
            double x_i = input[i];
            //Filter sample:
            double y_i = x_i * bCoef[0] + conditions[0];
            //Store filtered sample:
            output[i] = (short) y_i;
            //Adjust conditions:
            // all but the last condition:
            for (int j = 0; j < order - 1; j++) {
                conditions[j] = x_i * bCoef[j + 1] - y_i * aCoef[j + 1] + conditions[j + 1];
            }
            // last condition:
            conditions[order - 1] = x_i * bCoef[order] - y_i * aCoef[order];
        }
        return output;
    }

}
