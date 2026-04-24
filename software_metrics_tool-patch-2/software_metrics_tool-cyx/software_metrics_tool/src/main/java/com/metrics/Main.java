package com.metrics;

import com.metrics.core.MetricsManager;
import com.metrics.gui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * 项目主入口
 */
public class Main {
    public static void main(String[] args) {
        // 设置跨平台UI风格
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 初始化整体控制流 (供1号同学统筹)
        MetricsManager manager = new MetricsManager();
        // manager.setParser(new YourEclipseASTParserImpl()); 
        // manager.registerCalculator(new CKCalculator()); 
        // manager.registerCalculator(new LocCalculator());
        
        // 启动主界面
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}
