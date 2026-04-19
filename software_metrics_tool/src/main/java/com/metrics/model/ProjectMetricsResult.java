package com.metrics.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一数据结构：度量项目结果总汇
 */
public class ProjectMetricsResult {
    private String projectName;
    private List<ClassInfo> classes;
    private double totalLoc;
    private double avgCyclomaticComplexity;
    
    // 其他项目级度量结果...
    private double totalUCP;

    public ProjectMetricsResult(String projectName) {
        this.projectName = projectName;
        this.classes = new ArrayList<>();
    }

    public void addClassInfo(ClassInfo classInfo) {
        this.classes.add(classInfo);
    }
    
    public List<ClassInfo> getClasses() { return classes; }
    public String getProjectName() { return projectName; }
    
    public double getTotalLoc() { return totalLoc; }
    public void setTotalLoc(double totalLoc) { this.totalLoc = totalLoc; }
    
    public double getAvgCyclomaticComplexity() { return avgCyclomaticComplexity; }
    public void setAvgCyclomaticComplexity(double avgCyclomaticComplexity) { this.avgCyclomaticComplexity = avgCyclomaticComplexity; }
    
    public double getTotalUCP() { return totalUCP; }
    public void setTotalUCP(double totalUCP) { this.totalUCP = totalUCP; }
}
