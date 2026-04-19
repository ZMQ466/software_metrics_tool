package com.metrics.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一数据结构：方法信息存储
 * 主要用于圈复杂度、方法行数等代码级传统度量
 */
public class MethodInfo {
    private String methodName;
    private String returnType;
    private List<String> parameters;
    
    // 基础度量数据：供3号同学填入
    private int startLine;
    private int endLine;
    private int loc; // 代码行数
    private int cyclomaticComplexity; // 圈复杂度
    
    // 方法内部调用的其他方法 (用于计算RFC, LCOM等)
    private List<String> methodCalls;
    
    // 方法内部访问的类属性 (用于计算LCOM内聚)
    private List<String> accessedFields;

    public MethodInfo(String methodName) {
        this.methodName = methodName;
        this.parameters = new ArrayList<>();
        this.methodCalls = new ArrayList<>();
        this.accessedFields = new ArrayList<>();
    }

    // Getters and Setters
    public String getMethodName() { return methodName; }
    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
    
    public int getStartLine() { return startLine; }
    public void setStartLine(int startLine) { this.startLine = startLine; }
    
    public int getEndLine() { return endLine; }
    public void setEndLine(int endLine) { this.endLine = endLine; }
    
    public int getLoc() { return loc; }
    public void setLoc(int loc) { this.loc = loc; }
    
    public int getCyclomaticComplexity() { return cyclomaticComplexity; }
    public void setCyclomaticComplexity(int cyclomaticComplexity) { this.cyclomaticComplexity = cyclomaticComplexity; }

    public List<String> getParameters() { return parameters; }
    public List<String> getMethodCalls() { return methodCalls; }
    public List<String> getAccessedFields() { return accessedFields; }
}
