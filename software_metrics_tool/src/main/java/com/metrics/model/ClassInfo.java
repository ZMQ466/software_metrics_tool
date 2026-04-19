package com.metrics.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 统一数据结构：类信息存储
 * 用于面向对象度量（2号同学）和传统度量（3号同学）的分析基础
 */
public class ClassInfo {
    /** 简单类名（含匿名/内部类时常用 Outer$Inner 形式） */
    private String className;
    /** 包名限定全名，用于项目级继承与耦合统计，避免同名类冲突 */
    private String qualifiedName;
    private String superClassName;
    private List<String> interfaces;
    
    // 类的成员属性
    private List<String> fields;
    
    // 类的方法
    private List<MethodInfo> methods;
    
    // 调用的其他类 (用于计算耦合度 CBO)
    private List<String> calledClasses;
    
    // 存储该类的各项度量结果 (指标名 -> 指标值)
    private Map<String, Double> metrics;

    public ClassInfo(String className) {
        this.className = className;
        this.qualifiedName = null;
        this.interfaces = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.calledClasses = new ArrayList<>();
        this.metrics = new HashMap<>();
    }

    // Getters and Setters
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    /**
     * 返回 {@link #qualifiedName}；若未设置则退回简单 {@link #className}。
     */
    public String getQualifiedName() {
        return qualifiedName != null && !qualifiedName.trim().isEmpty() ? qualifiedName : className;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }
    
    public String getSuperClassName() { return superClassName; }
    public void setSuperClassName(String superClassName) { this.superClassName = superClassName; }
    
    public List<String> getInterfaces() { return interfaces; }
    public List<String> getFields() { return fields; }
    public List<MethodInfo> getMethods() { return methods; }
    public List<String> getCalledClasses() { return calledClasses; }
    
    public void addMetric(String name, double value) {
        this.metrics.put(name, value);
    }
    
    public Map<String, Double> getMetrics() {
        return metrics;
    }
}
