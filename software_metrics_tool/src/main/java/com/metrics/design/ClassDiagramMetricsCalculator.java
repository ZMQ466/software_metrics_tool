package com.metrics.design;

public class ClassDiagramMetricsCalculator {

    public ClassDiagramMetricsResult calculate(
            int classCount,
            double avgMethodCount,
            double avgAttributeCount,
            int inheritanceDepth,
            int subclassCount,
            int relationCount) {
        int safeClassCount = Math.max(0, classCount);
        double safeAvgMethodCount = Math.max(0.0, avgMethodCount);
        double safeAvgAttributeCount = Math.max(0.0, avgAttributeCount);
        int safeInheritanceDepth = Math.max(0, inheritanceDepth);
        int safeSubclassCount = Math.max(0, subclassCount);
        int safeRelationCount = Math.max(0, relationCount);

        // WMC: use average method count and class count as estimated weighted method total.
        double wmc = safeClassCount * safeAvgMethodCount;
        // DIT: directly use provided inheritance depth.
        double dit = safeInheritanceDepth;
        // NOC: directly use provided subclass count.
        double noc = safeSubclassCount;
        // CBO: average class coupling estimated by relation count per class.
        double cbo = safeClassCount == 0 ? 0.0 : (double) safeRelationCount / safeClassCount;
        // LCOM: cohesion gap estimated by average methods vs average attributes.
        double lcom = Math.max(0.0, safeAvgMethodCount - safeAvgAttributeCount);

        return new ClassDiagramMetricsResult(wmc, dit, noc, cbo, lcom);
    }
}
