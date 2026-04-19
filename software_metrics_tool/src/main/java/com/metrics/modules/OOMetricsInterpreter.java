package com.metrics.modules;

import com.metrics.model.ClassInfo;

import java.util.Locale;
import java.util.Map;

/**
 * 面向对象 CK / LK 指标的文字解释
 */
public final class OOMetricsInterpreter {

    private OOMetricsInterpreter() {
    }

    /** 展示全部 CK/LK 解释（兼容旧调用）。 */
    public static String interpretClass(ClassInfo c) {
        return interpretClass(c, true, true);
    }

    /**
     * 按界面勾选只生成对应分组的解释；未勾选的组不出现任何解释段落。
     */
    public static String interpretClass(ClassInfo c, boolean showCk, boolean showLk) {
        Map<String, Double> m = c.getMetrics();
        StringBuilder sb = new StringBuilder();

        if (showCk) {
            appendLine(sb, "CK_WMC", m, "加权方法复杂度(此处为各方法圈复杂度之和)",
                    "值越大表示类承担的决策分支越多，可维护性风险上升。",
                    v -> v <= 20 ? "整体尚可，注意个别长方法。" : (v <= 50 ? "偏高，建议拆分职责或降低分支。" : "很高，类可能承担过多逻辑。"));

            appendLine(sb, "CK_DIT", m, "继承树深度(相对当前解析到的项目类型)",
                    "层次越深，父类行为对子类的影响链越长。",
                    v -> v <= 2 ? "继承层次适中。" : (v <= 4 ? "偏深，需关注多态与重写带来的理解成本。" : "很深，可评估组合代替深层继承。"));

            appendLine(sb, "CK_NOC", m, "直接子类数(仅统计当前源码树内)",
                    "子类多表示抽象被多处复用，也可能表示基类职责过重。",
                    v -> v == 0 ? "无项目内子类。" : (v <= 3 ? "子类数量适中。" : "子类较多，基类变更影响面大。"));

            appendLine(sb, "CK_CBO", m, "类间耦合(不同类型引用数，含继承/实现/签名与实例化等)",
                    "与其它类型的耦合越多，修改连锁反应越大。",
                    v -> v <= 6 ? "耦合度较低。" : (v <= 14 ? "中等耦合，可审视是否可引入接口隔离。" : "耦合偏高，建议降低跨类直接依赖。"));

            appendLine(sb, "CK_RFC", m, "响应集规模(本类方法名 ∪ 方法体内调用的方法标识符)",
                    "无完整类型绑定时，RFC 为近似值；仍反映“潜在消息规模”。",
                    v -> v <= 25 ? "响应集较小。" : (v <= 50 ? "中等规模，注意是否上帝类。" : "响应集很大，类对外协作面过宽。"));

            appendLine(sb, "CK_LCOM", m, "LCOM(Henderson–Sellers)：不共享实例字段的方法对超出共享对的数量",
                    "越大表示方法间通过字段的数据内聚越弱。",
                    v -> v == 0 ? "未出现明显“缺内聚”计数(或方法数不足2)。" : (v <= 10 ? "存在一定内聚缺失。" : "内聚缺失明显，可考虑拆类或重组字段职责。"));
        }

        if (showLk) {
            appendLine(sb, "LK_LCOM_NORM", m, "归一化 LCOM：LCOM / 方法对数，范围约[0,1]",
                    "便于跨类比较内聚缺失的相对强度。",
                    v -> v <= 0.25 ? "相对内聚尚可。" : (v <= 0.5 ? "内聚偏弱。" : "内聚很弱，方法可能处理无关数据。"));

            appendLine(sb, "LK_COHESION", m, "方法对共享实例字段的比例(= 共享对 / 全部方法对)",
                    "越接近 1 表示方法越围绕同一批字段协作。",
                    v -> v >= 0.66 ? "字段协作较集中。" : (v >= 0.33 ? "内聚一般。" : "方法在字段层面较分散。"));
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
