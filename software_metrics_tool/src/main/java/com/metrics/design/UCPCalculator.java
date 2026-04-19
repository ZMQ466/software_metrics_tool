package com.metrics.design;

import com.metrics.model.UCPInput;

public class UCPCalculator {
    private static final double[] TECHNICAL_WEIGHTS = new double[] {2, 1, 1, 1, 1, 0.5, 0.5, 2, 1, 1, 1, 1, 1};
    private static final double[] ENVIRONMENTAL_WEIGHTS = new double[] {1.5, 0.5, 1, 1, 1, 2, -1, -1};

    public UCPResult calculate(UCPInput input) {
        double uaw = input.getSimpleActors() * 1.0 + input.getAverageActors() * 2.0 + input.getComplexActors() * 3.0;
        double uucw = input.getSimpleUseCases() * 5.0 + input.getAverageUseCases() * 10.0 + input.getComplexUseCases() * 15.0;
        double uucp = uaw + uucw;

        double tf = 0.0;
        int[] technical = input.getTechnicalFactors();
        for (int i = 0; i < TECHNICAL_WEIGHTS.length; i++) {
            int score = 0;
            if (technical != null && i < technical.length) {
                score = technical[i];
            }
            tf += score * TECHNICAL_WEIGHTS[i];
        }
        double tcf = 0.6 + 0.01 * tf;

        double ef = 0.0;
        int[] environmental = input.getEnvironmentalFactors();
        for (int i = 0; i < ENVIRONMENTAL_WEIGHTS.length; i++) {
            int score = 0;
            if (environmental != null && i < environmental.length) {
                score = environmental[i];
            }
            ef += score * ENVIRONMENTAL_WEIGHTS[i];
        }
        double ecf = 1.4 + (-0.03) * ef;

        double ucp = uucp * tcf * ecf;
        return new UCPResult(uaw, uucw, uucp, tcf, ecf, ucp);
    }
}
