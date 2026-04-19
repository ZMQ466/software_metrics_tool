package com.metrics.model;

/**
 * Unified input model for UCP calculation.
 */
public class UCPInput {
    // Use-case counts (UUCW)
    private int simpleUseCases;
    private int averageUseCases;
    private int complexUseCases;

    // Actor counts (UAW)
    private int simpleActors;
    private int averageActors;
    private int complexActors;

    // Technical factors (13 items, each 0-5)
    private int[] technicalFactors;

    // Environmental factors (8 items, each 0-5)
    private int[] environmentalFactors;

    public UCPInput() {
        this.technicalFactors = new int[13];
        this.environmentalFactors = new int[8];
    }

    public int getSimpleUseCases() {
        return simpleUseCases;
    }

    public void setSimpleUseCases(int simpleUseCases) {
        this.simpleUseCases = simpleUseCases;
    }

    public int getAverageUseCases() {
        return averageUseCases;
    }

    public void setAverageUseCases(int averageUseCases) {
        this.averageUseCases = averageUseCases;
    }

    public int getComplexUseCases() {
        return complexUseCases;
    }

    public void setComplexUseCases(int complexUseCases) {
        this.complexUseCases = complexUseCases;
    }

    public int getSimpleActors() {
        return simpleActors;
    }

    public void setSimpleActors(int simpleActors) {
        this.simpleActors = simpleActors;
    }

    public int getAverageActors() {
        return averageActors;
    }

    public void setAverageActors(int averageActors) {
        this.averageActors = averageActors;
    }

    public int getComplexActors() {
        return complexActors;
    }

    public void setComplexActors(int complexActors) {
        this.complexActors = complexActors;
    }

    public int[] getTechnicalFactors() {
        return technicalFactors;
    }

    public void setTechnicalFactors(int[] technicalFactors) {
        this.technicalFactors = technicalFactors;
    }

    public int[] getEnvironmentalFactors() {
        return environmentalFactors;
    }

    public void setEnvironmentalFactors(int[] environmentalFactors) {
        this.environmentalFactors = environmentalFactors;
    }
}
