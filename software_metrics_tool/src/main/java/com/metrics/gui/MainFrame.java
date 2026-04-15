package com.metrics.gui;

import javax.swing.*;
import java.awt.*;

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
    
    // 设计/需求度量面板组件 (对应 4号 同学的内容)
    private JPanel designMetricsPanel;
    private JButton calculateUcpButton;
    private JTextArea designResultArea;

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
        bottomPanel.add(new JCheckBox("CK度量集", true));
        bottomPanel.add(new JCheckBox("LK度量集", true));
        bottomPanel.add(new JCheckBox("代码行/复杂度", true));
        codeMetricsPanel.add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void initDesignMetricsPanel() {
        designMetricsPanel = new JPanel(new BorderLayout(10, 10));
        designMetricsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 模拟 4号同学的 用例点表单输入区
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("用例点度量参数输入"));
        
        inputPanel.add(new JLabel("简单用例数 (Simple Use Cases):"));
        inputPanel.add(new JTextField("0"));
        inputPanel.add(new JLabel("平均用例数 (Average Use Cases):"));
        inputPanel.add(new JTextField("0"));
        inputPanel.add(new JLabel("复杂用例数 (Complex Use Cases):"));
        inputPanel.add(new JTextField("0"));
        
        calculateUcpButton = new JButton("计算用例点 (UCP)");
        calculateUcpButton.addActionListener(e -> performUCPCalculation());
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
        codeResultArea.append("正在调用解析器解析源码: " + path + "\n");
        codeResultArea.append("正在应用 2号同学的 CK度量 和 LK度量 模块...\n");
        codeResultArea.append("正在应用 3号同学的 代码行 和 圈复杂度 模块...\n");
        // 这里应调用 MetricsManager 进行集成分析
        codeResultArea.append("分析完成。请查看各类的度量结果。\n\n");
    }
    
    private void performUCPCalculation() {
        designResultArea.append("正在调用 4号同学的 用例点计算 模块...\n");
        // 提取 UCPInput，进行计算并输出
        designResultArea.append("用例点计算完成。\n\n");
    }
}
