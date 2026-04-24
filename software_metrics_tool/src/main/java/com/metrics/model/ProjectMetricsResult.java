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
    private double totalEffectiveLoc;
    private double totalCommentLines;
    private double totalBlankLines;
    private double commentRate;
    private double avgCyclomaticComplexity;
    private double avgMethodLoc;
    private int maxMethodNestingDepth;
    private int highComplexityMethodCount;
    private int maxCyclomaticComplexity;
    
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

    public double getTotalEffectiveLoc() { return totalEffectiveLoc; }
    public void setTotalEffectiveLoc(double totalEffectiveLoc) { this.totalEffectiveLoc = totalEffectiveLoc; }

    public double getTotalCommentLines() { return totalCommentLines; }
    public void setTotalCommentLines(double totalCommentLines) { this.totalCommentLines = totalCommentLines; }

    public double getTotalBlankLines() { return totalBlankLines; }
    public void setTotalBlankLines(double totalBlankLines) { this.totalBlankLines = totalBlankLines; }

    public double getCommentRate() { return commentRate; }
    public void setCommentRate(double commentRate) { this.commentRate = commentRate; }
    
    public double getAvgCyclomaticComplexity() { return avgCyclomaticComplexity; }
    public void setAvgCyclomaticComplexity(double avgCyclomaticComplexity) { this.avgCyclomaticComplexity = avgCyclomaticComplexity; }

    public double getAvgMethodLoc() { return avgMethodLoc; }
    public void setAvgMethodLoc(double avgMethodLoc) { this.avgMethodLoc = avgMethodLoc; }

    public int getMaxMethodNestingDepth() { return maxMethodNestingDepth; }
    public void setMaxMethodNestingDepth(int maxMethodNestingDepth) { this.maxMethodNestingDepth = maxMethodNestingDepth; }

    public int getHighComplexityMethodCount() { return highComplexityMethodCount; }
    public void setHighComplexityMethodCount(int highComplexityMethodCount) { this.highComplexityMethodCount = highComplexityMethodCount; }

    public int getMaxCyclomaticComplexity() { return maxCyclomaticComplexity; }
    public void setMaxCyclomaticComplexity(int maxCyclomaticComplexity) { this.maxCyclomaticComplexity = maxCyclomaticComplexity; }
    
    public double getTotalUCP() { return totalUCP; }
    public void setTotalUCP(double totalUCP) { this.totalUCP = totalUCP; }
}
