package com.metrics.modules;

import com.metrics.core.MetricCalculator;
import com.metrics.model.ClassInfo;
import com.metrics.model.MethodInfo;
import com.metrics.model.ProjectMetricsResult;

import java.util.List;

public class TraditionalMetricsCalculator implements MetricCalculator {
    @Override
    public String getMetricName() {
        return "Traditional";
    }

    @Override
    public void calculate(ClassInfo classInfo) {
        int totalLoc = 0;
        int totalCc = 0;
        int methodCount = 0;

        List<MethodInfo> methods = classInfo.getMethods();
        for (MethodInfo m : methods) {
            int loc = m.getLoc();
            if (loc < 0) {
                loc = 0;
            }
            totalLoc += loc;
            int cc = m.getCyclomaticComplexity();
            if (cc <= 0) {
                cc = 1;
            }
            totalCc += cc;
            methodCount++;
        }

        classInfo.addMetric("LOC_CLASS", totalLoc);
        if (methodCount > 0) {
            classInfo.addMetric("CC_AVG_CLASS", (double) totalCc / methodCount);
        } else {
            classInfo.addMetric("CC_AVG_CLASS", 0.0);
        }
    }

    @Override
    public void calculateProjectLevel(ProjectMetricsResult project) {
        int totalLoc = 0;
        int totalCc = 0;
        int totalMethods = 0;

        for (ClassInfo c : project.getClasses()) {
            for (MethodInfo m : c.getMethods()) {
                totalLoc += Math.max(m.getLoc(), 0);
                int cc = m.getCyclomaticComplexity();
                if (cc <= 0) {
                    cc = 1;
                }
                totalCc += cc;
                totalMethods++;
            }
        }

        project.setTotalLoc(totalLoc);
        if (totalMethods > 0) {
            project.setAvgCyclomaticComplexity((double) totalCc / totalMethods);
        } else {
            project.setAvgCyclomaticComplexity(0.0);
        }
    }
}
