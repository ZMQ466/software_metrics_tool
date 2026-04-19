package com.metrics.gui;

import com.metrics.core.MetricsManager;
import com.metrics.design.ClassDiagramMetricsCalculator;
import com.metrics.design.ClassDiagramMetricsResult;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private JPanel classDiagramPanel;
    private JButton calculateClassDiagramButton;
    private JTextArea classDiagramResultArea;
    private JTextField classCountField;
    private JTextField avgMethodCountField;
    private JTextField avgAttributeCountField;
    private JTextField inheritanceDepthField;
    private JTextField subclassCountField;
    private JTextField relationCountField;

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
        pathActions.add(browseButton);
        pathActions.add(analyzeCodeButton);
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
        for (JCheckBox cb : new JCheckBox[]{ckMetricsCheckBox, lkMetricsCheckBox, traditionalMetricsCheckBox}) {
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
        designMetricsPanel.setBorder(new EmptyBorder(12, 14, 14, 14));
        designMetricsPanel.setBackground(PANEL_BG);

        JPanel inputPanel = new JPanel(new GridLayout(16, 2, 8, 8));
        inputPanel.setBackground(new Color(0xfffbeb));
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 3, new Color(0xfbbf24)),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xfde68a)),
                        new EmptyBorder(12, 12, 12, 12))));

        addLabeledField(inputPanel, "EI 外部输入", fpEiField = new JTextField("0"));
        addLabeledField(inputPanel, "EO 外部输出", fpEoField = new JTextField("0"));
        addLabeledField(inputPanel, "EQ 外部查询", fpEqField = new JTextField("0"));
        addLabeledField(inputPanel, "ILF 内部逻辑文件", fpIlfField = new JTextField("0"));
        addLabeledField(inputPanel, "EIF 外部接口文件", fpEifField = new JTextField("0"));
        addLabeledField(inputPanel, "GSC 总分 (0-70)", fpGscField = new JTextField("0"));

        addLabeledField(inputPanel, "简单用例数 (Simple)", simpleUseCasesField = new JTextField("0"));
        addLabeledField(inputPanel, "平均用例数 (Average)", averageUseCasesField = new JTextField("0"));
        addLabeledField(inputPanel, "复杂用例数 (Complex)", complexUseCasesField = new JTextField("0"));
        addLabeledField(inputPanel, "简单参与者 (Simple)", simpleActorsField = new JTextField("0"));
        addLabeledField(inputPanel, "平均参与者 (Average)", averageActorsField = new JTextField("0"));
        addLabeledField(inputPanel, "复杂参与者 (Complex)", complexActorsField = new JTextField("0"));
        addLabeledField(inputPanel, "特征点算法权重 (>0)", algorithmicWeightField = new JTextField("1.00"));
        addLabeledField(inputPanel, "技术因子 (13 项, 0–5)", technicalFactorsField = new JTextField("0,0,0,0,0,0,0,0,0,0,0,0,0"));
        addLabeledField(inputPanel, "环境因子 (8 项, 0–5)", environmentalFactorsField = new JTextField("0,0,0,0,0,0,0,0"));

        JPanel ucpRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        ucpRow.setOpaque(false);
        calculateUcpButton = styledButton("计算 FP / UCP / 特征点", true);
        calculateUcpButton.addActionListener(e -> performDesignMetricsCalculation());
        ucpRow.add(calculateUcpButton);
        inputPanel.add(new JLabel(""));
        inputPanel.add(ucpRow);

        designMetricsPanel.add(inputPanel, BorderLayout.NORTH);

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

    private void initClassDiagramPanel() {
        classDiagramPanel = new JPanel(new BorderLayout(10, 10));
        classDiagramPanel.setBorder(new EmptyBorder(12, 14, 14, 14));
        classDiagramPanel.setBackground(PANEL_BG);

        JPanel inputPanel = new JPanel(new GridLayout(7, 2, 8, 8));
        inputPanel.setBackground(new Color(0xf0fdf4));
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 3, new Color(0x22c55e)),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xbbf7d0)),
                        new EmptyBorder(12, 12, 12, 12))));

        addLabeledField(inputPanel, "类数量", classCountField = new JTextField("0"));
        addLabeledField(inputPanel, "平均方法数", avgMethodCountField = new JTextField("0"));
        addLabeledField(inputPanel, "平均属性数", avgAttributeCountField = new JTextField("0"));
        addLabeledField(inputPanel, "继承层级", inheritanceDepthField = new JTextField("0"));
        addLabeledField(inputPanel, "子类数", subclassCountField = new JTextField("0"));
        addLabeledField(inputPanel, "类间关系数", relationCountField = new JTextField("0"));

        JPanel classRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        classRow.setOpaque(false);
        calculateClassDiagramButton = styledButton("计算类图度量", true);
        calculateClassDiagramButton.addActionListener(e -> performClassDiagramCalculation());
        classRow.add(calculateClassDiagramButton);
        inputPanel.add(new JLabel(""));
        inputPanel.add(classRow);

        classDiagramPanel.add(inputPanel, BorderLayout.NORTH);

        classDiagramResultArea = new JTextArea();
        classDiagramResultArea.setEditable(false);
        classDiagramResultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        classDiagramResultArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        classDiagramResultArea.setBackground(Color.WHITE);
        classDiagramResultArea.setText("点击 \"计算类图度量\" 以获得 WMC/DIT/NOC/CBO/LCOM\n");
        JScrollPane scrollPane = new JScrollPane(classDiagramResultArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0xe2e8f0)));
        classDiagramPanel.add(scrollPane, BorderLayout.CENTER);
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
            classDetailPane.setText(emptyDetailHtml("分析失败", escapeHtml(ex.getMessage() != null ? ex.getMessage() : ex.toString())));
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
            RequirementDesignMetricsEngine.FunctionPointInput fpInput =
                    new RequirementDesignMetricsEngine.FunctionPointInput(
                            parseInt(fpEiField.getText()),
                            parseInt(fpEoField.getText()),
                            parseInt(fpEqField.getText()),
                            parseInt(fpIlfField.getText()),
                            parseInt(fpEifField.getText()),
                            parseInt(fpGscField.getText()));
            RequirementDesignMetricsEngine.FunctionPointResult fpResult =
                    RequirementDesignMetricsEngine.calculateFunctionPoint(fpInput);

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

            RequirementDesignMetricsEngine.FeaturePointInput featurePointInput =
                    new RequirementDesignMetricsEngine.FeaturePointInput(parseDouble(algorithmicWeightField.getText()));
            RequirementDesignMetricsEngine.FeaturePointResult featurePointResult =
                    RequirementDesignMetricsEngine.calculateFeaturePoint(fpResult, featurePointInput);

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

    private void performClassDiagramCalculation() {
        classDiagramResultArea.setText("");

        int classCount = parseInt(classCountField.getText());
        double avgMethodCount = parseDouble(avgMethodCountField.getText());
        double avgAttributeCount = parseDouble(avgAttributeCountField.getText());
        int inheritanceDepth = parseInt(inheritanceDepthField.getText());
        int subclassCount = parseInt(subclassCountField.getText());
        int relationCount = parseInt(relationCountField.getText());

        ClassDiagramMetricsCalculator calculator = new ClassDiagramMetricsCalculator();
        ClassDiagramMetricsResult result = calculator.calculate(
                classCount,
                avgMethodCount,
                avgAttributeCount,
                inheritanceDepth,
                subclassCount,
                relationCount);

        classDiagramResultArea.append("WMC=" + formatDouble(result.getWmc()) + "\n");
        classDiagramResultArea.append("DIT=" + formatDouble(result.getDit()) + "\n");
        classDiagramResultArea.append("NOC=" + formatDouble(result.getNoc()) + "\n");
        classDiagramResultArea.append("CBO=" + formatDouble(result.getCbo()) + "\n");
        classDiagramResultArea.append("LCOM=" + formatDouble(result.getLcom()) + "\n");
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
}
