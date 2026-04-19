package com.metrics.model;

/**
 * 统一数据结构：用例点度量输入表
 * 用于设计度量（4号同学）从GUI收集数据
 */
public class UCPInput {
    // 未调整用例权重 (UUCW) 相关的用例数量
    private int simpleUseCases;
    private int averageUseCases;
    private int complexUseCases;
    
    // 未调整参与者权重 (UAW) 相关的Actor数量
    private int simpleActors;
    private int averageActors;
    private int complexActors;
    
    // 技术复杂度因子 (TCF) 的13个评估项，0-5分
    private int[] technicalFactors;
    
    // 环境复杂度因子 (ECF) 的8个评估项，0-5分
    private int[] environmentalFactors;
    
    public UCPInput() {
        this.technicalFactors = new int[13];
        this.environmentalFactors = new int[8];
    }

    // Getters and Setters for UAW
    public int getSimpleActors() { return simpleActors; }
    public void setSimpleActors(int simpleActors) { this.simpleActors = simpleActors; }
    public int getAverageActors() { return averageActors; }
    public void setAverageActors(int averageActors) { this.averageActors = averageActors; }
    public int getComplexActors() { return complexActors; }
    public void setComplexActors(int complexActors) { this.complexActors = complexActors; }

    // Getters and Setters for UUCW
    public int getSimpleUseCases() { return simpleUseCases; }
    public void setSimpleUseCases(int simpleUseCases) { this.simpleUseCases = simpleUseCases; }
    public int getAverageUseCases() { return averageUseCases; }
    public void setAverageUseCases(int averageUseCases) { this.averageUseCases = averageUseCases; }
    public int getComplexUseCases() { return complexUseCases; }
    public void setComplexUseCases(int complexUseCases) { this.complexUseCases = complexUseCases; }

    // Getters and Setters for Factors
    public int[] getTechnicalFactors() { return technicalFactors; }
    public void setTechnicalFactors(int[] technicalFactors) { this.technicalFactors = technicalFactors; }
    
    public int[] getEnvironmentalFactors() { return environmentalFactors; }
    public void setEnvironmentalFactors(int[] environmentalFactors) { this.environmentalFactors = environmentalFactors; }
}
