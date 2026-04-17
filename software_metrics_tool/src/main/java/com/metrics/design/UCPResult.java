package com.metrics.design;

public class UCPResult {
    private final double uaw;
    private final double uucw;
    private final double uucp;
    private final double tcf;
    private final double ecf;
    private final double ucp;

    public UCPResult(double uaw, double uucw, double uucp, double tcf, double ecf, double ucp) {
        this.uaw = uaw;
        this.uucw = uucw;
        this.uucp = uucp;
        this.tcf = tcf;
        this.ecf = ecf;
        this.ucp = ucp;
    }

    public double getUaw() {
        return uaw;
    }

    public double getUucw() {
        return uucw;
    }

    public double getUucp() {
        return uucp;
    }

    public double getTcf() {
        return tcf;
    }

    public double getEcf() {
        return ecf;
    }

    public double getUcp() {
        return ucp;
    }
}
