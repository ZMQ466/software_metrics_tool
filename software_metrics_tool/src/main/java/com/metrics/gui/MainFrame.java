package com.metrics.gui;

import com.metrics.core.MetricsManager;
import com.metrics.design.UCPCalculator;
import com.metrics.design.UCPResult;
import com.metrics.model.ClassInfo;
import com.metrics.model.ProjectMetricsResult;
import com.metrics.model.UCPInput;
import com.metrics.modules.CKLkMetricsCalculator;
import com.metrics.modules.TraditionalMetricsCalculator;
import com.metrics.parser.EclipseJdtCodeParser;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 自动化度量工具 主界面 (1号同学负责整体GUI框架)
 */
public class MainFrame extends JFrame {

    private JTabbedPane tabbedPane;
    
    // 代码度量面板组件 (对应 2号 和 3号 同学的内容)
    private JPanel codeMetricsPanel;
    private JTextField sourcePathField;
    private JButton browseButton;
    private JButton analyzeCodeButton;
    private JTextArea codeResultArea;
    private JCheckBox ckMetricsCheckBox;
    private JCheckBox lkMetricsCheckBox;
    private JCheckBox traditionalMetricsCheckBox;
    
    // 设计/需求度量面板组件 (对应 4号 同学的内容)
    private JPanel designMetricsPanel;
    private JButton calculateUcpButton;
    private JTextArea designResultArea;
    private JTextField simpleUseCasesField;
    private JTextField averageUseCasesField;
    private JTextField complexUseCasesField;
    private JTextField simpleActorsField;
    private JTextField averageActorsField;
    private JTextField complexActorsField;
    private JTextField technicalFactorsField;
    private JTextField environmentalFactorsField;

    public MainFrame() {
        setTitle("软件度量自动化工具 - 团队集成版");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initComponents();
    }
    
    private void initComponents() {
        tabbedPane = new JTabbedPane();
        
        // 1. 代码度量 Tab
        initCodeMetricsPanel();
        tabbedPane.addTab("面向对象与代码度量 (代码级)", codeMetricsPanel);
        
        // 2. 需求与设计度量 Tab
        initDesignMetricsPanel();
        tabbedPane.addTab("用例点与功能点度量 (设计级)", designMetricsPanel);
        
        this.add(tabbedPane, BorderLayout.CENTER);
    }
    
    private void initCodeMetricsPanel() {
        codeMetricsPanel = new JPanel(new BorderLayout(10, 10));
        codeMetricsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 顶部控制区
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
        
        // 中间结果展示区
        codeResultArea = new JTextArea();
        codeResultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(codeResultArea);
        codeMetricsPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 底部可选度量区
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ckMetricsCheckBox = new JCheckBox("CK度量集", true);
        lkMetricsCheckBox = new JCheckBox("LK度量集", true);
        traditionalMetricsCheckBox = new JCheckBox("代码行/复杂度", true);
        bottomPanel.add(ckMetricsCheckBox);
        bottomPanel.add(lkMetricsCheckBox);
        bottomPanel.add(traditionalMetricsCheckBox);
        codeMetricsPanel.add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void initDesignMetricsPanel() {
        designMetricsPanel = new JPanel(new BorderLayout(10, 10));
        designMetricsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel inputPanel = new JPanel(new GridLayout(9, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("用例点度量参数输入"));
        
        inputPanel.add(new JLabel("简单用例数 (Simple Use Cases):"));
        simpleUseCasesField = new JTextField("0");
        inputPanel.add(simpleUseCasesField);
        inputPanel.add(new JLabel("平均用例数 (Average Use Cases):"));
        averageUseCasesField = new JTextField("0");
        inputPanel.add(averageUseCasesField);
        inputPanel.add(new JLabel("复杂用例数 (Complex Use Cases):"));
        complexUseCasesField = new JTextField("0");
        inputPanel.add(complexUseCasesField);

        inputPanel.add(new JLabel("简单参与者数 (Simple Actors):"));
        simpleActorsField = new JTextField("0");
        inputPanel.add(simpleActorsField);
        inputPanel.add(new JLabel("平均参与者数 (Average Actors):"));
        averageActorsField = new JTextField("0");
        inputPanel.add(averageActorsField);
        inputPanel.add(new JLabel("复杂参与者数 (Complex Actors):"));
        complexActorsField = new JTextField("0");
        inputPanel.add(complexActorsField);

        inputPanel.add(new JLabel("技术因子(13项,逗号分隔0-5):"));
        technicalFactorsField = new JTextField("0,0,0,0,0,0,0,0,0,0,0,0,0");
        inputPanel.add(technicalFactorsField);
        inputPanel.add(new JLabel("环境因子(8项,逗号分隔0-5):"));
        environmentalFactorsField = new JTextField("0,0,0,0,0,0,0,0");
        inputPanel.add(environmentalFactorsField);
        
        calculateUcpButton = new JButton("计算用例点 (UCP)");
        calculateUcpButton.addActionListener(e -> performUCPCalculation());
        inputPanel.add(new JLabel(""));
        inputPanel.add(calculateUcpButton);
        
        designMetricsPanel.add(inputPanel, BorderLayout.NORTH);
        
        // 结果展示区
        designResultArea = new JTextArea();
        designResultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(designResultArea);
        designMetricsPanel.add(scrollPane, BorderLayout.CENTER);
    }
    
    private void performCodeAnalysis() {
        String path = sourcePathField.getText();
        if (path == null || path.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择源码目录");
            return;
        }

        codeResultArea.setText("");
        codeResultArea.append("正在解析源码: " + path + "\n");

        MetricsManager manager = new MetricsManager();
        manager.setParser(new EclipseJdtCodeParser());

        if (ckMetricsCheckBox.isSelected() || lkMetricsCheckBox.isSelected()) {
            manager.registerCalculator(new CKLkMetricsCalculator());
        }
        if (traditionalMetricsCheckBox.isSelected()) {
            manager.registerCalculator(new TraditionalMetricsCalculator());
        }

        try {
            ProjectMetricsResult result = manager.runAnalysis(path);
            renderProjectResult(result);
        } catch (Exception ex) {
            codeResultArea.append("分析失败: " + ex.getMessage() + "\n");
        }
    }
    
    private void performUCPCalculation() {
        designResultArea.setText("");

        UCPInput input = new UCPInput();
        input.setSimpleUseCases(parseInt(simpleUseCasesField.getText()));
        input.setAverageUseCases(parseInt(averageUseCasesField.getText()));
        input.setComplexUseCases(parseInt(complexUseCasesField.getText()));
        input.setSimpleActors(parseInt(simpleActorsField.getText()));
        input.setAverageActors(parseInt(averageActorsField.getText()));
        input.setComplexActors(parseInt(complexActorsField.getText()));
        input.setTechnicalFactors(parseFactorList(technicalFactorsField.getText(), 13));
        input.setEnvironmentalFactors(parseFactorList(environmentalFactorsField.getText(), 8));

        UCPCalculator calculator = new UCPCalculator();
        UCPResult result = calculator.calculate(input);

        designResultArea.append("UAW=" + result.getUaw() + "\n");
        designResultArea.append("UUCW=" + result.getUucw() + "\n");
        designResultArea.append("UUCP=" + result.getUucp() + "\n");
        designResultArea.append("TCF=" + result.getTcf() + "\n");
        designResultArea.append("ECF=" + result.getEcf() + "\n");
        designResultArea.append("UCP=" + result.getUcp() + "\n");
    }

    private int parseInt(String value) {
        if (value == null) {
            return 0;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int[] parseFactorList(String text, int expectedLength) {
        int[] arr = new int[expectedLength];
        if (text == null || text.trim().isEmpty()) {
            return arr;
        }
        String[] parts = text.split(",");
        for (int i = 0; i < expectedLength && i < parts.length; i++) {
            arr[i] = parseInt(parts[i]);
        }
        return arr;
    }

    private void renderProjectResult(ProjectMetricsResult result) {
        codeResultArea.append("项目总LOC=" + result.getTotalLoc() + "\n");
        codeResultArea.append("项目平均圈复杂度=" + result.getAvgCyclomaticComplexity() + "\n");
        codeResultArea.append("类数量=" + result.getClasses().size() + "\n\n");

        for (ClassInfo c : result.getClasses()) {
            codeResultArea.append("Class: " + c.getClassName() + "\n");
            if (c.getSuperClassName() != null && !c.getSuperClassName().trim().isEmpty()) {
                codeResultArea.append("  extends: " + c.getSuperClassName() + "\n");
            }

            Map<String, Double> metrics = c.getMetrics();
            List<String> keys = new ArrayList<>(metrics.keySet());
            Collections.sort(keys);
            for (String k : keys) {
                codeResultArea.append("  " + k + " = " + metrics.get(k) + "\n");
            }
            codeResultArea.append("\n");
        }
    }
}
