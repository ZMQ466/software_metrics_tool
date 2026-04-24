package com.metrics.core;

import com.metrics.model.ClassInfo;
import com.metrics.model.ProjectMetricsResult;
import com.metrics.parser.CodeParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 度量总控管理器：1号同学在此集成其他同学的模块
 */
public class MetricsManager {

    private CodeParser parser;
    private List<MetricCalculator> calculators;
    
    public MetricsManager() {
        this.calculators = new ArrayList<>();
    }
    
    // 注入解析器 (例如 EclipseASTParser)
    public void setParser(CodeParser parser) {
        this.parser = parser;
    }
    
    // 注册其他同学开发的度量指标 (CK, LK, LoC, 圈复杂度等)
    public void registerCalculator(MetricCalculator calculator) {
        this.calculators.add(calculator);
    }
    
    /**
     * 执行主流程：1. 解析输入 -> 2. 计算度量 -> 3. 收集结果
     */
    public ProjectMetricsResult runAnalysis(String sourcePath) {
        if (parser == null) {
            throw new IllegalStateException("未设置解析器 (CodeParser)");
        }
        
        System.out.println("1. 开始解析源代码: " + sourcePath);
        List<ClassInfo> classInfos = parser.parseDirectory(sourcePath);
        
        ProjectMetricsResult result = new ProjectMetricsResult(sourcePath);
        
        System.out.println("2. 开始执行各项度量计算...");
        for (ClassInfo clazz : classInfos) {
            result.addClassInfo(clazz);
            // 依次调用各个度量模块
            for (MetricCalculator calc : calculators) {
                calc.calculate(clazz);
            }
        }
        
        // 执行项目级度量（例如总体规模统计，UCP功能点计算集成等）
        for (MetricCalculator calc : calculators) {
            calc.calculateProjectLevel(result);
        }
        
        System.out.println("3. 分析完成");
        return result;
    }
}
