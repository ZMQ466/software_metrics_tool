package com.metrics.gui;

import com.metrics.core.MetricsManager;
import com.metrics.design.ControlFlowPlantUmlAnalyzer;
import com.metrics.design.DeepSeekClient;
import com.metrics.design.PlantUmlClassDiagramAnalyzer;
import com.metrics.design.RequirementDesignMetricsEngine;
import com.metrics.design.UseCasePlantUmlAnalyzer;
import com.metrics.design.UCPCalculator;
import com.metrics.design.UCPResult;
import com.metrics.model.ClassInfo;
import com.metrics.model.MethodInfo;
import com.metrics.model.ProjectMetricsResult;
import com.metrics.model.UCPInput;
import com.metrics.modules.CKLkMetricsCalculator;
import com.metrics.modules.OOMetricsInterpreter;
import com.metrics.modules.TraditionalMetricsCalculator;
import com.metrics.parser.EclipseJdtCodeParser;

import javax.swing.*;
import javax.swing.border.Border;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 自动化度量工具主界面：代码度量页支持按类浏览与 HTML 样式化展示。
 */
public class MainFrame extends JFrame {

    private static final Color PRIMARY = new Color(0x6ea8ff);
    private static final Color PRIMARY_DARK = new Color(0x3b82f6);
    private static final Color PRIMARY_SOFT = new Color(0xdbeafe);
    private static final Color PRIMARY_SOFT_2 = new Color(0xeaf2ff);
    private static final Color APP_BG = new Color(0xf5f9ff);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER = new Color(0xd6e4ff);
    private static final Color MUTED = new Color(0x64748b);
    private static final Color TEXT = new Color(0x16324f);
    private static final Color SHADOW = new Color(0x9fbbe8);
    private static final Color ON_PRIMARY = Color.WHITE;

    private static final Font FONT_BODY = new Font("Microsoft YaHei UI", Font.PLAIN, 14);
    private static final Font FONT_BODY_LG = new Font("Microsoft YaHei UI", Font.PLAIN, 15);
    private static final Font FONT_TITLE = new Font("Microsoft YaHei UI", Font.BOLD, 18);
    private static final Font FONT_TITLE_LG = new Font("Microsoft YaHei UI", Font.BOLD, 22);
    private static final Font FONT_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    private JTabbedPane tabbedPane;
    private JPanel navBar;
    private JButton[] navButtons;

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

    private List<ClassInfo> lastAnalyzedClasses = Collections.emptyList();

    private JPanel traditionalCodeMetricsPanel;
    private JTextField traditionalSourcePathField;
    private JButton traditionalBrowseButton;
    private JButton traditionalAnalyzeButton;
    private JButton traditionalAiAnalyzeButton;
    private JComboBox<String> traditionalClassSelectorCombo;
    private JEditorPane traditionalDetailPane;
    private JLabel traditionalSummaryLabel;
    private JLabel traditionalStatusLabel;
    private List<ClassInfo> lastTraditionalClasses = Collections.emptyList();

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
    private JButton uploadUseCasePlantUmlButton;
    private JButton parseUseCasePlantUmlButton;
    private JTextArea useCasePlantUmlArea;

    private JPanel classDiagramPanel;
    private JButton uploadPlantUmlButton;
    private JButton calculateClassDiagramButton;
    private JButton aiAnalyzeClassDiagramButton;
    private JTextArea plantUmlCodeArea;
    private JTextArea classDiagramResultArea;

    private JPanel controlFlowPanel;
    private JButton uploadControlFlowPlantUmlButton;
    private JButton calculateControlFlowButton;
    private JButton aiAnalyzeControlFlowButton;
    private JTextArea controlFlowPlantUmlArea;
    private JTextArea controlFlowResultArea;

    private final DeepSeekClient deepSeekClient = new DeepSeekClient();

    public MainFrame() {
        setTitle("软件度量自动化工具");
        setSize(1280, 960);
        setMinimumSize(new Dimension(1280, 960));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(APP_BG);

        initComponents();
    }

    private void initComponents() {
        installTabbedPaneChrome();

      //  navBar = buildTopNavigation();
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FONT_BODY);
        tabbedPane.setBackground(APP_BG);
        tabbedPane.setBorder(new EmptyBorder(12, 0, 0, 0));

        initCodeMetricsPanel();
        tabbedPane.addTab("面向对象度量", codeMetricsPanel);

        initTraditionalCodeMetricsPanel();
        tabbedPane.addTab("传统代码度量", traditionalCodeMetricsPanel);

        initDesignMetricsPanel();
        initClassDiagramPanel();
        initControlFlowPanel();
        tabbedPane.addTab("类图度量", classDiagramPanel);
        tabbedPane.addTab("控制流图度量", controlFlowPanel);
        tabbedPane.addTab("用例点与功能点度量", designMetricsPanel);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(APP_BG);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));
     //   root.add(navBar, BorderLayout.NORTH);
        root.add(tabbedPane, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);

        tabbedPane.addChangeListener(e -> syncNavSelection(tabbedPane.getSelectedIndex()));
        syncNavSelection(0);
    }

    private void installTabbedPaneChrome() {
        UIManager.put("TabbedPane.background", APP_BG);
        UIManager.put("TabbedPane.unselectedBackground", PRIMARY_SOFT_2);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TabbedPane.contentAreaColor", APP_BG);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.selectedForeground", PRIMARY_DARK);
        UIManager.put("TabbedPane.focus", new Color(0, 0, 0, 0));
        Font tabFont = UIManager.getFont("TabbedPane.font");
        if (tabFont == null) {
            tabFont = UIManager.getFont("Label.font");
        }
        if (tabFont != null) {
            UIManager.put("TabbedPane.font", FONT_BODY_LG);
        }
    }

    private JPanel buildTopNavigation() {
        JPanel hero = createCardPanel(new BorderLayout(12, 8), BORDER, PRIMARY);
        hero.setBorder(BorderFactory.createCompoundBorder(createShadowBorder(18), hero.getBorder()));

        // JPanel titleBox = new JPanel();
        // titleBox.setOpaque(false);
        // titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        // JLabel title = new JLabel("软件度量工具");
        // title.setFont(FONT_TITLE_LG);
        // title.setForeground(TEXT);
        // JLabel subtitle = new JLabel("Material Design 3 · 浅蓝配色 · 圆角卡片导航");
        // subtitle.setFont(FONT_BODY_LG);
        // subtitle.setForeground(MUTED);
        // titleBox.add(title);
        // titleBox.add(Box.createVerticalStrut(3));
        // titleBox.add(subtitle);
        // hero.add(titleBox, BorderLayout.WEST);

        JPanel navRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        navRow.setOpaque(false);
        navButtons = new JButton[] {
                navPill("面向对象度量", 0),
                navPill("传统代码度量", 1),
                navPill("类图度量", 2),
                navPill("控制流图度量", 3),
                navPill("用例点与功能点度量", 4)
        };
        for (JButton button : navButtons) {
            navRow.add(button);
        }
        hero.add(navRow, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(hero, BorderLayout.CENTER);
        return wrapper;
    }

    private JButton navPill(String text, int index) {
        JButton button = styledButton(text, false);
        button.setFont(FONT_BODY_LG);
        button.setBorder(BorderFactory.createCompoundBorder(
                createShadowBorder(12),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(10, 16, 10, 16))));
        button.addActionListener(e -> tabbedPane.setSelectedIndex(index));
        button.putClientProperty("navIndex", index);
        return button;
    }

    private void syncNavSelection(int selectedIndex) {
        if (navButtons == null) {
            return;
        }
        for (int i = 0; i < navButtons.length; i++) {
            JButton button = navButtons[i];
            boolean selected = i == selectedIndex;
            button.setBackground(selected ? PRIMARY_DARK : CARD_BG);
            button.setForeground(selected ? ON_PRIMARY : TEXT);
            button.setBorder(BorderFactory.createCompoundBorder(
                    createShadowBorder(selected ? 18 : 10),
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(selected ? PRIMARY_DARK : BORDER),
                            new EmptyBorder(10, 16, 10, 16))));
        }
    }

    private void initCodeMetricsPanel() {
        codeMetricsPanel = new JPanel(new BorderLayout(16, 16));
        codeMetricsPanel.setBorder(new EmptyBorder(16, 4, 4, 4));
        codeMetricsPanel.setBackground(APP_BG);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);

        JPanel pathRow = createCardPanel(new BorderLayout(12, 0), BORDER, PRIMARY);
        pathRow.setOpaque(false);
        JLabel pathLbl = new JLabel("源码目录");
        pathLbl.setForeground(MUTED);
        pathLbl.setFont(FONT_BODY_LG);
        pathRow.add(pathLbl, BorderLayout.WEST);

        sourcePathField = new JTextField();
        styleTextField(sourcePathField);
        pathRow.add(sourcePathField, BorderLayout.CENTER);

        JPanel pathActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
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

        JPanel summaryRow = createCardPanel(new BorderLayout(), BORDER, PRIMARY);
        summaryRow.setOpaque(false);
        projectSummaryLabel = new JLabel(defaultSummaryHtml());
        projectSummaryLabel.setOpaque(true);
        projectSummaryLabel.setBackground(CARD_BG);
        projectSummaryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        projectSummaryLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 4, PRIMARY),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 3, PRIMARY_SOFT),
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(BORDER),
                                new EmptyBorder(16, 20, 16, 20)))));
        summaryRow.add(projectSummaryLabel, BorderLayout.CENTER);
        north.add(summaryRow);

        north.add(Box.createVerticalStrut(8));
        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setOpaque(false);
        statusLabel = new JLabel("<html><div style='text-align:center;color:#64748b'>请选择源码目录后点击「开始分析」</div></html>");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(FONT_BODY);
        statusRow.add(statusLabel, BorderLayout.CENTER);
        north.add(statusRow);

        codeMetricsPanel.add(north, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        JPanel selectorCard = createCardPanel(new BorderLayout(8, 0), BORDER, PRIMARY);
        JLabel selLbl = new JLabel("查看类");
        selLbl.setForeground(TEXT);
        selLbl.setFont(FONT_BODY);
        selectorCard.add(selLbl, BorderLayout.WEST);

        classSelectorCombo = new JComboBox<>();
        classSelectorCombo.setFont(FONT_BODY_LG);
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
        classDetailPane.setFont(FONT_BODY);
        classDetailPane.setBorder(new EmptyBorder(4, 4, 4, 4));
        classDetailPane.setText(emptyDetailHtml("尚未执行分析", "选择源码目录并点击「开始分析」后，在此查看单个类的度量与解释。"));
        JScrollPane detailScroll = new JScrollPane(classDetailPane);
        detailScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(3, 0, 0, 0, PRIMARY),
                BorderFactory.createLineBorder(BORDER)));
        detailScroll.getViewport().setBackground(Color.WHITE);
        center.add(detailScroll, BorderLayout.CENTER);

        codeMetricsPanel.add(center, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 8));
        bottomPanel.setBackground(PRIMARY_SOFT_2);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_SOFT),
                new EmptyBorder(8, 14, 8, 14)));
        ckMetricsCheckBox = new JCheckBox("CK 度量集", true);
        lkMetricsCheckBox = new JCheckBox("LK 度量集", true);
        for (JCheckBox cb : new JCheckBox[] { ckMetricsCheckBox, lkMetricsCheckBox }) {
            cb.setOpaque(false);
            cb.setForeground(new Color(0x4338ca));
        }
        bottomPanel.add(ckMetricsCheckBox);
        bottomPanel.add(lkMetricsCheckBox);
        codeMetricsPanel.add(bottomPanel, BorderLayout.SOUTH);

        ActionListener metricVisibilityListener = e -> {
            if (!lastAnalyzedClasses.isEmpty()) {
                refreshSelectedClassDetail();
            }
        };
        ckMetricsCheckBox.addActionListener(metricVisibilityListener);
        lkMetricsCheckBox.addActionListener(metricVisibilityListener);
    }

    private void initTraditionalCodeMetricsPanel() {
        traditionalCodeMetricsPanel = new JPanel(new BorderLayout(16, 16));
        traditionalCodeMetricsPanel.setBorder(new EmptyBorder(16, 4, 4, 4));
        traditionalCodeMetricsPanel.setBackground(APP_BG);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setOpaque(false);

        JPanel pathRow = createCardPanel(new BorderLayout(12, 0), BORDER, PRIMARY);
        pathRow.setOpaque(false);
        JLabel pathLbl = new JLabel("源码目录");
        pathLbl.setForeground(MUTED);
        pathLbl.setFont(FONT_BODY_LG);
        pathRow.add(pathLbl, BorderLayout.WEST);

        traditionalSourcePathField = new JTextField();
        styleTextField(traditionalSourcePathField);
        pathRow.add(traditionalSourcePathField, BorderLayout.CENTER);

        JPanel pathActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pathActions.setOpaque(false);
        traditionalBrowseButton = styledButton("浏览…", false);
        traditionalBrowseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                traditionalSourcePathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        traditionalAnalyzeButton = styledButton("开始分析", true);
        traditionalAnalyzeButton.addActionListener(e -> performTraditionalCodeAnalysis());
        traditionalAiAnalyzeButton = styledButton("智能分析", false);
        traditionalAiAnalyzeButton.addActionListener(e -> performAiTraditionalCodeAnalysis());
        pathActions.add(traditionalBrowseButton);
        pathActions.add(traditionalAnalyzeButton);
        pathActions.add(traditionalAiAnalyzeButton);
        pathRow.add(pathActions, BorderLayout.EAST);
        north.add(pathRow);
        north.add(Box.createVerticalStrut(10));

        JPanel summaryRow = createCardPanel(new BorderLayout(), BORDER, PRIMARY);
        summaryRow.setOpaque(false);
        traditionalSummaryLabel = new JLabel(defaultTraditionalSummaryHtml());
        traditionalSummaryLabel.setOpaque(true);
        traditionalSummaryLabel.setBackground(CARD_BG);
        traditionalSummaryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        traditionalSummaryLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 4, PRIMARY),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 3, PRIMARY_SOFT),
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(BORDER),
                                new EmptyBorder(16, 20, 16, 20)))));
        summaryRow.add(traditionalSummaryLabel, BorderLayout.CENTER);
        north.add(summaryRow);

        north.add(Box.createVerticalStrut(8));
        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setOpaque(false);
        traditionalStatusLabel = new JLabel(
                "<html><div style='text-align:center;color:#64748b'>请选择源码目录后点击「开始分析」</div></html>");
        traditionalStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        traditionalStatusLabel.setFont(FONT_BODY);
        statusRow.add(traditionalStatusLabel, BorderLayout.CENTER);
        north.add(statusRow);

        traditionalCodeMetricsPanel.add(north, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        JPanel selectorCard = createCardPanel(new BorderLayout(8, 0), BORDER, PRIMARY);
        JLabel selLbl = new JLabel("查看类");
        selLbl.setForeground(TEXT);
        selLbl.setFont(FONT_BODY);
        selectorCard.add(selLbl, BorderLayout.WEST);

        traditionalClassSelectorCombo = new JComboBox<>();
        traditionalClassSelectorCombo.setFont(FONT_BODY_LG);
        traditionalClassSelectorCombo.setEnabled(false);
        traditionalClassSelectorCombo.addActionListener(e -> {
            if (traditionalClassSelectorCombo.getSelectedIndex() >= 0) {
                refreshSelectedTraditionalClassDetail();
            }
        });
        selectorCard.add(traditionalClassSelectorCombo, BorderLayout.CENTER);
        center.add(selectorCard, BorderLayout.NORTH);

        traditionalDetailPane = new JEditorPane();
        traditionalDetailPane.setEditable(false);
        traditionalDetailPane.setContentType("text/html");
        traditionalDetailPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        traditionalDetailPane.setFont(FONT_BODY);
        traditionalDetailPane.setBorder(new EmptyBorder(4, 4, 4, 4));
        traditionalDetailPane.setText(emptyDetailHtml("尚未执行分析", "选择源码目录并点击「开始分析」后，在此查看单个类与方法的传统代码度量。"));
        JScrollPane detailScroll = new JScrollPane(traditionalDetailPane);
        detailScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(3, 0, 0, 0, PRIMARY),
                BorderFactory.createLineBorder(BORDER)));
        detailScroll.getViewport().setBackground(Color.WHITE);
        center.add(detailScroll, BorderLayout.CENTER);

        traditionalCodeMetricsPanel.add(center, BorderLayout.CENTER);
    }

    private static JButton styledButton(String text, boolean primary) {
        JButton b = new JButton(text);
        b.setFont(FONT_BODY_LG);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setMargin(new Insets(10, 18, 10, 18));
        if (primary) {
            b.setBackground(PRIMARY);
            b.setForeground(ON_PRIMARY);
        } else {
            b.setBackground(CARD_BG);
            b.setForeground(TEXT);
        }
        b.setBorder(BorderFactory.createCompoundBorder(
                createShadowBorder(primary ? 20 : 12),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(primary ? PRIMARY_DARK : BORDER),
                        new EmptyBorder(10, 18, 10, 18))));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(primary ? PRIMARY_DARK : PRIMARY_SOFT_2);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(primary ? PRIMARY : CARD_BG);
            }
        });
        return b;
    }

    private void initDesignMetricsPanel() {
        designMetricsPanel = new JPanel(new BorderLayout(16, 16));
        designMetricsPanel.setBorder(new EmptyBorder(16, 4, 4, 4));
        designMetricsPanel.setBackground(APP_BG);

        // 使用 GridBagLayout 实现真正的 3列布局，每列占权重 1.0
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(CARD_BG);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                createShadowBorder(18),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 3, PRIMARY),
                        new EmptyBorder(16, 18, 16, 18))));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0 / 3.0;

        int row = 0;

        JLabel designTitle = new JLabel("用例点与功能点度量");
        designTitle.setFont(FONT_TITLE_LG);
        designTitle.setForeground(TEXT);
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        inputPanel.add(designTitle, gbc);
        gbc.gridwidth = 1;
        row++;

        // ========== 第1行：EI, EO, EQ ==========
        // 列0: EI
        gbc.gridx = 0;
        gbc.gridy = row;
        fpEiField = new JTextField("0");
        addLabeledFieldRow(inputPanel, gbc, "EI", fpEiField);
        // 列1: EO
        gbc.gridx = 1;
        gbc.gridy = row;
        fpEoField = new JTextField("0");
        addLabeledFieldRow(inputPanel, gbc, "EO", fpEoField);
        // 列2: EQ
        gbc.gridx = 2;
        gbc.gridy = row;
        fpEqField = new JTextField("0");
        addLabeledFieldRow(inputPanel, gbc, "EQ", fpEqField);
        row++;

        // ========== 第2行：ILF, EIF, GSC总分 ==========
        // 列0: ILF
        gbc.gridx = 0;
        gbc.gridy = row;
        fpIlfField = new JTextField("0");
        addLabeledFieldRow(inputPanel, gbc, "ILF", fpIlfField);
        // 列1: EIF
        gbc.gridx = 1;
        gbc.gridy = row;
        fpEifField = new JTextField("0");
        addLabeledFieldRow(inputPanel, gbc, "EIF", fpEifField);
        // 列2: GSC总分
        gbc.gridx = 2;
        gbc.gridy = row;
        fpGscField = new JTextField("0");
        addLabeledFieldRow(inputPanel, gbc, "GSC总分", fpGscField);
        row++;

        // ========== 第3行：简单用例, 平均用例, 复杂用例 ==========
        // 列0: 简单用例
        gbc.gridx = 0;
        gbc.gridy = row;
        simpleUseCasesField = new JTextField("0");
        addLabeledFieldRow(inputPanel, gbc, "简单用例", simpleUseCasesField);
        // 列1: 平均用例
        gbc.gridx = 1;
        gbc.gridy = row;
        averageUseCasesField = new JTextField("0");
        addLabeledFieldRow(inputPanel, gbc, "平均用例", averageUseCasesField);
        // 列2: 复杂用例
        gbc.gridx = 2;
        gbc.gridy = row;
        complexUseCasesField = new JTextField("0");
        addLabeledFieldRow(inputPanel, gbc, "复杂用例", complexUseCasesField);
        row++;

        // ========== 第4行：简单参与者, 平均参与者, 复杂参与者 ==========
        // 列0: 简单参与者
        gbc.gridx = 0;
        gbc.gridy = row;
        simpleActorsField = new JTextField("0");
        addLabeledFieldRow(inputPanel, gbc, "简单参与者", simpleActorsField);
        // 列1: 平均参与者
        gbc.gridx = 1;
        gbc.gridy = row;
        averageActorsField = new JTextField("0");
        addLabeledFieldRow(inputPanel, gbc, "平均参与者", averageActorsField);
        // 列2: 复杂参与者
        gbc.gridx = 2;
        gbc.gridy = row;
        complexActorsField = new JTextField("0");
        addLabeledFieldRow(inputPanel, gbc, "复杂参与者", complexActorsField);
        row++;

        // ========== 第5行：特征点权重 ==========
        gbc.gridx = 0;
        gbc.gridy = row;
        algorithmicWeightField = new JTextField("1.00");
        addLabeledFieldRow(inputPanel, gbc, "特征点权重", algorithmicWeightField);
        // 列1、2留空
        row++;

        // ========== 第6行：技术因子（独占一行，跨6列） ==========
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 6;
        gbc.weightx = 1.0;
        JPanel techPanel = new JPanel(new BorderLayout(8, 0));
        techPanel.setOpaque(false);
        JLabel techLabel = new JLabel("技术因子 (13项, 0-5):");
        techLabel.setFont(FONT_BODY_LG);
        techLabel.setPreferredSize(new Dimension(150, 30));
        techPanel.add(techLabel, BorderLayout.WEST);
        technicalFactorsField = new JTextField("0,0,0,0,0,0,0,0,0,0,0,0,0");
        styleTextField(technicalFactorsField);
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
        envLabel.setFont(FONT_BODY_LG);
        envLabel.setPreferredSize(new Dimension(150, 30));
        envPanel.add(envLabel, BorderLayout.WEST);
        environmentalFactorsField = new JTextField("0,0,0,0,0,0,0,0");
        styleTextField(environmentalFactorsField);
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
        useCasePlantLabel.setFont(FONT_BODY_LG);
        useCasePlantPanel.add(useCasePlantLabel, BorderLayout.NORTH);

        useCasePlantUmlArea = new JTextArea(5, 20);
        styleTextArea(useCasePlantUmlArea, true);
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
        useCasePlantScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        useCasePlantPanel.add(useCasePlantScroll, BorderLayout.CENTER);

        uploadUseCasePlantUmlButton = styledButton("上传 PlantUML 代码", false);
        uploadUseCasePlantUmlButton.addActionListener(e -> performUploadUseCasePlantUml());
        parseUseCasePlantUmlButton = styledButton("解析PlantUML用例图", false);
        parseUseCasePlantUmlButton.addActionListener(e -> performParseUseCasePlantUml());
        JPanel parseButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        parseButtonPanel.setOpaque(false);
        parseButtonPanel.add(uploadUseCasePlantUmlButton);
        parseButtonPanel.add(Box.createHorizontalStrut(8));
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
        calculateUcpButton = floatingActionButton("计算 FP / UCP / 特征点", PRIMARY_DARK, ON_PRIMARY);
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
        styleTextArea(designResultArea, true);
        designResultArea.setText("点击 \"计算 FP / UCP / 特征点\" 以获得三类需求/设计度量结果\n");
        designResultArea.setBorder(new EmptyBorder(14, 16, 14, 16));
        JScrollPane scrollPane = new JScrollPane(designResultArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                createShadowBorder(16),
                BorderFactory.createLineBorder(BORDER)));
        designMetricsPanel.add(scrollPane, BorderLayout.CENTER);
    }

    // 辅助方法：只添加标签，不添加文本框
    private void addLabeledFieldCompactGrid(JPanel panel, GridBagConstraints gbc,
            String labelText, JTextField field) {
        JLabel label = new JLabel(labelText);
        label.setFont(FONT_BODY);
        label.setForeground(TEXT);
        styleTextField(field);
        panel.add(label, gbc);
    }

    // 辅助方法：在 3 列布局中添加标签和输入框的组合
    private void addLabeledFieldRow(JPanel panel, GridBagConstraints gbc,
            String labelText, JTextField field) {
        JPanel cellPanel = new JPanel(new BorderLayout(4, 0));
        cellPanel.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(FONT_BODY);
        label.setForeground(TEXT);
        label.setPreferredSize(new Dimension(60, 26));
        cellPanel.add(label, BorderLayout.WEST);

        styleTextField(field);
        cellPanel.add(field, BorderLayout.CENTER);

        panel.add(cellPanel, gbc);
    }

    private void initClassDiagramPanel() {
        classDiagramPanel = new JPanel(new BorderLayout(16, 16));
        classDiagramPanel.setBorder(new EmptyBorder(16, 4, 4, 4));
        classDiagramPanel.setBackground(APP_BG);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);
        uploadPlantUmlButton = styledButton("上传 PlantUML 代码", false);
        uploadPlantUmlButton.addActionListener(e -> performUploadPlantUml());
        calculateClassDiagramButton = floatingActionButton("解析 + CK/LK 度量分析", PRIMARY, ON_PRIMARY);
        calculateClassDiagramButton.addActionListener(e -> performClassDiagramCalculation());
        aiAnalyzeClassDiagramButton = styledButton("智能分析", false);
        aiAnalyzeClassDiagramButton.addActionListener(e -> performAiClassDiagramAnalysis());
        actionPanel.add(uploadPlantUmlButton);
        actionPanel.add(calculateClassDiagramButton);
        actionPanel.add(aiAnalyzeClassDiagramButton);
        JPanel headerCard = createMaterialCard(new BorderLayout());
        JLabel title = new JLabel("类图度量分析");
        title.setFont(FONT_TITLE_LG);
        title.setForeground(TEXT);
        headerCard.add(title, BorderLayout.WEST);
        headerCard.add(actionPanel, BorderLayout.EAST);
        classDiagramPanel.add(headerCard, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        centerPanel.setOpaque(false);

        plantUmlCodeArea = new JTextArea();
        styleTextArea(plantUmlCodeArea, true);
        plantUmlCodeArea.setBorder(new EmptyBorder(14, 16, 14, 16));
        plantUmlCodeArea
                .setText("@startuml\nclass User {\n  +id: Long\n  +name: String\n  +getName(): String\n}\n@enduml\n");
        JScrollPane inputScroll = new JScrollPane(plantUmlCodeArea);
        inputScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER), "PlantUML Class Diagram"),
                new EmptyBorder(4, 4, 4, 4)));
        centerPanel.add(inputScroll);

        classDiagramResultArea = new JTextArea();
        classDiagramResultArea.setEditable(false);
        styleTextArea(classDiagramResultArea, true);
        classDiagramResultArea.setBorder(new EmptyBorder(14, 16, 14, 16));
        classDiagramResultArea.setText("上传 PlantUML 代码，点击 解析 + CK/LK 分析 以获得结果.\n");
        JScrollPane resultScroll = new JScrollPane(classDiagramResultArea);
        resultScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER), "CK/LK Metrics Result"),
                new EmptyBorder(4, 4, 4, 4)));
        centerPanel.add(resultScroll);

        classDiagramPanel.add(centerPanel, BorderLayout.CENTER);
    }

    private void initControlFlowPanel() {
        controlFlowPanel = new JPanel(new BorderLayout(16, 16));
        controlFlowPanel.setBorder(new EmptyBorder(16, 4, 4, 4));
        controlFlowPanel.setBackground(APP_BG);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionPanel.setOpaque(false);
        uploadControlFlowPlantUmlButton = styledButton("上传 PlantUML", false);
        uploadControlFlowPlantUmlButton.addActionListener(e -> performUploadControlFlowPlantUml());
        calculateControlFlowButton = floatingActionButton("解析 + 控制流度量分析", PRIMARY, ON_PRIMARY);
        calculateControlFlowButton.addActionListener(e -> performControlFlowCalculation());
        aiAnalyzeControlFlowButton = styledButton("智能分析", false);
        aiAnalyzeControlFlowButton.addActionListener(e -> performAiControlFlowAnalysis());
        actionPanel.add(uploadControlFlowPlantUmlButton);
        actionPanel.add(calculateControlFlowButton);
        actionPanel.add(aiAnalyzeControlFlowButton);
        JPanel headerCard = createMaterialCard(new BorderLayout());
        JLabel title = new JLabel("控制流图度量分析");
        title.setFont(FONT_TITLE_LG);
        title.setForeground(TEXT);
        headerCard.add(title, BorderLayout.WEST);
        headerCard.add(actionPanel, BorderLayout.EAST);
        controlFlowPanel.add(headerCard, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        centerPanel.setOpaque(false);

        controlFlowPlantUmlArea = new JTextArea();
        styleTextArea(controlFlowPlantUmlArea, true);
        controlFlowPlantUmlArea.setBorder(new EmptyBorder(14, 16, 14, 16));
        controlFlowPlantUmlArea.setText(
                "@startuml\nstart\nif (x > 0?) then (yes)\n  :work;\nelse (no)\n  :skip;\nendif\nstop\n@enduml\n");
        JScrollPane inputScroll = new JScrollPane(controlFlowPlantUmlArea);
        inputScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER), "PlantUML Flowchart"),
                new EmptyBorder(4, 4, 4, 4)));
        centerPanel.add(inputScroll);

        controlFlowResultArea = new JTextArea();
        controlFlowResultArea.setEditable(false);
        styleTextArea(controlFlowResultArea, true);
        controlFlowResultArea.setBorder(new EmptyBorder(14, 16, 14, 16));
        controlFlowResultArea.setText("上传 PlantUML 代码，点击“解析 + 控制流度量分析”以获得结果\n");
        JScrollPane resultScroll = new JScrollPane(controlFlowResultArea);
        resultScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(BORDER),
                        "Control-Flow Metrics Result"),
                new EmptyBorder(4, 4, 4, 4)));
        centerPanel.add(resultScroll);

        controlFlowPanel.add(centerPanel, BorderLayout.CENTER);
    }

    private static void addLabeledField(JPanel grid, String label, JTextField field) {
        JLabel l = new JLabel(label);
        l.setForeground(MUTED);
        l.setFont(FONT_BODY);
        grid.add(l);
        styleTextField(field);
        grid.add(field);
    }

    private static void addLabeledFieldCompact(JPanel grid, String label, JTextField field) {
        JLabel l = new JLabel(label);
        l.setForeground(MUTED);
        l.setFont(FONT_BODY.deriveFont(Font.PLAIN, 12f));
        grid.add(l);
        styleTextField(field);
        grid.add(field);
    }

    private static JPanel createCardPanel(LayoutManager layout, Color borderColor, Color accentStripe) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 3, accentStripe),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borderColor),
                        new EmptyBorder(10, 12, 10, 12))));
        return panel;
    }

    private static JPanel createMaterialCard(LayoutManager layout) {
        JPanel panel = new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 28, 28);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        panel.setOpaque(false);
        panel.setBackground(CARD_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
                createShadowBorder(16),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER),
                        new EmptyBorder(14, 16, 14, 16))));
        return panel;
    }

    private static Border createShadowBorder(int alpha) {
        return new javax.swing.border.AbstractBorder() {
            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(0, 0, 6, 0);
            }

            @Override
            public Insets getBorderInsets(Component c, Insets insets) {
                insets.top = 0;
                insets.left = 0;
                insets.bottom = 6;
                insets.right = 0;
                return insets;
            }

            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(SHADOW.getRed(), SHADOW.getGreen(), SHADOW.getBlue(), Math.max(36, alpha)));
                g2.fillRoundRect(x + 4, y + 4, width - 8, height - 8, 24, 24);
                g2.dispose();
            }
        };
    }

    private static JButton floatingActionButton(String text, Color background, Color foreground) {
        JButton button = styledButton(text, true);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFont(FONT_BODY_LG);
        button.setBorder(BorderFactory.createCompoundBorder(
                createShadowBorder(20),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(background.brighter());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(background);
            }
        });
        return button;
    }

    private static void styleTextField(JTextField field) {
        field.setFont(FONT_BODY);
        field.setForeground(TEXT);
        field.setBackground(Color.WHITE);
        field.setCaretColor(TEXT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(6, 10, 6, 10)));
    }

    private static void styleTextArea(JTextArea textArea, boolean monospaced) {
        textArea.setFont(monospaced ? FONT_MONO : FONT_BODY);
        textArea.setForeground(TEXT);
        textArea.setBackground(Color.WHITE);
        textArea.setCaretColor(TEXT);
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

        projectSummaryLabel.setText(summaryHtmlOo(sorted.size()));

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
                false));
        classDetailPane.setCaretPosition(0);
    }

    private void performTraditionalCodeAnalysis() {
        String path = traditionalSourcePathField.getText();
        if (path == null || path.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择源码目录", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        traditionalClassSelectorCombo.setEnabled(false);
        traditionalStatusLabel.setForeground(MUTED);
        traditionalStatusLabel.setText("<html><div style='text-align:center;color:#0e7490'>正在解析与度量…</div></html>");
        traditionalSummaryLabel.setText(summaryHtmlLoading());
        traditionalDetailPane.setText(emptyDetailHtml("正在分析", escapeHtml(path)));

        MetricsManager manager = new MetricsManager();
        manager.setParser(new EclipseJdtCodeParser());
        manager.registerCalculator(new TraditionalMetricsCalculator());

        try {
            ProjectMetricsResult result = manager.runAnalysis(path);
            applyTraditionalCodeAnalysisResult(result);
        } catch (Exception ex) {
            lastTraditionalClasses = Collections.emptyList();
            traditionalClassSelectorCombo.removeAllItems();
            traditionalClassSelectorCombo.setEnabled(false);
            traditionalStatusLabel.setForeground(new Color(0xb91c1c));
            traditionalStatusLabel
                    .setText("<html><div style='text-align:center;color:#b91c1c'>分析失败，请检查路径或依赖后重试</div></html>");
            traditionalSummaryLabel.setText(summaryHtmlError());
            traditionalDetailPane.setText(
                    emptyDetailHtml("分析失败", escapeHtml(ex.getMessage() != null ? ex.getMessage() : ex.toString())));
        }
    }

    private void applyTraditionalCodeAnalysisResult(ProjectMetricsResult result) {
        List<ClassInfo> sorted = new ArrayList<>(result.getClasses());
        sorted.sort(Comparator.comparing(ClassInfo::getQualifiedName, String.CASE_INSENSITIVE_ORDER));
        lastTraditionalClasses = sorted;

        traditionalSummaryLabel.setText(summaryHtmlTraditional(
                String.valueOf((long) result.getTotalLoc()),
                String.valueOf((long) result.getTotalEffectiveLoc()),
                String.valueOf((long) result.getTotalBlankLines()),
                String.valueOf((long) result.getTotalCommentLines()),
                formatPercent(result.getCommentRate()),
                formatDouble(result.getAvgCyclomaticComplexity()),
                formatDouble(result.getAvgMethodLoc()),
                sorted.size()));

        traditionalStatusLabel.setForeground(new Color(0x047857));
        traditionalStatusLabel.setText("<html><div style='text-align:center;color:#047857'>分析完成 · 共 <b>" + sorted.size()
                + "</b> 个类，请在下拉框中选择要查看的类</div></html>");

        traditionalClassSelectorCombo.removeAllItems();
        for (ClassInfo c : sorted) {
            traditionalClassSelectorCombo.addItem(c.getQualifiedName());
        }
        traditionalClassSelectorCombo.setEnabled(!sorted.isEmpty());
        if (!sorted.isEmpty()) {
            traditionalClassSelectorCombo.setSelectedIndex(0);
        } else {
            traditionalDetailPane.setText(emptyDetailHtml("无类", "未在目录中解析到任何 Java 类。"));
        }
        refreshSelectedTraditionalClassDetail();
    }

    private void refreshSelectedTraditionalClassDetail() {
        int idx = traditionalClassSelectorCombo.getSelectedIndex();
        if (idx < 0 || idx >= lastTraditionalClasses.size()) {
            return;
        }
        ClassInfo c = lastTraditionalClasses.get(idx);
        traditionalDetailPane.setText(buildTraditionalClassDetailHtml(c));
        traditionalDetailPane.setCaretPosition(0);
    }

    private static String defaultSummaryHtml() {
        return "<html><body style='margin:0'>"
                + "<div style='text-align:center;width:100%;font-family:Segoe UI,Microsoft YaHei UI,sans-serif;color:#475569'>"
                + "<div style='font-size:13px;color:#64748b;letter-spacing:0.08em;font-weight:700;margin-bottom:10px'>项目概览</div>"
                + "<div style='font-size:13px;line-height:1.65;color:#64748b;margin-bottom:12px'>"
                + "尚未执行分析。选择源码目录并点击「开始分析」后，将在此展示 CK/LK 度量分析结果。"
                + "</div>"
                + "<div style='font-size:12px;line-height:2.2;padding:4px 0'>"
                + chipStat("类数量", "—", "#0891b2", "#cffafe")
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

    private static String summaryHtmlOo(int classCount) {
        return "<html><body style='margin:0'>"
                + "<div style='text-align:center;width:100%;font-family:Segoe UI,Microsoft YaHei UI,sans-serif;color:#334155'>"
                + "<div style='font-size:13px;color:#64748b;letter-spacing:0.08em;font-weight:700;margin-bottom:12px'>项目概览</div>"
                + "<div style='font-size:12px;line-height:2.2;padding:4px 0'>"
                + chipStat("类数量", String.valueOf(classCount), "#0891b2", "#cffafe")
                + "</div>"
                + "</div></body></html>";
    }

    private static String defaultTraditionalSummaryHtml() {
        return "<html><body style='margin:0'>"
                + "<div style='text-align:center;width:100%;font-family:Segoe UI,Microsoft YaHei UI,sans-serif;color:#475569'>"
                + "<div style='font-size:13px;color:#64748b;letter-spacing:0.08em;font-weight:700;margin-bottom:10px'>项目概览</div>"
                + "<div style='font-size:13px;line-height:1.65;color:#64748b;margin-bottom:12px'>"
                + "尚未执行分析。选择源码目录并点击「开始分析」后，将在此展示传统代码度量统计结果。"
                + "</div>"
                + "<div style='font-size:12px;line-height:2.2;padding:4px 0'>"
                + "<div style='margin:6px 0'>"
                + chipStatCompact("代码行数 LoC", "—", "#0369a1", "#e0f2fe")
                + chipStatCompact("有效代码行 ELoC", "—", "#0f766e", "#ccfbf1")
                + chipStatCompact("空行 Blank", "—", "#64748b", "#f1f5f9")
                + chipStatCompact("注释行 Comment", "—", "#be123c", "#ffe4e6")
                + "</div>"
                + "<div style='margin:6px 0'>"
                + chipStatCompact("注释率 Comment%", "—", "#9f1239", "#fff1f2")
                + chipStatCompact("平均圈复杂度 Avg CC", "—", "#5b21b6", "#ede9fe")
                + chipStatCompact("平均方法长度 Avg LOC", "—", "#0e7490", "#cffafe")
                + chipStatCompact("类数量 Classes", "—", "#0891b2", "#cffafe")
                + "</div>"
                + "</div>"
                + "</div></body></html>";
    }

    private static String summaryHtmlTraditional(String totalLoc, String effectiveLoc, String blankLines,
            String commentLines,
            String commentRate, String avgCc, String avgMethodLoc, int classCount) {
        return "<html><body style='margin:0'>"
                + "<div style='text-align:center;width:100%;font-family:Segoe UI,Microsoft YaHei UI,sans-serif;color:#334155'>"
                + "<div style='font-size:13px;color:#64748b;letter-spacing:0.08em;font-weight:700;margin-bottom:12px'>项目概览</div>"
                + "<div style='font-size:12px;line-height:2.2;padding:4px 0'>"
                + "<div style='margin:6px 0'>"
                + chipStatCompact("代码行数 LoC", totalLoc, "#0369a1", "#e0f2fe")
                + chipStatCompact("有效代码行 ELoC", effectiveLoc, "#0f766e", "#ccfbf1")
                + chipStatCompact("空行 Blank", blankLines, "#64748b", "#f1f5f9")
                + chipStatCompact("注释行 Comment", commentLines, "#be123c", "#ffe4e6")
                + "</div>"
                + "<div style='margin:6px 0'>"
                + chipStatCompact("注释率 Comment%", commentRate, "#9f1239", "#fff1f2")
                + chipStatCompact("平均圈复杂度 Avg CC", avgCc, "#5b21b6", "#ede9fe")
                + chipStatCompact("平均方法长度 Avg LOC", avgMethodLoc, "#0e7490", "#cffafe")
                + chipStatCompact("类数量 Classes", String.valueOf(classCount), "#0891b2", "#cffafe")
                + "</div>"
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

    private static String chipStatCompact(String label, String value, String fg, String bg) {
        return "<span style='display:inline-block;background:" + bg + ";color:" + fg
                + ";padding:7px 14px;border-radius:999px;font-size:12px;font-weight:700;"
                + "margin:6px 14px;vertical-align:middle'>"
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

        if (showTrad) {
            List<MethodInfo> high = new ArrayList<>();
            for (MethodInfo m : c.getMethods()) {
                if (m.getCyclomaticComplexity() >= TraditionalMetricsCalculator.HIGH_COMPLEXITY_THRESHOLD) {
                    high.add(m);
                }
            }
            sb.append("<p class='sec'>高复杂度方法告警</p>");
            if (high.isEmpty()) {
                sb.append("<p class='muted'>未发现高复杂度方法（阈值：CC ≥ ")
                        .append(TraditionalMetricsCalculator.HIGH_COMPLEXITY_THRESHOLD)
                        .append("）。</p>");
            } else {
                sb.append("<p class='muted'>阈值：CC ≥ ")
                        .append(TraditionalMetricsCalculator.HIGH_COMPLEXITY_THRESHOLD)
                        .append("。建议优先拆分/提炼以下方法。</p>");
                sb.append("<table class='metrics'>");
                for (MethodInfo m : high) {
                    sb.append("<tr><td class='k'>").append(escapeHtml(m.getMethodName())).append("</td><td class='v'>")
                            .append(escapeHtml(formatDouble(m.getCyclomaticComplexity())))
                            .append("</td></tr>");
                }
                sb.append("</table>");
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
        if (key.startsWith("TRAD_")) {
            return showTrad;
        }
        if ("LOC_CLASS".equals(key) || "CC_AVG_CLASS".equals(key)) {
            return showTrad;
        }
        return true;
    }

    private static String formatDouble(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }

    private static String formatPercent(double v) {
        return String.format(Locale.ROOT, "%.2f%%", v * 100.0);
    }

    private static String buildTraditionalClassDetailHtml(ClassInfo c) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset='UTF-8'>");
        sb.append(detailStyles());
        sb.append("</head><body>");

        sb.append("<h2>").append(escapeHtml(c.getClassName())).append("</h2>");
        sb.append("<p class='sub'>").append(escapeHtml(c.getQualifiedName())).append("</p>");

        sb.append("<p class='sec'>指标说明</p>");
        sb.append("<p class='muted'>")
                .append("CC：Cyclomatic Complexity. LOC：Lines of Code. Nest：Nesting Depth. Params：Parameter Count.")
                .append("</p>");

        sb.append("<p class='sec'>类级指标</p>");
        sb.append("<table class='metrics'>");
        appendMetricRow(sb, "TRAD_CLASS_LOC", c);
        appendMetricRow(sb, "TRAD_CLASS_ELOC", c);
        appendMetricRow(sb, "TRAD_CLASS_BLANK", c);
        appendMetricRow(sb, "TRAD_CLASS_COMMENT", c);
        appendMetricRow(sb, "CC_AVG_CLASS", c);
        appendMetricRow(sb, "TRAD_METHOD_LOC_AVG_CLASS", c);
        appendMetricRow(sb, "TRAD_CC_MAX_CLASS", c);
        appendMetricRow(sb, "TRAD_MAX_NESTING_CLASS", c);
        appendMetricRow(sb, "TRAD_HIGH_CC_METHOD_COUNT_CLASS", c);
        sb.append("</table>");

        sb.append("<p class='sec'>方法级指标</p>");
        if (c.getMethods().isEmpty()) {
            sb.append("<p class='muted'>未解析到方法。</p>");
        } else {
            sb.append("<table class='metrics'>");
            sb.append("<tr>")
                    .append("<td class='k'>方法 / Method</td>")
                    .append("<td class='v'>长度 LOC · 复杂度 CC · 嵌套 Nest · 参数 Params</td>")
                    .append("</tr>");
            for (MethodInfo m : c.getMethods()) {
                sb.append("<tr><td class='k'>").append(escapeHtml(m.getMethodName())).append("</td><td class='v'>")
                        .append(escapeHtml(String.valueOf(Math.max(m.getLoc(), 0)))).append(" · ")
                        .append(escapeHtml(String.valueOf(Math.max(m.getCyclomaticComplexity(), 1)))).append(" · ")
                        .append(escapeHtml(String.valueOf(Math.max(m.getMaxNestingDepth(), 0)))).append(" · ")
                        .append(escapeHtml(String.valueOf(m.getParameters() != null ? m.getParameters().size() : 0)))
                        .append("</td></tr>");
            }
            sb.append("</table>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private static void appendMetricRow(StringBuilder sb, String key, ClassInfo c) {
        if (!c.getMetrics().containsKey(key)) {
            return;
        }
        sb.append("<tr><td class='k'>").append(escapeHtml(traditionalMetricTitleZh(key)))
                .append("<div style='margin-top:2px;color:#64748b;font-size:11px;font-family:Segoe UI,Microsoft YaHei UI,sans-serif'>")
                .append(escapeHtml(traditionalMetricExplainZh(key)))
                .append("</div>")
                .append("</td><td class='v'>")
                .append(escapeHtml(formatDouble(c.getMetrics().get(key))))
                .append("</td></tr>");
    }

    private static String traditionalMetricTitleZh(String key) {
        return switch (key) {
            case "LOC_CLASS" -> "类代码行数 LoC / Class LoC";
            case "TRAD_CLASS_LOC" -> "类代码行数 LoC / Class LoC";
            case "TRAD_CLASS_ELOC" -> "类有效代码行 ELoC / Class ELoC";
            case "TRAD_CLASS_BLANK" -> "类空行 Blank / Class Blank";
            case "TRAD_CLASS_COMMENT" -> "类注释行 Comment / Class Comment";
            case "CC_AVG_CLASS" -> "类平均圈复杂度 CC / Avg CC";
            case "TRAD_METHOD_LOC_AVG_CLASS" -> "类平均方法长度 LOC / Avg Method LOC";
            case "TRAD_CC_MAX_CLASS" -> "类最大圈复杂度 CC / Max CC";
            case "TRAD_MAX_NESTING_CLASS" -> "类最大嵌套深度 / Max Nesting";
            case "TRAD_HIGH_CC_METHOD_COUNT_CLASS" -> "高复杂度方法数 / High-CC Methods";
            default -> key;
        };
    }

    private static String traditionalMetricExplainZh(String key) {
        return switch (key) {
            case "LOC_CLASS" -> "方法 LOC 合计 / Sum of method LOC";
            case "TRAD_CLASS_LOC" -> "按类范围统计 / Counted by class range";
            case "TRAD_CLASS_ELOC" -> "类范围内有效代码 / Effective code in class range";
            case "TRAD_CLASS_BLANK" -> "类范围内空行 / Blank lines in class range";
            case "TRAD_CLASS_COMMENT" -> "类范围内注释 / Comment lines in class range";
            case "CC_AVG_CLASS" -> "方法 CC 平均 / Avg method CC";
            case "TRAD_METHOD_LOC_AVG_CLASS" -> "方法 LOC 平均 / Avg method LOC";
            case "TRAD_CC_MAX_CLASS" -> "方法 CC 最大 / Max method CC";
            case "TRAD_MAX_NESTING_CLASS" -> "方法 Nest 最大 / Max method nesting";
            case "TRAD_HIGH_CC_METHOD_COUNT_CLASS" -> "CC 达阈值的方法数 / Methods above threshold";
            default -> "";
        };
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
            UseCasePlantUmlAnalyzer.ParseResult parsed = UseCasePlantUmlAnalyzer.analyze(plantUml);

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

    private void performUploadControlFlowPlantUml() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select PlantUML file");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path path = chooser.getSelectedFile().toPath();
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            controlFlowPlantUmlArea.setText(text);
            controlFlowPlantUmlArea.setCaretPosition(0);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to read file: " + ex.getMessage(),
                    "Read Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performUploadUseCasePlantUml() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select PlantUML file");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path path = chooser.getSelectedFile().toPath();
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            useCasePlantUmlArea.setText(text);
            useCasePlantUmlArea.setCaretPosition(0);
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

    private void performControlFlowCalculation() {
        controlFlowResultArea.setText("");
        String plantUml = controlFlowPlantUmlArea.getText();
        if (plantUml == null || plantUml.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please paste PlantUML flowchart text first.",
                    "Input Required",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            ControlFlowPlantUmlAnalyzer.Result result = ControlFlowPlantUmlAnalyzer.analyze(plantUml);
            controlFlowResultArea.append("=== Control-Flow Metrics ===\n");
            controlFlowResultArea.append("CyclomaticComplexity=" + result.cyclomaticComplexity + "\n");
            controlFlowResultArea.append("BranchCount=" + result.branchCount + "\n");
            controlFlowResultArea.append("LoopCount=" + result.loopCount + "\n");
            controlFlowResultArea.append("RiskLevel=" + result.riskLevel + "\n");
            controlFlowResultArea.setCaretPosition(0);
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

    private void performAiTraditionalCodeAnalysis() {
        int idx = traditionalClassSelectorCombo.getSelectedIndex();
        if (idx < 0 || idx >= lastTraditionalClasses.size()) {
            JOptionPane.showMessageDialog(this,
                    "请先执行传统代码度量分析并选择一个类。",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        ClassInfo c = lastTraditionalClasses.get(idx);
        String prompt = buildTraditionalCodePagePrompt(c);
        runAiAnalysisAsync(prompt, traditionalAiAnalyzeButton, reply -> showAiResultDialog("传统代码度量智能分析", reply));
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

    private void performAiControlFlowAnalysis() {
        String plantUml = controlFlowPlantUmlArea.getText();
        if (plantUml == null || plantUml.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "请先上传或粘贴 PlantUML 控制流图代码。",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            ControlFlowPlantUmlAnalyzer.Result result = ControlFlowPlantUmlAnalyzer.analyze(plantUml);
            String prompt = buildControlFlowPagePrompt(result);
            runAiAnalysisAsync(prompt, aiAnalyzeControlFlowButton, reply -> showAiResultDialog("控制流图度量智能分析", reply));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "控制流图分析失败: " + ex.getMessage(),
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
        sb.append("NOA=").append(metricValue(m, "LK_NOA")).append("\n");
        sb.append("NOO=").append(metricValue(m, "LK_NOO")).append("\n");
        sb.append("CS=").append(metricValue(m, "LK_CS")).append("\n\n");
        appendCommonAiRequirements(sb);
        return sb.toString();
    }

    private String buildTraditionalCodePagePrompt(ClassInfo c) {
        Map<String, Double> m = c.getMetrics();
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下传统代码度量输出指标，分析该类代码质量，并给出3条简洁的改进建议。\n");
        sb.append("类名: ").append(c.getQualifiedName()).append("\n");
        sb.append("LOC=").append(metricValue(m, "TRAD_CLASS_LOC")).append("\n");
        sb.append("ELoC=").append(metricValue(m, "TRAD_CLASS_ELOC")).append("\n");
        sb.append("BlankLines=").append(metricValue(m, "TRAD_CLASS_BLANK")).append("\n");
        sb.append("CommentLines=").append(metricValue(m, "TRAD_CLASS_COMMENT")).append("\n");
        sb.append("AvgCC=").append(metricValue(m, "CC_AVG_CLASS")).append("\n");
        sb.append("AvgMethodLOC=").append(metricValue(m, "TRAD_METHOD_LOC_AVG_CLASS")).append("\n");
        sb.append("MaxCC=").append(metricValue(m, "TRAD_CC_MAX_CLASS")).append("\n");
        sb.append("MaxNesting=").append(metricValue(m, "TRAD_MAX_NESTING_CLASS")).append("\n");
        sb.append("HighCCMethodCount=").append(metricValue(m, "TRAD_HIGH_CC_METHOD_COUNT_CLASS")).append("\n\n");
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
        double sumNoa = 0.0;
        double sumNoo = 0.0;
        double sumCs = 0.0;
        for (ClassInfo c : classes) {
            Map<String, Double> m = c.getMetrics();
            sumWmc += m.getOrDefault("CK_WMC", 0.0);
            sumDit += m.getOrDefault("CK_DIT", 0.0);
            sumNoc += m.getOrDefault("CK_NOC", 0.0);
            sumCbo += m.getOrDefault("CK_CBO", 0.0);
            sumRfc += m.getOrDefault("CK_RFC", 0.0);
            sumLcom += m.getOrDefault("CK_LCOM", 0.0);
            sumNoa += m.getOrDefault("LK_NOA", 0.0);
            sumNoo += m.getOrDefault("LK_NOO", 0.0);
            sumCs += m.getOrDefault("LK_CS", 0.0);
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
        sb.append("NOA(avg)=").append(formatDouble(sumNoa / n)).append("\n");
        sb.append("NOO(avg)=").append(formatDouble(sumNoo / n)).append("\n");
        sb.append("CS(avg)=").append(formatDouble(sumCs / n)).append("\n\n");
        appendCommonAiRequirements(sb);
        return sb.toString();
    }

    private String buildControlFlowPagePrompt(ControlFlowPlantUmlAnalyzer.Result result) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下控制流图度量结果，分析程序流程设计质量，并给出3条简洁的改进建议。\n");
        sb.append("CyclomaticComplexity=").append(result.cyclomaticComplexity).append("\n");
        sb.append("BranchCount=").append(result.branchCount).append("\n");
        sb.append("LoopCount=").append(result.loopCount).append("\n");
        sb.append("RiskLevel=").append(result.riskLevel).append("\n\n");
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
}
