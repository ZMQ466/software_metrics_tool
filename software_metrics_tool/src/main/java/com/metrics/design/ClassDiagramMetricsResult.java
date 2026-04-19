package com.metrics.design;

public class ClassDiagramMetricsResult {
    private final double wmc;
    private final double dit;
    private final double noc;
    private final double cbo;
    private final double lcom;

    public ClassDiagramMetricsResult(double wmc, double dit, double noc, double cbo, double lcom) {
        this.wmc = wmc;
        this.dit = dit;
        this.noc = noc;
        this.cbo = cbo;
        this.lcom = lcom;
    }

    public double getWmc() {
        return wmc;
    }

    public double getDit() {
        return dit;
    }

    public double getNoc() {
        return noc;
    }

    public double getCbo() {
        return cbo;
    }

    public double getLcom() {
        return lcom;
    }
}
