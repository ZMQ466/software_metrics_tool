package com.metrics.modules;

import com.metrics.core.MetricCalculator;
import com.metrics.model.ClassInfo;
import com.metrics.model.MethodInfo;
import com.metrics.model.ProjectMetricsResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CKLkMetricsCalculator implements MetricCalculator {
    @Override
    public String getMetricName() {
        return "CK_LK";
    }

    @Override
    public void calculate(ClassInfo classInfo) {
        double wmc = 0.0;
        Set<String> rfcSet = new HashSet<>();
        List<MethodInfo> methods = classInfo.getMethods();
        for (MethodInfo method : methods) {
            int cc = method.getCyclomaticComplexity();
            if (cc <= 0) {
                cc = 1;
            }
            wmc += cc;
            rfcSet.add(method.getMethodName());
            rfcSet.addAll(method.getMethodCalls());
        }

        classInfo.addMetric("CK_WMC", wmc);

        Set<String> uniqueCalledClasses = new HashSet<>(classInfo.getCalledClasses());
        uniqueCalledClasses.remove(classInfo.getClassName());
        classInfo.addMetric("CK_CBO", uniqueCalledClasses.size());

        classInfo.addMetric("CK_RFC", rfcSet.size());

        LcomResult lcom = calculateLcom(methods);
        classInfo.addMetric("CK_LCOM", lcom.lcom);
        classInfo.addMetric("LK_COHESION", lcom.cohesion);
    }

    @Override
    public void calculateProjectLevel(ProjectMetricsResult project) {
        Map<String, String> parent = new HashMap<>();
        Map<String, List<String>> children = new HashMap<>();

        for (ClassInfo c : project.getClasses()) {
            String className = c.getClassName();
            String superName = c.getSuperClassName();
            if (className != null) {
                parent.put(className, superName);
                if (superName != null && !superName.trim().isEmpty()) {
                    children.computeIfAbsent(superName, k -> new ArrayList<>()).add(className);
                }
            }
        }

        for (ClassInfo c : project.getClasses()) {
            String className = c.getClassName();
            int dit = computeDit(className, parent);
            int noc = children.getOrDefault(className, new ArrayList<>()).size();
            c.addMetric("CK_DIT", dit);
            c.addMetric("CK_NOC", noc);
        }
    }

    private int computeDit(String className, Map<String, String> parent) {
        int depth = 0;
        Set<String> visited = new HashSet<>();
        String current = className;
        while (current != null) {
            if (!visited.add(current)) {
                break;
            }
            String p = parent.get(current);
            if (p == null || p.trim().isEmpty() || "Object".equals(p) || "java.lang.Object".equals(p)) {
                break;
            }
            depth++;
            current = p;
        }
        return depth;
    }

    private LcomResult calculateLcom(List<MethodInfo> methods) {
        int n = methods.size();
        if (n < 2) {
            return new LcomResult(0.0, 1.0);
        }

        int p = 0;
        int q = 0;
        for (int i = 0; i < n; i++) {
            Set<String> a = new HashSet<>(methods.get(i).getAccessedFields());
            for (int j = i + 1; j < n; j++) {
                Set<String> b = new HashSet<>(methods.get(j).getAccessedFields());
                boolean share = false;
                for (String f : a) {
                    if (b.contains(f)) {
                        share = true;
                        break;
                    }
                }
                if (share) {
                    q++;
                } else {
                    p++;
                }
            }
        }
        double lcom = Math.max(p - q, 0);
        double totalPairs = (double) (n * (n - 1)) / 2.0;
        double cohesion = totalPairs == 0 ? 1.0 : (q / totalPairs);
        return new LcomResult(lcom, cohesion);
    }

    private static class LcomResult {
        private final double lcom;
        private final double cohesion;

        private LcomResult(double lcom, double cohesion) {
            this.lcom = lcom;
            this.cohesion = cohesion;
        }
    }
}
