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

/**
 * CK 度量集（WMC、DIT、NOC、CBO、RFC、LCOM）及 LK 相关内聚指标。
 * <p>
 * WMC 采用各方法圈复杂度之和（课程常见“方法复杂度之和”口径）；LCOM 采用 Henderson–Sellers 形式：
 * max(|P|−|Q|, 0)，其中 P、Q 为方法对是否共享实例字段的划分。
 */
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
        classInfo.addMetric("CK_RFC", rfcSet.size());

        LcomResult lcom = calculateLcom(methods);
        classInfo.addMetric("CK_LCOM", lcom.lcom);
        classInfo.addMetric("LK_LCOM_NORM", lcom.lcomNorm);
        classInfo.addMetric("LK_COHESION", lcom.cohesion);
    }

    @Override
    public void calculateProjectLevel(ProjectMetricsResult project) {
        List<ClassInfo> all = project.getClasses();
        for (ClassInfo c : all) {
            applyCbo(c, all);
        }

        Map<String, String> superRawByQualified = new HashMap<>();
        for (ClassInfo c : all) {
            superRawByQualified.put(c.getQualifiedName(), normalizeTypeRef(c.getSuperClassName()));
        }

        Map<String, List<String>> children = new HashMap<>();
        for (ClassInfo c : all) {
            String childQ = c.getQualifiedName();
            String sup = normalizeTypeRef(c.getSuperClassName());
            String resolvedSuper = resolveToProjectFqn(sup, all);
            if (resolvedSuper != null) {
                children.computeIfAbsent(resolvedSuper, k -> new ArrayList<>()).add(childQ);
            }
        }

        for (ClassInfo c : all) {
            String q = c.getQualifiedName();
            int dit = computeDit(q, superRawByQualified, all);
            int noc = children.getOrDefault(q, new ArrayList<>()).size();
            c.addMetric("CK_DIT", dit);
            c.addMetric("CK_NOC", noc);
        }
    }

    /**
     * CK 的 CBO：与该类存在类型级耦合（继承、实现、成员/参数/返回值/局部/实例化等）的不同类型数量，
     * 在整项目范围内尽量解析为限定名并排除自身。
     */
    private static void applyCbo(ClassInfo self, List<ClassInfo> all) {
        String selfQ = self.getQualifiedName();
        Set<String> distinct = new HashSet<>();
        for (String raw : self.getCalledClasses()) {
            String t = normalizeTypeRef(raw);
            if (t.isEmpty()) {
                continue;
            }
            String resolved = resolveToProjectFqn(t, all);
            String key = resolved != null ? resolved : t;
            if (key.equals(selfQ)) {
                continue;
            }
            distinct.add(key);
        }
        self.addMetric("CK_CBO", distinct.size());
    }

    private static String normalizeTypeRef(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        int angle = t.indexOf('<');
        if (angle > 0) {
            t = t.substring(0, angle).trim();
        }
        return t;
    }

    /**
     * 将源码中的类型引用解析为项目中某类的限定名；无法唯一确定时返回 null（视为外部类型）。
     */
    static String resolveToProjectFqn(String typeReference, List<ClassInfo> classes) {
        if (typeReference == null) {
            return null;
        }
        String t = typeReference.trim();
        if (t.isEmpty() || "Object".equals(t) || "java.lang.Object".equals(t)) {
            return null;
        }
        for (ClassInfo c : classes) {
            if (t.equals(c.getQualifiedName())) {
                return c.getQualifiedName();
            }
        }
        String simple = t.contains(".") ? t.substring(t.lastIndexOf('.') + 1) : t;
        String matched = null;
        for (ClassInfo c : classes) {
            if (simple.equals(c.getClassName()) || c.getQualifiedName().endsWith("." + simple)
                    || c.getQualifiedName().endsWith("$" + simple)) {
                if (matched != null && !matched.equals(c.getQualifiedName())) {
                    return null;
                }
                matched = c.getQualifiedName();
            }
        }
        return matched;
    }

    private static int computeDit(String qualifiedName,
                                  Map<String, String> superRawByQualified,
                                  List<ClassInfo> all) {
        int depth = 0;
        Set<String> visited = new HashSet<>();
        String current = qualifiedName;
        while (current != null && visited.add(current)) {
            String supRaw = superRawByQualified.get(current);
            if (supRaw == null || supRaw.isEmpty() || "Object".equals(supRaw) || "java.lang.Object".equals(supRaw)) {
                break;
            }
            String next = resolveToProjectFqn(supRaw, all);
            if (next == null) {
                depth++;
                break;
            }
            depth++;
            current = next;
        }
        return depth;
    }

    private static LcomResult calculateLcom(List<MethodInfo> methods) {
        int n = methods.size();
        if (n < 2) {
            return new LcomResult(0.0, 0.0, 1.0);
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
        double lcomNorm = totalPairs == 0 ? 0.0 : (lcom / totalPairs);
        return new LcomResult(lcom, lcomNorm, cohesion);
    }

    private static class LcomResult {
        private final double lcom;
        private final double lcomNorm;
        private final double cohesion;

        private LcomResult(double lcom, double lcomNorm, double cohesion) {
            this.lcom = lcom;
            this.lcomNorm = lcomNorm;
            this.cohesion = cohesion;
        }
    }
}
