package com.metrics.core;

import com.metrics.model.ClassInfo;
import com.metrics.model.ProjectMetricsResult;

/**
 * 核心模块：度量计算接口
 * 其他同学（2号、3号、4号）通过实现此接口来接入1号同学的主流程中。
 */
public interface MetricCalculator {
    
    /**
     * 获取度量名称，如 "CK_WMC", "LOC", "Cyclomatic_Complexity"
     */
    String getMetricName();

    /**
     * 执行度量计算，并将结果直接更新到 ClassInfo 内部
     * @param classInfo 待计算度量的类对象
     */
    void calculate(ClassInfo classInfo);
    
    /**
     * 执行整个项目的级联度量（可选实现）
     * @param project 项目全量信息
     */
    default void calculateProjectLevel(ProjectMetricsResult project) {
        // 默认不实现，留给有项目级需求的度量指标
    }
}
