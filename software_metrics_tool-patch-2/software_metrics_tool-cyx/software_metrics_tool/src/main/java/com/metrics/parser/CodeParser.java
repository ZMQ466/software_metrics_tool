package com.metrics.parser;

import com.metrics.model.ClassInfo;
import java.util.List;

/**
 * 代码解析器接口：负责将源代码转化为统一定义的数据结构
 * 这里可以对接 Eclipse ASTParser 或 JavaParser
 */
public interface CodeParser {
    
    /**
     * 解析单个源文件
     * @param filePath 源文件路径
     * @return 包含该文件内所有类信息的列表
     */
    List<ClassInfo> parseFile(String filePath);

    /**
     * 解析整个项目/文件夹
     * @param directoryPath 项目目录路径
     * @return 项目中所有类的集合
     */
    List<ClassInfo> parseDirectory(String directoryPath);
}
