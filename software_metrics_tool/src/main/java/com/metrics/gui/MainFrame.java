package com.metrics.gui;

import com.metrics.design.RequirementDesignMetricsEngine;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * 自动化度量工具主界面。
 */
public class MainFrame extends JFrame {

    private JTabbedPane tabbedPane;

    // 代码度量面板组件
    private JPanel codeMetricsPanel;
    private JTextField sourcePathField;
    private JButton browseButton;
    private JButton analyzeCodeButton;
    private JTextArea codeResultArea;

    // 需求/设计度量面板组件
    private JPanel designMetricsPanel;
    private JTextField fpEiField;
    private JTextField fpEoField;
    private JTextField fpEqField;
    private JTextField fpIlfField;
    private JTextField fpEifField;
    private JTextField fpGscField;
    private JTextField ucSimpleField;
    private JTextField ucAverageField;
    private JTextField ucComplexField;
    private JTextField actorSimpleField;
    private JTextField actorAverageField;
    private JTextField actorComplexField;
    private JTextField tcfField;
    private JTextField ecfField;
    private JTextField featureWeightField;
    private JButton calculateUcpButton;
    private JTextArea designResultArea;

    public MainFrame() {
        setTitle("软件度量自动化工具 - 团队集成版");
        setSize(920, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();

        initCodeMetricsPanel();
        tabbedPane.addTab("面向对象与代码度量 (代码级)", codeMetricsPanel);

        initDesignMetricsPanel();
        tabbedPane.addTab("功能点 / 用例点 / 特征点 (设计级)", designMetricsPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void initCodeMetricsPanel() {
        codeMetricsPanel = new JPanel(new BorderLayout(10, 10));
        codeMetricsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("源码目录:"));

        sourcePathField = new JTextField(30);
        topPanel.add(sourcePathField);

        browseButton = new JButton("浏览...");
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                sourcePathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        topPanel.add(browseButton);

        analyzeCodeButton = new JButton("开始分析");
        analyzeCodeButton.addActionListener(e -> performCodeAnalysis());
        topPanel.add(analyzeCodeButton);

        codeMetricsPanel.add(topPanel, BorderLayout.NORTH);

        codeResultArea = new JTextArea();
        codeResultArea.setEditable(false);
        codeMetricsPanel.add(new JScrollPane(codeResultArea), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(new JCheckBox("CK度量集", true));
        bottomPanel.add(new JCheckBox("LK度量集", true));
        bottomPanel.add(new JCheckBox("代码行/复杂度", true));
        codeMetricsPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void initDesignMetricsPanel() {
        designMetricsPanel = new JPanel(new BorderLayout(10, 10));
        designMetricsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        formPanel.setBorder(BorderFactory.createTitledBorder("设计度量输入参数"));

        fpEiField = createField(formPanel, "EI 外部输入", "0");
        fpEoField = createField(formPanel, "EO 外部输出", "0");
        fpEqField = createField(formPanel, "EQ 外部查询", "0");
        fpIlfField = createField(formPanel, "ILF 内部逻辑文件", "0");
        fpEifField = createField(formPanel, "EIF 外部接口文件", "0");
        fpGscField = createField(formPanel, "GSC 总分 (0~70)", "0");

        ucSimpleField = createField(formPanel, "简单用例数", "0");
        ucAverageField = createField(formPanel, "一般用例数", "0");
        ucComplexField = createField(formPanel, "复杂用例数", "0");

        actorSimpleField = createField(formPanel, "简单参与者数", "0");
        actorAverageField = createField(formPanel, "一般参与者数", "0");
        actorComplexField = createField(formPanel, "复杂参与者数", "0");

        tcfField = createField(formPanel, "TCF", "1.00");
        ecfField = createField(formPanel, "ECF", "1.00");
        featureWeightField = createField(formPanel, "特征点权重", "1.00");

        calculateUcpButton = new JButton("计算功能点 / 用例点 / 特征点");
        calculateUcpButton.addActionListener(e -> performDesignMetricsCalculation());
        JPanel buttonHolder = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonHolder.add(calculateUcpButton);
        buttonHolder.add(new JLabel("可直接用于实验对比与报告截图", SwingConstants.LEFT));

        JPanel northPanel = new JPanel(new BorderLayout(8, 8));
        northPanel.add(formPanel, BorderLayout.CENTER);
        northPanel.add(buttonHolder, BorderLayout.SOUTH);
        designMetricsPanel.add(northPanel, BorderLayout.NORTH);

        designResultArea = new JTextArea();
        designResultArea.setEditable(false);
        designMetricsPanel.add(new JScrollPane(designResultArea), BorderLayout.CENTER);
    }

    private JTextField createField(JPanel panel, String label, String defaultValue) {
        panel.add(new JLabel(label + ":"));
        JTextField field = new JTextField(defaultValue);
        panel.add(field);
        return field;
    }

    private void performCodeAnalysis() {
        String path = sourcePathField.getText();
        if (path == null || path.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择源码目录");
            return;
        }

        codeResultArea.setText("");
        codeResultArea.append("正在调用解析器解析源码: " + path + "\n");
        codeResultArea.append("当前版本已完成设计度量模块接入，代码度量模块可继续接入 CK / LoC / 圈复杂度实现。\n");
        codeResultArea.append("后续可将 MetricsManager 与解析器接入此处。\n\n");
    }

    private void performDesignMetricsCalculation() {
        try {
            RequirementDesignMetricsEngine.FunctionPointInput fpInput = new RequirementDesignMetricsEngine.FunctionPointInput(
                    parseNonNegativeInt(fpEiField.getText(), "EI"),
                    parseNonNegativeInt(fpEoField.getText(), "EO"),
                    parseNonNegativeInt(fpEqField.getText(), "EQ"),
                    parseNonNegativeInt(fpIlfField.getText(), "ILF"),
                    parseNonNegativeInt(fpEifField.getText(), "EIF"),
                    parseIntInRange(fpGscField.getText(), "GSC", 0, 70));

            RequirementDesignMetricsEngine.UseCasePointInput ucpInput = new RequirementDesignMetricsEngine.UseCasePointInput(
                    parseNonNegativeInt(ucSimpleField.getText(), "简单用例数"),
                    parseNonNegativeInt(ucAverageField.getText(), "一般用例数"),
                    parseNonNegativeInt(ucComplexField.getText(), "复杂用例数"),
                    parseNonNegativeInt(actorSimpleField.getText(), "简单参与者数"),
                    parseNonNegativeInt(actorAverageField.getText(), "一般参与者数"),
                    parseNonNegativeInt(actorComplexField.getText(), "复杂参与者数"),
                    parsePositiveDouble(tcfField.getText(), "TCF"),
                    parsePositiveDouble(ecfField.getText(), "ECF"));

            RequirementDesignMetricsEngine.FeaturePointInput featureInput = new RequirementDesignMetricsEngine.FeaturePointInput(
                    parsePositiveDouble(featureWeightField.getText(), "特征点权重"));

            RequirementDesignMetricsEngine.FunctionPointResult fpResult = RequirementDesignMetricsEngine
                    .calculateFunctionPoint(fpInput);
            RequirementDesignMetricsEngine.UseCasePointResult ucpResult = RequirementDesignMetricsEngine
                    .calculateUseCasePoint(ucpInput);
            RequirementDesignMetricsEngine.FeaturePointResult featureResult = RequirementDesignMetricsEngine
                    .calculateFeaturePoint(fpResult, featureInput);

            designResultArea.setText("");
            designResultArea.append("=== 功能点计算 ===\n");
            designResultArea.append(String.format("UFP: %d%n", fpResult.ufp));
            designResultArea.append(String.format("VAF: %.2f%n", fpResult.vaf));
            designResultArea.append(String.format("Adjusted FP: %.2f%n%n", fpResult.adjustedFp));

            designResultArea.append("=== 用例点计算 ===\n");
            designResultArea.append(String.format("UUCW: %d%n", ucpResult.uucw));
            designResultArea.append(String.format("UAW: %d%n", ucpResult.uaw));
            designResultArea.append(String.format("UCP: %.2f%n%n", ucpResult.ucp));

            designResultArea.append("=== 特征点扩展 ===\n");
            designResultArea.append(String.format("Base Adjusted FP: %.2f%n", featureResult.baseAdjustedFp));
            designResultArea.append(String.format("Weight: %.2f%n", featureResult.algorithmicWeight));
            designResultArea.append(String.format("Feature Point: %.2f%n%n", featureResult.featurePoint));

            designResultArea.append("说明：以上结果可直接用于实验对比、表格截图和报告撰写。\n");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "输入错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int parseNonNegativeInt(String raw, String label) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value >= 0) {
                return value;
            }
        } catch (Exception ignored) {
        }
        throw new IllegalArgumentException(label + " 必须是非负整数。");
    }

    private int parseIntInRange(String raw, String label, int min, int max) {
        int value = parseNonNegativeInt(raw, label);
        if (value < min || value > max) {
            throw new IllegalArgumentException(label + " 必须在 " + min + " 到 " + max + " 之间。");
        }
        return value;
    }

    private double parsePositiveDouble(String raw, String label) {
        try {
            double value = Double.parseDouble(raw.trim());
            if (value > 0) {
                return value;
            }
        } catch (Exception ignored) {
        }
        throw new IllegalArgumentException(label + " 必须是大于 0 的数字。");
    }
}
