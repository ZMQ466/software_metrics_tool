package com.metrics.modules;

import com.metrics.model.ClassInfo;

import java.util.Locale;
import java.util.Map;

/**
 * 面向对象 CK / LK 指标的文本解释。
 */
public final class OOMetricsInterpreter {

    private OOMetricsInterpreter() {
    }

    public static String interpretClass(ClassInfo c) {
        return interpretClass(c, true, true);
    }

    public static String interpretClass(ClassInfo c, boolean showCk, boolean showLk) {
        Map<String, Double> m = c.getMetrics();
        StringBuilder sb = new StringBuilder();

        if (showCk) {
            appendLine(sb, "CK_WMC", m, "加权方法复杂度（各方法圈复杂度之和）",
                    "值越大表示该类承担的决策分支越多。",
                    v -> v <= 20 ? "整体尚可。" : (v <= 50 ? "偏高，建议拆分职责。" : "很高，建议优先重构。"));
            appendLine(sb, "CK_DIT", m, "继承树深度", "层次越深，理解成本通常越高。",
                    v -> v <= 2 ? "继承层次适中。" : (v <= 4 ? "继承偏深。" : "继承很深，建议评估组合替代。"));
            appendLine(sb, "CK_NOC", m, "直接子类数", "子类多会扩大基类改动影响面。",
                    v -> v == 0 ? "无项目内子类。" : (v <= 3 ? "子类数量适中。" : "子类较多，需稳控变更。"));
            appendLine(sb, "CK_CBO", m, "类间耦合数", "耦合高会提高联动修改风险。",
                    v -> v <= 6 ? "耦合较低。" : (v <= 14 ? "耦合中等。" : "耦合偏高，建议解耦。"));
            appendLine(sb, "CK_RFC", m, "响应集规模", "规模越大通常表示协作面越宽。",
                    v -> v <= 25 ? "规模较小。" : (v <= 50 ? "规模中等。" : "规模较大，建议收敛接口。"));
            appendLine(sb, "CK_LCOM", m, "LCOM 内聚缺失度", "值越大说明方法间共享状态越少。",
                    v -> v == 0 ? "未见明显内聚缺失。" : (v <= 10 ? "存在一定内聚问题。" : "内聚问题明显。"));
        }

        if (showLk) {
            appendLine(sb, "LK_NOA", m, "新增方法数量（非重写方法）",
                    "衡量该类在继承基础上新增的行为数。",
                    v -> v <= 3 ? "新增行为较少。" : (v <= 8 ? "新增行为适中。" : "新增行为较多，关注职责边界。"));
            appendLine(sb, "LK_NOO", m, "方法重写数（override 数）",
                    "衡量该类通过继承进行多态定制的程度。",
                    v -> v == 0 ? "未发生重写。" : (v <= 3 ? "重写数量适中。" : "重写较多，建议回归继承链兼容性。"));
            appendLine(sb, "LK_CS", m, "类规模（字段数 + 方法数）",
                    "规模越大，理解与维护成本通常越高。",
                    v -> v <= 15 ? "类规模较小。" : (v <= 30 ? "类规模中等。" : "类规模偏大，建议评估拆分类。"));
        }

        if (sb.length() == 0) {
            return "";
        }
        return "  【指标解释】\n" + sb;
    }

    private static void appendLine(StringBuilder sb, String key, Map<String, Double> metrics,
                                   String meaning, String note, java.util.function.DoubleFunction<String> level) {
        if (!metrics.containsKey(key)) {
            return;
        }
        double v = metrics.get(key);
        sb.append("  - ").append(key).append("=").append(String.format(Locale.ROOT, "%.4f", v)).append("\n");
        sb.append("    含义: ").append(meaning).append("\n");
        sb.append("    说明: ").append(note).append("\n");
        sb.append("    解读: ").append(level.apply(v)).append("\n");
    }
}
