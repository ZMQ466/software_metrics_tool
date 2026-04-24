package com.metrics.gui;

import com.metrics.core.MetricsManager;
import com.metrics.design.DeepSeekClient;
import com.metrics.design.PlantUmlClassDiagramAnalyzer;
import com.metrics.design.RequirementDesignMetricsEngine;
import com.metrics.design.UCPCalculator;
import com.metrics.design.UCPResult;
import com.metrics.model.ClassInfo;
import com.metrics.model.ProjectMetricsResult;
import com.metrics.model.UCPInput;
import com.metrics.modules.CKLkMetricsCalculator;
import com.metrics.modules.OOMetricsInterpreter;
import com.metrics.modules.TraditionalMetricsCalculator;
import com.metrics.parser.EclipseJdtCodeParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自动化度量工具主界面：代码度量页支持按类浏览与 HTML 样式化展示。
 */
public class MainFrame extends JFrame {

    private static final Color ACCENT = new Color(0x0d9488);
    /** 辅色：靛紫，用于描边、次按钮与部分数据强调 */
    private static final Color ACCENT2 = new Color(0x6366f1);
    private static final Color MUTED = new Color(0x64748b);
    private static final Color PANEL_BG = new Color(0xeff6ff);

    private JTabbedPane tabbedPane;

    private JPanel codeMetricsPanel;
    private JTextField sourcePathField;
    private JButton browseButton;
    private JButton analyzeCodeButton;
    private JButton aiAnalyzeCodeButton;
    private JComboBox<String> classSelectorCombo;
    private JEditorPane classDetailPane;
    private JLabel projectSummaryLabel;
    private JLabel statusLabel;
    private JCheckBox ckMetricsCheckBox;
    private JCheckBox lkMetricsCheckBox;
    private JCheckBox traditionalMetricsCheckBox;

    private List<ClassInfo> lastAnalyzedClasses = Collections.emptyList();

    private JPanel designMetricsPanel;
    private JButton calculateUcpButton;
    private JButton aiAnalyzeDesignButton;
    private JTextArea designResultArea;
    private JTextField fpEiField;
    private JTextField fpEoField;
    private JTextField fpEqField;
    private JTextField fpIlfField;
    private JTextField fpEifField;
    private JTextField fpGscField;
    private JTextField simpleUseCasesField;
    private JTextField averageUseCasesField;
    private JTextField complexUseCasesField;
    private JTextField simpleActorsField;
    private JTextField averageActorsField;
    private JTextField complexActorsField;
    private JTextField algorithmicWeightField;
    private JTextField technicalFactorsField;
    private JTextField environmentalFactorsField;
    private JButton parseUseCasePlantUmlButton;
    private JTextArea useCasePlantUmlArea;

    private JPanel classDiagramPanel;
    private JButton uploadPlantUmlButton;
    private JButton calculateClassDiagramButton;
    private JButton aiAnalyzeClassDiagramButton;
    private JTextArea plantUmlCodeArea;
    private JTextArea classDiagramResultArea;

    private final DeepSeekClient deepSeekClient = new DeepSeekClient();

    public MainFrame() {
        setTitle("软件度量自动化工具");
        setSize(960, 700);
        setMinimumSize(new Dimension(820, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        installTabbedPaneChrome();

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(Font.PLAIN, 13f));
        tabbedPane.setBackground(PANEL_BG);

        initCodeMetricsPanel();
        tabbedPane.addTab("面向对象与代码度量", codeMetricsPanel);

        initDesignMetricsPanel();
        initClassDiagramPanel();
        tabbedPane.addTab("类图度量", classDiagramPanel);
        tabbedPane.addTab("用例点与功能点度量", designMetricsPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void installTabbedPaneChrome() {
        UIManager.put("TabbedPane.background", new Color(0xe2e8f0));
        UIManager.put("TabbedPane.unselectedBackground", new Color(0xf1f5f9));
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TabbedPane.contentAreaColor", PANEL_BG);
        UIManager.put("TabbedPane.foreground", new Color(0x334155));
        UIManager.put("TabbedPane.selectedForeground", new Color(0x0f172a));
        Font tabFont = UIManager.getFont("TabbedPane.font");
        if (tabFont == null) {
            tabFont = UIManager.getFont("Label.font");
        }
        if (tabFont != null) {
            UIManager.put("TabbedPane.font", tabFont.deriveFont(Font.PLAIN, 13f));
        }
    }

    private void initCodeMetricsPanel() {
        codeMetricsPanel = new JPanel(new BorderLayout(12, 12));
        codeMetricsPanel.setBorder(new EmptyBorder(12, 14, 14, 14));
        codeMetricsPanel.setBackground(PANEL_BG);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);

        JPanel pathRow = new JPanel(new BorderLayout(10, 0));
        pathRow.setOpaque(false);
        JLabel pathLbl = new JLabel("源码目录");
        pathLbl.setForeground(MUTED);
        pathLbl.setFont(pathLbl.getFont().deriveFont(Font.PLAIN, 12.5f));
        pathRow.add(pathLbl, BorderLayout.WEST);

        sourcePathField = new JTextField();
        sourcePathField.setFont(sourcePathField.getFont().deriveFont(Font.PLAIN, 12.5f));
        sourcePathField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xe2e8f0)),
                new EmptyBorder(6, 10, 6, 10)));
        pathRow.add(sourcePathField, BorderLayout.CENTER);

        JPanel pathActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pathActions.setOpaque(false);
        browseButton = styledButton("浏览…", false);
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                sourcePathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        analyzeCodeButton = styledButton("开始分析", true);
        analyzeCodeButton.addActionListener(e -> performCodeAnalysis());
        aiAnalyzeCodeButton = styledButton("智能分析", false);
        aiAnalyzeCodeButton.addActionListener(e -> performAiCodeAnalysis());
        pathActions.add(browseButton);
        pathActions.add(analyzeCodeButton);
        pathActions.add(aiAnalyzeCodeButton);
        pathRow.add(pathActions, BorderLayout.EAST);
        north.add(pathRow);
        north.add(Box.createVerticalStrut(10));

        JPanel summaryRow = new JPanel(new BorderLayout());
        summaryRow.setOpaque(false);
        projectSummaryLabel = new JLabel(defaultSummaryHtml());
        projectSummaryLabel.setOpaque(true);
        projectSummaryLabel.setBackground(Color.WHITE);
        projectSummaryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        projectSummaryLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 4, ACCENT),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 3, new Color(0xe9d5ff)),
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(0xe2e8f0)),
                                new EmptyBorder(16, 20, 16, 20)))));
        summaryRow.add(projectSummaryLabel, BorderLayout.CENTER);
        north.add(summaryRow);

        north.add(Box.createVerticalStrut(8));
        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setOpaque(false);
        statusLabel = new JLabel("<html><div style='text-align:center;color:#64748b'>请选择源码目录后点击「开始分析」</div></html>");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 12f));
        statusRow.add(statusLabel, BorderLayout.CENTER);
        north.add(statusRow);

        codeMetricsPanel.add(north, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);

        JPanel selectorCard = new JPanel(new BorderLayout(8, 0));
        selectorCard.setBackground(new Color(0xfcfcff));
        selectorCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 3, ACCENT2),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xc7d2fe)),
                        new EmptyBorder(10, 12, 10, 12))));
        JLabel selLbl = new JLabel("查看类");
        selLbl.setForeground(ACCENT2);
        selLbl.setFont(selLbl.getFont().deriveFont(Font.PLAIN, 12.5f));
        selectorCard.add(selLbl, BorderLayout.WEST);

        classSelectorCombo = new JComboBox<>();
        classSelectorCombo.setFont(classSelectorCombo.getFont().deriveFont(Font.PLAIN, 12.5f));
        classSelectorCombo.setEnabled(false);
        classSelectorCombo.addActionListener(e -> {
            if (classSelectorCombo.getSelectedIndex() >= 0) {
                refreshSelectedClassDetail();
            }
        });
        selectorCard.add(classSelectorCombo, BorderLayout.CENTER);
        center.add(selectorCard, BorderLayout.NORTH);

        classDetailPane = new JEditorPane();
        classDetailPane.setEditable(false);
        classDetailPane.setContentType("text/html");
        classDetailPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        classDetailPane.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        classDetailPane.setBorder(new EmptyBorder(4, 4, 4, 4));
        classDetailPane.setText(emptyDetailHtml("尚未执行分析", "选择源码目录并点击「开始分析」后，在此查看单个类的度量与解释。"));
        JScrollPane detailScroll = new JScrollPane(classDetailPane);
        detailScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(3, 0, 0, 0, new Color(0x99f6e4)),
                BorderFactory.createLineBorder(new Color(0xcbd5e1))));
        detailScroll.getViewport().setBackground(Color.WHITE);
        center.add(detailScroll, BorderLayout.CENTER);

        codeMetricsPanel.add(center, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 8));
        bottomPanel.setBackground(new Color(0xf5f3ff));
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xe9d5ff)),
                new EmptyBorder(8, 14, 8, 14)));
        ckMetricsCheckBox = new JCheckBox("CK 度量集", true);
        lkMetricsCheckBox = new JCheckBox("LK 度量集", true);
        traditionalMetricsCheckBox = new JCheckBox("代码行 / 圈复杂度", true);
        for (JCheckBox cb : new JCheckBox[] { ckMetricsCheckBox, lkMetricsCheckBox, traditionalMetricsCheckBox }) {
            cb.setOpaque(false);
            cb.setForeground(new Color(0x4338ca));
        }
        bottomPanel.add(ckMetricsCheckBox);
        bottomPanel.add(lkMetricsCheckBox);
        bottomPanel.add(traditionalMetricsCheckBox);
        codeMetricsPanel.add(bottomPanel, BorderLayout.SOUTH);

        ActionListener metricVisibilityListener = e -> {
            if (!lastAnalyzedClasses.isEmpty()) {
                refreshSelectedClassDetail();
            }
        };
        ckMetricsCheckBox.addActionListener(metricVisibilityListener);
        lkMetricsCheckBox.addActionListener(metricVisibilityListener);
        traditionalMetricsCheckBox.addActionListener(metricVisibilityListener);
    }

    private static JButton styledButton(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 12.5f));
        b.setFocusPainted(false);
        if (primary) {
            b.setBackground(ACCENT);
            b.setForeground(Color.WHITE);
            b.setOpaque(true);
            b.setBorderPainted(false);
            b.setBorder(new EmptyBorder(8, 16, 8, 16));
        } else {
            b.setBackground(new Color(0xf8fafc));
            b.setForeground(ACCENT2);
            b.setOpaque(true);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xa5b4fc)),
                    new EmptyBorder(7, 14, 7, 14)));
        }
        return b;
    }

    private void initDesignMetricsPanel() {
        designMetricsPanel = new JPanel(new BorderLayout(10, 10));
        designMetricsPanel.setBorder(new EmptyBorder(8, 12, 10, 12));
        designMetricsPanel.setBackground(PANEL_BG);

        // 使用 GridBagLayout 实现 6列，比例 1:2:1:2:1:2
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(new Color(0xfffbeb));
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 3, new Color(0xfbbf24)),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xfde68a)),
                        new EmptyBorder(8, 10, 8, 10))));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 列宽度比例：1,2,1,2,1,2
        double[] weights = { 1.0, 2.0, 1.0, 2.0, 1.0, 2.0 };

        int row = 0;

        // ========== 第1行 ==========
        // 列0: EI
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = weights[0];
        addLabeledFieldCompactGrid(inputPanel, gbc, "EI", fpEiField = new JTextField("0"));
        // 列1: 输入框
        gbc.gridx = 1;
        gbc.weightx = weights[1];
        inputPanel.add(fpEiField, gbc);
        // 列2: EO
        gbc.gridx = 2;
        gbc.weightx = weights[2];
        addLabeledFieldCompactGrid(inputPanel, gbc, "EO", fpEoField = new JTextField("0"));
        // 列3: 输入框
        gbc.gridx = 3;
        gbc.weightx = weights[3];
        inputPanel.add(fpEoField, gbc);
        // 列4: EQ
        gbc.gridx = 4;
        gbc.weightx = weights[4];
        addLabeledFieldCompactGrid(inputPanel, gbc, "EQ", fpEqField = new JTextField("0"));
        // 列5: 输入框
        gbc.gridx = 5;
        gbc.weightx = weights[5];
        inputPanel.add(fpEqField, gbc);
        row++;

        // ========== 第2行 ==========
        // 列0: ILF
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = weights[0];
        addLabeledFieldCompactGrid(inputPanel, gbc, "ILF", fpIlfField = new JTextField("0"));
        gbc.gridx = 1;
        gbc.weightx = weights[1];
        inputPanel.add(fpIlfField, gbc);
        // 列2: EIF
        gbc.gridx = 2;
        gbc.weightx = weights[2];
        addLabeledFieldCompactGrid(inputPanel, gbc, "EIF", fpEifField = new JTextField("0"));
        gbc.gridx = 3;
        gbc.weightx = weights[3];
        inputPanel.add(fpEifField, gbc);
        // 列4: GSC总分
        gbc.gridx = 4;
        gbc.weightx = weights[4];
        addLabeledFieldCompactGrid(inputPanel, gbc, "GSC总分", fpGscField = new JTextField("0"));
        gbc.gridx = 5;
        gbc.weightx = weights[5];
        inputPanel.add(fpGscField, gbc);
        row++;

        // ========== 第3行 ==========
        // 列0: 简单用例
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = weights[0];
        addLabeledFieldCompactGrid(inputPanel, gbc, "简单用例", simpleUseCasesField = new JTextField("0"));
        gbc.gridx = 1;
        gbc.weightx = weights[1];
        inputPanel.add(simpleUseCasesField, gbc);
        // 列2: 平均用例
        gbc.gridx = 2;
        gbc.weightx = weights[2];
        addLabeledFieldCompactGrid(inputPanel, gbc, "平均用例", averageUseCasesField = new JTextField("0"));
        gbc.gridx = 3;
        gbc.weightx = weights[3];
        inputPanel.add(averageUseCasesField, gbc);
        // 列4: 复杂用例
        gbc.gridx = 4;
        gbc.weightx = weights[4];
        addLabeledFieldCompactGrid(inputPanel, gbc, "复杂用例", complexUseCasesField = new JTextField("0"));
        gbc.gridx = 5;
        gbc.weightx = weights[5];
        inputPanel.add(complexUseCasesField, gbc);
        row++;

        // ========== 第4行 ==========
        // 列0: 简单参与者
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = weights[0];
        addLabeledFieldCompactGrid(inputPanel, gbc, "简单参与者", simpleActorsField = new JTextField("0"));
        gbc.gridx = 1;
        gbc.weightx = weights[1];
        inputPanel.add(simpleActorsField, gbc);
        // 列2: 平均参与者
        gbc.gridx = 2;
        gbc.weightx = weights[2];
        addLabeledFieldCompactGrid(inputPanel, gbc, "平均参与者", averageActorsField = new JTextField("0"));
        gbc.gridx = 3;
        gbc.weightx = weights[3];
        inputPanel.add(averageActorsField, gbc);
        // 列4: 复杂参与者
        gbc.gridx = 4;
        gbc.weightx = weights[4];
        addLabeledFieldCompactGrid(inputPanel, gbc, "复杂参与者", complexActorsField = new JTextField("0"));
        gbc.gridx = 5;
        gbc.weightx = weights[5];
        inputPanel.add(complexActorsField, gbc);
        row++;

        // ========== 第5行：特征点权重（跨两列，但为了保持布局，放在第5行） ==========
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = weights[0];
        addLabeledFieldCompactGrid(inputPanel, gbc, "特征点权重", algorithmicWeightField = new JTextField("1.00"));
        gbc.gridx = 1;
        gbc.weightx = weights[1];
        inputPanel.add(algorithmicWeightField, gbc);
        // 列2-5 留空或放其他
        row++;

        // ========== 第6行：技术因子（独占一行，跨6列） ==========
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 6;
        gbc.weightx = 1.0;
        JPanel techPanel = new JPanel(new BorderLayout(8, 0));
        techPanel.setOpaque(false);
        JLabel techLabel = new JLabel("技术因子 (13项, 0-5):");
        techLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        techLabel.setPreferredSize(new Dimension(140, 26));
        techPanel.add(techLabel, BorderLayout.WEST);
        technicalFactorsField = new JTextField("0,0,0,0,0,0,0,0,0,0,0,0,0");
        techPanel.add(technicalFactorsField, BorderLayout.CENTER);
        inputPanel.add(techPanel, gbc);
        gbc.gridwidth = 1;
        row++;

        // ========== 第7行：环境因子（独占一行，跨6列） ==========
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 6;
        JPanel envPanel = new JPanel(new BorderLayout(8, 0));
        envPanel.setOpaque(false);
        JLabel envLabel = new JLabel("环境因子 (8项, 0-5):");
        envLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        envLabel.setPreferredSize(new Dimension(140, 26));
        envPanel.add(envLabel, BorderLayout.WEST);
        environmentalFactorsField = new JTextField("0,0,0,0,0,0,0,0");
        envPanel.add(environmentalFactorsField, BorderLayout.CENTER);
        inputPanel.add(envPanel, gbc);
        gbc.gridwidth = 1;
        row++;

        // ========== 第8行：PlantUML 用例图输入（跨6列） ==========
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 6;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        JPanel useCasePlantPanel = new JPanel(new BorderLayout(8, 8));
        useCasePlantPanel.setOpaque(false);
        JLabel useCasePlantLabel = new JLabel("PlantUML 用例图输入（可选，解析后自动填充用例/参与者分类）");
        useCasePlantLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        useCasePlantPanel.add(useCasePlantLabel, BorderLayout.NORTH);

        useCasePlantUmlArea = new JTextArea(5, 20);
        useCasePlantUmlArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        useCasePlantUmlArea.setLineWrap(true);
        useCasePlantUmlArea.setWrapStyleWord(true);
        useCasePlantUmlArea.setText("@startuml\n"
                + "left to right direction\n"
                + "actor User\n"
                + "actor Admin\n"
                + "User --> (Login)\n"
                + "User --> (Search Book)\n"
                + "Admin --> (Manage Order)\n"
                + "@enduml\n");
        JScrollPane useCasePlantScroll = new JScrollPane(useCasePlantUmlArea);
        useCasePlantScroll.setBorder(BorderFactory.createLineBorder(new Color(0xe2e8f0)));
        useCasePlantPanel.add(useCasePlantScroll, BorderLayout.CENTER);

        parseUseCasePlantUmlButton = styledButton("解析PlantUML用例图", false);
        parseUseCasePlantUmlButton.addActionListener(e -> performParseUseCasePlantUml());
        JPanel parseButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        parseButtonPanel.setOpaque(false);
        parseButtonPanel.add(parseUseCasePlantUmlButton);
        useCasePlantPanel.add(parseButtonPanel, BorderLayout.SOUTH);

        inputPanel.add(useCasePlantPanel, gbc);
        gbc.gridwidth = 1;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        row++;

        // ========== 第9行：按钮 ==========
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 6;
        gbc.anchor = GridBagConstraints.EAST;
        calculateUcpButton = styledButton("计算 FP / UCP / 特征点", true);
        calculateUcpButton.addActionListener(e -> performDesignMetricsCalculation());
        aiAnalyzeDesignButton = styledButton("智能分析", false);
        aiAnalyzeDesignButton.addActionListener(e -> performAiDesignAnalysis());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(aiAnalyzeDesignButton);
        buttonPanel.add(calculateUcpButton);
        inputPanel.add(buttonPanel, gbc);

        designMetricsPanel.add(inputPanel, BorderLayout.NORTH);

        // 结果区域（保持不变）
        designResultArea = new JTextArea();
        designResultArea.setEditable(false);
        designResultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        designResultArea.setText("点击 \"计算 FP / UCP / 特征点\" 以获得三类需求/设计度量结果\n");
        designResultArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        designResultArea.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(designResultArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xe2e8f0)));
        designMetricsPanel.add(scrollPane, BorderLayout.CENTER);
    }

    // 辅助方法：只添加标签，不添加文本框
    private void addLabeledFieldCompactGrid(JPanel panel, GridBagConstraints gbc,
            String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        panel.add(label, gbc);
    }

    private void initClassDiagramPanel() {
        classDiagramPanel = new JPanel(new BorderLayout(10, 10));
        classDiagramPanel.setBorder(new EmptyBorder(12, 14, 14, 14));
        classDiagramPanel.setBackground(PANEL_BG);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);
        uploadPlantUmlButton = styledButton("上传 PlantUML 代码", false);
        uploadPlantUmlButton.addActionListener(e -> performUploadPlantUml());
        calculateClassDiagramButton = styledButton("解析 + CK/LK 度量分析", true);
        calculateClassDiagramButton.addActionListener(e -> performClassDiagramCalculation());
        aiAnalyzeClassDiagramButton = styledButton("智能分析", false);
        aiAnalyzeClassDiagramButton.addActionListener(e -> performAiClassDiagramAnalysis());
        actionPanel.add(uploadPlantUmlButton);
        actionPanel.add(calculateClassDiagramButton);
        actionPanel.add(aiAnalyzeClassDiagramButton);
        classDiagramPanel.add(actionPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        centerPanel.setOpaque(false);

        plantUmlCodeArea = new JTextArea();
        plantUmlCodeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        plantUmlCodeArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        plantUmlCodeArea.setBackground(Color.WHITE);
        plantUmlCodeArea
                .setText("@startuml\nclass User {\n  +id: Long\n  +name: String\n  +getName(): String\n}\n@enduml\n");
        JScrollPane inputScroll = new JScrollPane(plantUmlCodeArea);
        inputScroll.setBorder(BorderFactory.createTitledBorder("PlantUML Class Diagram"));
        centerPanel.add(inputScroll);

        classDiagramResultArea = new JTextArea();
        classDiagramResultArea.setEditable(false);
        classDiagramResultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        classDiagramResultArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        classDiagramResultArea.setBackground(Color.WHITE);
        classDiagramResultArea.setText("上传 PlantUML 代码，点击 解析 + CK/LK 分析 以获得结果.\n");
        JScrollPane resultScroll = new JScrollPane(classDiagramResultArea);
        resultScroll.setBorder(BorderFactory.createTitledBorder("CK/LK Metrics Result"));
        centerPanel.add(resultScroll);

        classDiagramPanel.add(centerPanel, BorderLayout.CENTER);
    }

    private static void addLabeledField(JPanel grid, String label, JTextField field) {
        JLabel l = new JLabel(label);
        l.setForeground(MUTED);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        grid.add(l);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xe2e8f0)),
                new EmptyBorder(6, 8, 6, 8)));
        grid.add(field);
    }

    private static void addLabeledFieldCompact(JPanel grid, String label, JTextField field) {
        JLabel l = new JLabel(label);
        l.setForeground(MUTED);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11.5f));
        grid.add(l);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xe2e8f0)),
                new EmptyBorder(3, 8, 3, 8)));
        grid.add(field);
    }

    private void performCodeAnalysis() {
        String path = sourcePathField.getText();
        if (path == null || path.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择源码目录", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        classSelectorCombo.setEnabled(false);
        statusLabel.setForeground(MUTED);
        statusLabel.setText("<html><div style='text-align:center;color:#0e7490'>正在解析与度量…</div></html>");
        projectSummaryLabel.setText(summaryHtmlLoading());
        classDetailPane.setText(emptyDetailHtml("正在分析", escapeHtml(path)));

        MetricsManager manager = new MetricsManager();
        manager.setParser(new EclipseJdtCodeParser());
        /* 始终计算全部指标；下方复选框仅控制展示 */
        manager.registerCalculator(new CKLkMetricsCalculator());
        manager.registerCalculator(new TraditionalMetricsCalculator());

        try {
            ProjectMetricsResult result = manager.runAnalysis(path);
            applyCodeAnalysisResult(result);
        } catch (Exception ex) {
            lastAnalyzedClasses = Collections.emptyList();
            classSelectorCombo.removeAllItems();
            classSelectorCombo.setEnabled(false);
            statusLabel.setForeground(new Color(0xb91c1c));
            statusLabel.setText("<html><div style='text-align:center;color:#b91c1c'>分析失败，请检查路径或依赖后重试</div></html>");
            projectSummaryLabel.setText(summaryHtmlError());
            classDetailPane.setText(
                    emptyDetailHtml("分析失败", escapeHtml(ex.getMessage() != null ? ex.getMessage() : ex.toString())));
        }
    }

    private void applyCodeAnalysisResult(ProjectMetricsResult result) {
        List<ClassInfo> sorted = new ArrayList<>(result.getClasses());
        sorted.sort(Comparator.comparing(ClassInfo::getQualifiedName, String.CASE_INSENSITIVE_ORDER));
        lastAnalyzedClasses = sorted;

        projectSummaryLabel.setText(summaryHtml(
                formatDouble(result.getTotalLoc()),
                formatDouble(result.getAvgCyclomaticComplexity()),
                sorted.size()));

        statusLabel.setForeground(new Color(0x047857));
        statusLabel.setText("<html><div style='text-align:center;color:#047857'>分析完成 · 共 <b>" + sorted.size()
                + "</b> 个类，请在下拉框中选择要查看的类</div></html>");

        classSelectorCombo.removeAllItems();
        for (ClassInfo c : sorted) {
            classSelectorCombo.addItem(c.getQualifiedName());
        }
        classSelectorCombo.setEnabled(!sorted.isEmpty());
        if (!sorted.isEmpty()) {
            classSelectorCombo.setSelectedIndex(0);
        } else {
            classDetailPane.setText(emptyDetailHtml("无类", "未在目录中解析到任何 Java 类。"));
        }
        refreshSelectedClassDetail();
    }

    private void refreshSelectedClassDetail() {
        int idx = classSelectorCombo.getSelectedIndex();
        if (idx < 0 || idx >= lastAnalyzedClasses.size()) {
            return;
        }
        ClassInfo c = lastAnalyzedClasses.get(idx);
        classDetailPane.setText(buildClassDetailHtml(
                c,
                ckMetricsCheckBox.isSelected(),
                lkMetricsCheckBox.isSelected(),
                traditionalMetricsCheckBox.isSelected()));
        classDetailPane.setCaretPosition(0);
    }

    private static String defaultSummaryHtml() {
        return "<html><body style='margin:0'>"
                + "<div style='text-align:center;width:100%;font-family:Segoe UI,Microsoft YaHei UI,sans-serif;color:#475569'>"
                + "<div style='font-size:13px;color:#64748b;letter-spacing:0.08em;font-weight:700;margin-bottom:10px'>项目概览</div>"
                + "<div style='font-size:13px;line-height:1.65;color:#64748b;margin-bottom:12px'>"
                + "尚未执行分析。选择源码目录并点击「开始分析」后，将在此展示统计结果。"
                + "</div>"
                + "<div style='font-size:12px;line-height:2.2;padding:4px 0'>"
                + chipStat("总 LOC", "—", "#0369a1", "#e0f2fe")
                + chipStat("平均圈复杂度", "—", "#5b21b6", "#ede9fe")
                + chipStat("类数量", "—", "#0f766e", "#ccfbf1")
                + "</div>"
                + "</div></body></html>";
    }

    private static String summaryHtmlLoading() {
        return "<html><body style='margin:0'>"
                + "<div style='text-align:center;width:100%;font-family:Segoe UI,Microsoft YaHei UI,sans-serif'>"
                + "<div style='font-size:13px;color:#64748b;letter-spacing:0.08em;font-weight:700;margin-bottom:10px'>项目概览</div>"
                + "<div style='font-size:14px;font-weight:700;color:#0d9488'>正在解析与度量…</div>"
                + "<div style='font-size:12px;color:#64748b;margin-top:8px'>请稍候，统计完成后将自动更新</div>"
                + "</div></body></html>";
    }

    private static String summaryHtmlError() {
        return "<html><body style='margin:0'>"
                + "<div style='text-align:center;width:100%;font-family:Segoe UI,Microsoft YaHei UI,sans-serif'>"
                + "<div style='font-size:13px;color:#94a3b8;letter-spacing:0.08em;font-weight:700;margin-bottom:10px'>项目概览</div>"
                + "<div style='font-size:13px;color:#b91c1c;font-weight:600'>无法完成本次统计</div>"
                + "<div style='font-size:12px;color:#64748b;margin-top:8px'>请检查源码路径、权限或控制台错误信息后重试</div>"
                + "</div></body></html>";
    }

    private static String summaryHtml(String totalLoc, String avgCc, int classCount) {
        return "<html><body style='margin:0'>"
                + "<div style='text-align:center;width:100%;font-family:Segoe UI,Microsoft YaHei UI,sans-serif;color:#334155'>"
                + "<div style='font-size:13px;color:#64748b;letter-spacing:0.08em;font-weight:700;margin-bottom:12px'>项目概览</div>"
                + "<div style='font-size:12px;line-height:2.2;padding:4px 0'>"
                + chipStat("总 LOC", totalLoc, "#0369a1", "#e0f2fe")
                + chipStat("平均圈复杂度", avgCc, "#5b21b6", "#ede9fe")
                + chipStat("类数量", String.valueOf(classCount), "#0f766e", "#ccfbf1")
                + "</div>"
                + "</div></body></html>";
    }

    private static String chipStat(String label, String value, String fg, String bg) {
        /* 左右 margin 较大，避免三个标签在居中排布时视觉挤在一起（Swing HTML 对 flex gap 支持差） */
        return "<span style='display:inline-block;background:" + bg + ";color:" + fg
                + ";padding:8px 18px;border-radius:999px;font-size:12px;font-weight:700;"
                + "margin:6px 28px;vertical-align:middle'>"
                + "<span style='font-weight:600;opacity:.88'>" + escapeHtml(label) + "</span> "
                + "<span style='font-weight:800'>" + escapeHtml(value) + "</span></span>";
    }

    private static String emptyDetailHtml(String title, String body) {
        return "<html><head><meta charset='UTF-8'>" + detailStyles() + "</head><body>"
                + "<h2 style='color:#94a3b8'>" + escapeHtml(title) + "</h2>"
                + "<p class='muted'>" + body + "</p>"
                + "</body></html>";
    }

    private static String detailStyles() {
        return "<style>"
                + "body { font-family:Segoe UI,Microsoft YaHei UI,sans-serif;margin:12px 14px;color:#1e293b;font-size:13px; }"
                + "h2 { font-size:18px;color:#0f766e;margin:0 0 6px 0;font-weight:700; }"
                + ".muted { color:#64748b;font-size:13px;line-height:1.55;margin:0; }"
                + ".sub { color:#64748b;font-size:12px;margin:0 0 14px 0; }"
                + ".sec { color:#6366f1;font-size:11px;text-transform:uppercase;letter-spacing:0.08em;margin:18px 0 8px 0;font-weight:700; }"
                + "table.metrics { border-collapse:collapse;width:100%;max-width:640px; }"
                + "td.k { color:#475569;padding:6px 14px 6px 0;font-family:Consolas,Courier New,monospace;font-size:12px;vertical-align:top;border-bottom:1px solid #eef2ff; }"
                + "td.v { color:#0c4a6e;font-weight:700;font-family:Consolas,Courier New,monospace;font-size:12px;padding:6px 0;border-bottom:1px solid #eef2ff; }"
                + ".interpret { margin-top:18px;padding:14px 16px;background:linear-gradient(135deg,#ecfeff 0%,#f0fdfa 100%);"
                + "border-radius:12px;border-left:5px solid #14b8a6;box-shadow:0 1px 2px rgba(15,118,110,0.06); }"
                + ".interpret h3 { margin:0 0 10px 0;font-size:13px;color:#0f766e;font-weight:700;letter-spacing:0.02em; }"
                + ".interpret pre { margin:0;white-space:pre-wrap;font-family:Segoe UI,Microsoft YaHei UI,sans-serif;font-size:12.5px;"
                + "line-height:1.6;color:#115e59; }"
                + "</style>";
    }

    /**
     * 按复选框过滤展示的指标行与解释；{@code ClassInfo} 中仍保留全部分析结果。
     */
    private static String buildClassDetailHtml(ClassInfo c, boolean showCk, boolean showLk, boolean showTrad) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset='UTF-8'>");
        sb.append(detailStyles());
        sb.append("</head><body>");

        sb.append("<h2>").append(escapeHtml(c.getClassName())).append("</h2>");
        sb.append("<p class='sub'>").append(escapeHtml(c.getQualifiedName())).append("</p>");
        if (c.getSuperClassName() != null && !c.getSuperClassName().trim().isEmpty()) {
            sb.append("<p class='sub'>extends <span style='color:#0d9488;font-weight:600'>")
                    .append(escapeHtml(c.getSuperClassName().trim()))
                    .append("</span></p>");
        }

        sb.append("<p class='sec'>度量指标</p>");
        Map<String, Double> metrics = c.getMetrics();
        List<String> keys = new ArrayList<>(metrics.keySet());
        Collections.sort(keys);
        boolean anyGroup = showCk || showLk || showTrad;
        int visibleRows = 0;
        if (!anyGroup) {
            sb.append("<p class='muted' style='margin-top:4px'>请至少勾选一个度量类别以在表格中展示指标（分析时仍已计算全部度量）。</p>");
        } else {
            sb.append("<table class='metrics'>");
            for (String k : keys) {
                if (!isMetricVisibleInUi(k, showCk, showLk, showTrad)) {
                    continue;
                }
                visibleRows++;
                sb.append("<tr><td class='k'>").append(escapeHtml(k)).append("</td><td class='v'>")
                        .append(escapeHtml(formatDouble(metrics.get(k))))
                        .append("</td></tr>");
            }
            sb.append("</table>");
            if (visibleRows == 0) {
                sb.append("<p class='muted' style='margin-top:8px'>当前勾选下没有可展示的指标行。</p>");
            }
        }

        boolean showOoInterpret = showCk || showLk;
        if (showOoInterpret) {
            String interp = OOMetricsInterpreter.interpretClass(c, showCk, showLk);
            if (interp != null && !interp.isEmpty()) {
                sb.append("<div class='interpret'><h3>指标解释</h3><pre>")
                        .append(escapeHtml(interp))
                        .append("</pre></div>");
            }
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private static boolean isMetricVisibleInUi(String key, boolean showCk, boolean showLk, boolean showTrad) {
        if (key == null) {
            return false;
        }
        if (key.startsWith("CK_")) {
            return showCk;
        }
        if (key.startsWith("LK_")) {
            return showLk;
        }
        if ("LOC_CLASS".equals(key) || "CC_AVG_CLASS".equals(key)) {
            return showTrad;
        }
        return true;
    }

    private static String formatDouble(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void performDesignMetricsCalculation() {
        designResultArea.setText("");

        try {
            RequirementDesignMetricsEngine.FunctionPointInput fpInput = new RequirementDesignMetricsEngine.FunctionPointInput(
                    parseInt(fpEiField.getText()),
                    parseInt(fpEoField.getText()),
                    parseInt(fpEqField.getText()),
                    parseInt(fpIlfField.getText()),
                    parseInt(fpEifField.getText()),
                    parseInt(fpGscField.getText()));
            RequirementDesignMetricsEngine.FunctionPointResult fpResult = RequirementDesignMetricsEngine
                    .calculateFunctionPoint(fpInput);

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
            UCPResult ucpResult = calculator.calculate(input);

            RequirementDesignMetricsEngine.FeaturePointInput featurePointInput = new RequirementDesignMetricsEngine.FeaturePointInput(
                    parseDouble(algorithmicWeightField.getText()));
            RequirementDesignMetricsEngine.FeaturePointResult featurePointResult = RequirementDesignMetricsEngine
                    .calculateFeaturePoint(fpResult, featurePointInput);

            designResultArea.append("=== 功能点 FP ===\n");
            designResultArea.append("UFP=" + fpResult.ufp + "\n");
            designResultArea.append("VAF=" + formatDouble(fpResult.vaf) + "\n");
            designResultArea.append("FP=" + formatDouble(fpResult.adjustedFp) + "\n\n");

            designResultArea.append("=== 用例点 UCP ===\n");
            designResultArea.append("UAW=" + ucpResult.getUaw() + "\n");
            designResultArea.append("UUCW=" + ucpResult.getUucw() + "\n");
            designResultArea.append("UUCP=" + ucpResult.getUucp() + "\n");
            designResultArea.append("TCF=" + formatDouble(ucpResult.getTcf()) + "\n");
            designResultArea.append("ECF=" + formatDouble(ucpResult.getEcf()) + "\n");
            designResultArea.append("UCP=" + formatDouble(ucpResult.getUcp()) + "\n\n");

            designResultArea.append("=== 特征点 Feature Point ===\n");
            designResultArea.append("AlgorithmicWeight=" + formatDouble(featurePointResult.algorithmicWeight) + "\n");
            designResultArea.append("FeaturePoint=" + formatDouble(featurePointResult.featurePoint) + "\n");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "输入错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performParseUseCasePlantUml() {
        String plantUml = useCasePlantUmlArea.getText();
        if (plantUml == null || plantUml.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "请先输入 PlantUML 用例图代码。",
                    "输入为空",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            UseCasePlantUmlParseResult parsed = parseUseCasePlantUml(plantUml);

            simpleUseCasesField.setText(String.valueOf(parsed.simpleUseCases));
            averageUseCasesField.setText(String.valueOf(parsed.averageUseCases));
            complexUseCasesField.setText(String.valueOf(parsed.complexUseCases));
            simpleActorsField.setText(String.valueOf(parsed.simpleActors));
            averageActorsField.setText(String.valueOf(parsed.averageActors));
            complexActorsField.setText(String.valueOf(parsed.complexActors));

            String summary = "PlantUML 解析完成：用例(简单/平均/复杂)="
                    + parsed.simpleUseCases + "/" + parsed.averageUseCases + "/" + parsed.complexUseCases
                    + "，参与者(简单/平均/复杂)="
                    + parsed.simpleActors + "/" + parsed.averageActors + "/" + parsed.complexActors;

            designResultArea.append("\n=== PlantUML 用例图自动填充 ===\n");
            designResultArea.append(summary + "\n");
            JOptionPane.showMessageDialog(this, summary, "解析成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "解析失败",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private UseCasePlantUmlParseResult parseUseCasePlantUml(String rawText) {
        String text = rawText.replace("\r\n", "\n");

        Pattern actorDeclPattern = Pattern.compile(
                "(?im)^\\s*actor\\s+(?:\\\"([^\\\"]+)\\\"\\s+as\\s+)?(\\\"[^\\\"]+\\\"|[A-Za-z_][A-Za-z0-9_]*)?.*$");
        Pattern actorToUseCasePattern = Pattern
                .compile("(\\\"[^\\\"]+\\\"|[A-Za-z_][A-Za-z0-9_]*)\\s*[-.o*]+>\\s*\\(([^)]+)\\)");
        Pattern useCaseToActorPattern = Pattern
                .compile("\\(([^)]+)\\)\\s*[-.o*]+>\\s*(\\\"[^\\\"]+\\\"|[A-Za-z_][A-Za-z0-9_]*)");
        Pattern useCasePattern = Pattern.compile("\\(([^)]+)\\)");

        Set<String> declaredActors = new LinkedHashSet<>();
        Matcher actorDeclMatcher = actorDeclPattern.matcher(text);
        while (actorDeclMatcher.find()) {
            String aliasOrName = actorDeclMatcher.group(2);
            String quotedName = actorDeclMatcher.group(1);
            String actorName = sanitizeReference(
                    aliasOrName != null && !aliasOrName.trim().isEmpty() ? aliasOrName : quotedName);
            if (!actorName.isEmpty()) {
                declaredActors.add(actorName);
            }
        }

        Map<String, Set<String>> actorToUseCases = new LinkedHashMap<>();
        Map<String, Set<String>> useCaseToActors = new LinkedHashMap<>();
        Set<String> allUseCases = new LinkedHashSet<>();

        Matcher ucMatcher = useCasePattern.matcher(text);
        while (ucMatcher.find()) {
            String useCase = sanitizeUseCaseName(ucMatcher.group(1));
            if (!useCase.isEmpty()) {
                allUseCases.add(useCase);
            }
        }

        Matcher a2u = actorToUseCasePattern.matcher(text);
        while (a2u.find()) {
            String actor = sanitizeReference(a2u.group(1));
            String useCase = sanitizeUseCaseName(a2u.group(2));
            if (actor.isEmpty() || useCase.isEmpty()) {
                continue;
            }
            declaredActors.add(actor);
            allUseCases.add(useCase);
            actorToUseCases.computeIfAbsent(actor, k -> new LinkedHashSet<>()).add(useCase);
            useCaseToActors.computeIfAbsent(useCase, k -> new LinkedHashSet<>()).add(actor);
        }

        Matcher u2a = useCaseToActorPattern.matcher(text);
        while (u2a.find()) {
            String useCase = sanitizeUseCaseName(u2a.group(1));
            String actor = sanitizeReference(u2a.group(2));
            if (actor.isEmpty() || useCase.isEmpty()) {
                continue;
            }
            declaredActors.add(actor);
            allUseCases.add(useCase);
            actorToUseCases.computeIfAbsent(actor, k -> new LinkedHashSet<>()).add(useCase);
            useCaseToActors.computeIfAbsent(useCase, k -> new LinkedHashSet<>()).add(actor);
        }

        if (declaredActors.isEmpty() && allUseCases.isEmpty()) {
            throw new IllegalArgumentException("未识别到参与者或用例，请检查 PlantUML 语法（如 actor A、A --> (Login)）。");
        }

        for (String actor : declaredActors) {
            actorToUseCases.computeIfAbsent(actor, k -> new LinkedHashSet<>());
        }
        for (String useCase : allUseCases) {
            useCaseToActors.computeIfAbsent(useCase, k -> new LinkedHashSet<>());
        }

        int simpleUc = 0;
        int averageUc = 0;
        int complexUc = 0;
        for (String useCase : allUseCases) {
            int actorCount = useCaseToActors.getOrDefault(useCase, Collections.emptySet()).size();
            if (actorCount <= 1) {
                simpleUc++;
            } else if (actorCount == 2) {
                averageUc++;
            } else {
                complexUc++;
            }
        }

        int simpleActor = 0;
        int averageActor = 0;
        int complexActor = 0;
        for (String actor : declaredActors) {
            int useCaseCount = actorToUseCases.getOrDefault(actor, Collections.emptySet()).size();
            if (useCaseCount <= 1) {
                simpleActor++;
            } else if (useCaseCount <= 3) {
                averageActor++;
            } else {
                complexActor++;
            }
        }

        return new UseCasePlantUmlParseResult(
                simpleUc,
                averageUc,
                complexUc,
                simpleActor,
                averageActor,
                complexActor);
    }

    private static String sanitizeReference(String raw) {
        if (raw == null) {
            return "";
        }
        String v = raw.trim();
        if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
            v = v.substring(1, v.length() - 1);
        }
        return v.trim();
    }

    private static String sanitizeUseCaseName(String raw) {
        return sanitizeReference(raw);
    }

    private void performUploadPlantUml() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select PlantUML file");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path path = chooser.getSelectedFile().toPath();
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            plantUmlCodeArea.setText(text);
            plantUmlCodeArea.setCaretPosition(0);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to read file: " + ex.getMessage(),
                    "Read Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performClassDiagramCalculation() {
        classDiagramResultArea.setText("");
        String plantUml = plantUmlCodeArea.getText();
        if (plantUml == null || plantUml.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please paste PlantUML class diagram text first.",
                    "Input Required",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            ProjectMetricsResult result = PlantUmlClassDiagramAnalyzer.analyze(plantUml);
            List<ClassInfo> classes = new ArrayList<>(result.getClasses());
            classes.sort(Comparator.comparing(ClassInfo::getQualifiedName, String.CASE_INSENSITIVE_ORDER));

            if (classes.isEmpty()) {
                classDiagramResultArea.setText("No classes parsed from PlantUML.\n");
                return;
            }

            classDiagramResultArea.append("Classes Parsed=" + classes.size() + "\n\n");
            double sumWmc = 0.0;
            double sumDit = 0.0;
            double sumNoc = 0.0;
            double sumCbo = 0.0;
            double sumLcom = 0.0;

            for (ClassInfo c : classes) {
                double wmc = c.getMetrics().getOrDefault("CK_WMC", 0.0);
                double dit = c.getMetrics().getOrDefault("CK_DIT", 0.0);
                double noc = c.getMetrics().getOrDefault("CK_NOC", 0.0);
                double cbo = c.getMetrics().getOrDefault("CK_CBO", 0.0);
                double lcom = c.getMetrics().getOrDefault("CK_LCOM", 0.0);

                sumWmc += wmc;
                sumDit += dit;
                sumNoc += noc;
                sumCbo += cbo;
                sumLcom += lcom;

                classDiagramResultArea.append("[" + c.getQualifiedName() + "]\n");
                classDiagramResultArea.append("  WMC=" + formatDouble(wmc) + "\n");
                classDiagramResultArea.append("  DIT=" + formatDouble(dit) + "\n");
                classDiagramResultArea.append("  NOC=" + formatDouble(noc) + "\n");
                classDiagramResultArea.append("  CBO=" + formatDouble(cbo) + "\n");
                classDiagramResultArea.append("  LCOM=" + formatDouble(lcom) + "\n\n");
            }

            int n = classes.size();
            classDiagramResultArea.append("=== Average ===\n");
            classDiagramResultArea.append("WMC=" + formatDouble(sumWmc / n) + "\n");
            classDiagramResultArea.append("DIT=" + formatDouble(sumDit / n) + "\n");
            classDiagramResultArea.append("NOC=" + formatDouble(sumNoc / n) + "\n");
            classDiagramResultArea.append("CBO=" + formatDouble(sumCbo / n) + "\n");
            classDiagramResultArea.append("LCOM=" + formatDouble(sumLcom / n) + "\n");
            classDiagramResultArea.setCaretPosition(0);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "PlantUML Parse Error",
                    JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to parse PlantUML: " + ex.getMessage(),
                    "PlantUML Parse Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performAiCodeAnalysis() {
        int idx = classSelectorCombo.getSelectedIndex();
        if (idx < 0 || idx >= lastAnalyzedClasses.size()) {
            JOptionPane.showMessageDialog(this,
                    "请先执行代码分析并选择一个类。",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        ClassInfo c = lastAnalyzedClasses.get(idx);
        String prompt = buildCodePagePrompt(c);
        runAiAnalysisAsync(prompt, aiAnalyzeCodeButton, reply -> showAiResultDialog("代码度量智能分析", reply));
    }

    private void performAiDesignAnalysis() {
        try {
            RequirementDesignMetricsEngine.FunctionPointInput fpInput = new RequirementDesignMetricsEngine.FunctionPointInput(
                    parseInt(fpEiField.getText()),
                    parseInt(fpEoField.getText()),
                    parseInt(fpEqField.getText()),
                    parseInt(fpIlfField.getText()),
                    parseInt(fpEifField.getText()),
                    parseInt(fpGscField.getText()));
            RequirementDesignMetricsEngine.FunctionPointResult fpResult = RequirementDesignMetricsEngine
                    .calculateFunctionPoint(fpInput);

            UCPInput input = new UCPInput();
            input.setSimpleUseCases(parseInt(simpleUseCasesField.getText()));
            input.setAverageUseCases(parseInt(averageUseCasesField.getText()));
            input.setComplexUseCases(parseInt(complexUseCasesField.getText()));
            input.setSimpleActors(parseInt(simpleActorsField.getText()));
            input.setAverageActors(parseInt(averageActorsField.getText()));
            input.setComplexActors(parseInt(complexActorsField.getText()));
            input.setTechnicalFactors(parseFactorList(technicalFactorsField.getText(), 13));
            input.setEnvironmentalFactors(parseFactorList(environmentalFactorsField.getText(), 8));
            UCPResult ucpResult = new UCPCalculator().calculate(input);

            RequirementDesignMetricsEngine.FeaturePointInput featurePointInput = new RequirementDesignMetricsEngine.FeaturePointInput(
                    parseDouble(algorithmicWeightField.getText()));
            RequirementDesignMetricsEngine.FeaturePointResult featurePointResult = RequirementDesignMetricsEngine
                    .calculateFeaturePoint(fpResult, featurePointInput);

            String prompt = buildDesignPagePrompt(fpResult, ucpResult, featurePointResult);
            runAiAnalysisAsync(prompt, aiAnalyzeDesignButton, reply -> showAiResultDialog("用例点与功能点智能分析", reply));
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "输入错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performAiClassDiagramAnalysis() {
        String plantUml = plantUmlCodeArea.getText();
        if (plantUml == null || plantUml.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "请先上传或粘贴 PlantUML 类图代码。",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            ProjectMetricsResult result = PlantUmlClassDiagramAnalyzer.analyze(plantUml);
            List<ClassInfo> classes = new ArrayList<>(result.getClasses());
            if (classes.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "PlantUML 未解析出任何类。",
                        "提示",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String prompt = buildClassDiagramPagePrompt(classes);
            runAiAnalysisAsync(prompt, aiAnalyzeClassDiagramButton, reply -> showAiResultDialog("类图度量智能分析", reply));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "类图分析失败: " + ex.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runAiAnalysisAsync(String prompt, JButton triggerButton, Consumer<String> onSuccess) {
        triggerButton.setEnabled(false);
        String oldText = triggerButton.getText();
        triggerButton.setText("分析中...");
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return deepSeekClient.analyze(prompt);
            }

            @Override
            protected void done() {
                triggerButton.setEnabled(true);
                triggerButton.setText(oldText);
                try {
                    onSuccess.accept(get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "大模型调用失败: " + ex.getMessage()
                                    + "\n请检查 DEEPSEEK_API_KEY / DEEPSEEK_BASE_URL 配置。",
                            "智能分析失败",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private String buildCodePagePrompt(ClassInfo c) {
        Map<String, Double> m = c.getMetrics();
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下软件度量结果，分析该类的设计质量，并给出3条简洁的改进建议。\n");
        sb.append("类名: ").append(c.getQualifiedName()).append("\n");
        sb.append("WMC=").append(metricValue(m, "CK_WMC")).append("\n");
        sb.append("DIT=").append(metricValue(m, "CK_DIT")).append("\n");
        sb.append("NOC=").append(metricValue(m, "CK_NOC")).append("\n");
        sb.append("CBO=").append(metricValue(m, "CK_CBO")).append("\n");
        sb.append("RFC=").append(metricValue(m, "CK_RFC")).append("\n");
        sb.append("LCOM=").append(metricValue(m, "CK_LCOM")).append("\n");
        sb.append("LCOM4=").append(metricValue(m, "LK_LCOM_NORM")).append("\n");
        sb.append("LCOM_HS=").append(metricValue(m, "LK_COHESION")).append("\n\n");
        appendCommonAiRequirements(sb);
        return sb.toString();
    }

    private String buildDesignPagePrompt(RequirementDesignMetricsEngine.FunctionPointResult fp,
            UCPResult ucp,
            RequirementDesignMetricsEngine.FeaturePointResult featurePoint) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下用例点与功能点度量结果，分析当前需求/设计规模与风险，并给出3条简洁的改进建议。\n");
        sb.append("UFP=").append(fp.ufp).append("\n");
        sb.append("VAF=").append(formatDouble(fp.vaf)).append("\n");
        sb.append("FP=").append(formatDouble(fp.adjustedFp)).append("\n");
        sb.append("UAW=").append(formatDouble(ucp.getUaw())).append("\n");
        sb.append("UUCW=").append(formatDouble(ucp.getUucw())).append("\n");
        sb.append("UUCP=").append(formatDouble(ucp.getUucp())).append("\n");
        sb.append("TCF=").append(formatDouble(ucp.getTcf())).append("\n");
        sb.append("ECF=").append(formatDouble(ucp.getEcf())).append("\n");
        sb.append("UCP=").append(formatDouble(ucp.getUcp())).append("\n");
        sb.append("FeaturePoint=").append(formatDouble(featurePoint.featurePoint)).append("\n\n");
        appendCommonAiRequirements(sb);
        return sb.toString();
    }

    private String buildClassDiagramPagePrompt(List<ClassInfo> classes) {
        double sumWmc = 0.0;
        double sumDit = 0.0;
        double sumNoc = 0.0;
        double sumCbo = 0.0;
        double sumRfc = 0.0;
        double sumLcom = 0.0;
        double sumLkNorm = 0.0;
        double sumLkHs = 0.0;
        for (ClassInfo c : classes) {
            Map<String, Double> m = c.getMetrics();
            sumWmc += m.getOrDefault("CK_WMC", 0.0);
            sumDit += m.getOrDefault("CK_DIT", 0.0);
            sumNoc += m.getOrDefault("CK_NOC", 0.0);
            sumCbo += m.getOrDefault("CK_CBO", 0.0);
            sumRfc += m.getOrDefault("CK_RFC", 0.0);
            sumLcom += m.getOrDefault("CK_LCOM", 0.0);
            sumLkNorm += m.getOrDefault("LK_LCOM_NORM", 0.0);
            sumLkHs += m.getOrDefault("LK_COHESION", 0.0);
        }
        int n = classes.size();
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下类图解析后的CK/LK度量结果，分析设计质量，并给出3条简洁的改进建议。\n");
        sb.append("类数量=").append(n).append("\n");
        sb.append("WMC(avg)=").append(formatDouble(sumWmc / n)).append("\n");
        sb.append("DIT(avg)=").append(formatDouble(sumDit / n)).append("\n");
        sb.append("NOC(avg)=").append(formatDouble(sumNoc / n)).append("\n");
        sb.append("CBO(avg)=").append(formatDouble(sumCbo / n)).append("\n");
        sb.append("RFC(avg)=").append(formatDouble(sumRfc / n)).append("\n");
        sb.append("LCOM(avg)=").append(formatDouble(sumLcom / n)).append("\n");
        sb.append("LCOM4(avg)=").append(formatDouble(sumLkNorm / n)).append("\n");
        sb.append("LCOM_HS(avg)=").append(formatDouble(sumLkHs / n)).append("\n\n");
        appendCommonAiRequirements(sb);
        return sb.toString();
    }

    private static String metricValue(Map<String, Double> metrics, String key) {
        Double v = metrics.get(key);
        return v == null ? "N/A" : formatDouble(v);
    }

    private static void appendCommonAiRequirements(StringBuilder sb) {
        sb.append("要求：\n");
        sb.append("1. 先给总体评价\n");
        sb.append("2. 再指出主要风险\n");
        sb.append("3. 最后给3条可执行建议\n");
        sb.append("4. 用中文输出\n");
    }

    private void showAiResultDialog(String title, String content) {
        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        pane.setContentType("text/html");
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        pane.setText(buildStyledAiHtml(content));
        pane.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(pane);
        scrollPane.setPreferredSize(new Dimension(640, 420));
        JOptionPane.showMessageDialog(this, scrollPane, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private static String buildStyledAiHtml(String raw) {
        String text = raw == null ? "" : raw.replace("\r\n", "\n").trim();
        String[] lines = text.split("\n");

        StringBuilder overall = new StringBuilder();
        StringBuilder risk = new StringBuilder();
        StringBuilder advice = new StringBuilder();
        StringBuilder other = new StringBuilder();

        int mode = 0; // 1 overall, 2 risk, 3 advice, 4 other
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            String low = t.toLowerCase(Locale.ROOT);
            if (containsAny(low, "总体评价", "整体评价", "综合评价")) {
                mode = 1;
                appendLine(overall, t);
                continue;
            }
            if (containsAny(low, "主要风险", "风险点", "风险")) {
                mode = 2;
                appendLine(risk, t);
                continue;
            }
            if (containsAny(low, "建议", "改进")) {
                mode = 3;
                appendLine(advice, t);
                continue;
            }

            if (mode == 1) {
                appendLine(overall, t);
            } else if (mode == 2) {
                appendLine(risk, t);
            } else if (mode == 3) {
                appendLine(advice, t);
            } else {
                mode = 4;
                appendLine(other, t);
            }
        }

        if (overall.length() == 0 && risk.length() == 0 && advice.length() == 0) {
            appendLine(other, text);
        }

        StringBuilder html = new StringBuilder();
        html.append(
                "<html><body style='font-family:Segoe UI,Microsoft YaHei UI,sans-serif;margin:10px 12px;color:#0f172a;'>");
        html.append("<div style='font-size:12px;color:#64748b;margin-bottom:10px;'>智能分析结果</div>");
        if (overall.length() > 0) {
            html.append(sectionHtml("总体评价", "#0369a1", "#e0f2fe", overall.toString()));
        }
        if (risk.length() > 0) {
            html.append(sectionHtml("主要风险", "#b91c1c", "#fee2e2", risk.toString()));
        }
        if (advice.length() > 0) {
            html.append(sectionHtml("改进建议", "#0f766e", "#ccfbf1", advice.toString()));
        }
        if (other.length() > 0) {
            html.append(sectionHtml("补充说明", "#5b21b6", "#ede9fe", other.toString()));
        }
        html.append("</body></html>");
        return html.toString();
    }

    private static String sectionHtml(String title, String fg, String bg, String content) {
        return "<div style='margin:0 0 12px 0;border:1px solid #e2e8f0;border-radius:10px;overflow:hidden;'>"
                + "<div style='padding:8px 10px;background:" + bg + ";color:" + fg
                + ";font-weight:700;font-size:13px;'>"
                + escapeHtml(title) + "</div>"
                + "<div style='padding:10px 12px;line-height:1.65;font-size:13px;color:#1e293b;white-space:pre-wrap;'>"
                + escapeHtml(content) + "</div>"
                + "</div>";
    }

    private static void appendLine(StringBuilder sb, String line) {
        if (sb.length() > 0) {
            sb.append('\n');
        }
        sb.append(line);
    }

    private static boolean containsAny(String text, String... keys) {
        for (String k : keys) {
            if (text.contains(k.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private double parseDouble(String value) {
        if (value == null) {
            return 0.0;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return 0.0;
        }
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

    private static final class UseCasePlantUmlParseResult {
        private final int simpleUseCases;
        private final int averageUseCases;
        private final int complexUseCases;
        private final int simpleActors;
        private final int averageActors;
        private final int complexActors;

        private UseCasePlantUmlParseResult(int simpleUseCases,
                int averageUseCases,
                int complexUseCases,
                int simpleActors,
                int averageActors,
                int complexActors) {
            this.simpleUseCases = simpleUseCases;
            this.averageUseCases = averageUseCases;
            this.complexUseCases = complexUseCases;
            this.simpleActors = simpleActors;
            this.averageActors = averageActors;
            this.complexActors = complexActors;
        }
    }
}
