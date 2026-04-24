package com.metrics.modules;

import com.metrics.core.MetricCalculator;
import com.metrics.model.ClassInfo;
import com.metrics.model.MethodInfo;
import com.metrics.model.ProjectMetricsResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TraditionalMetricsCalculator implements MetricCalculator {
    public static final int HIGH_COMPLEXITY_THRESHOLD = 10;

    @Override
    public String getMetricName() {
        return "Traditional";
    }

    @Override
    public void calculate(ClassInfo classInfo) {
        int totalLoc = 0;
        int totalCc = 0;
        int methodCount = 0;
        int maxCc = 1;
        int maxNesting = 0;
        int highComplexityMethods = 0;

        List<MethodInfo> methods = classInfo.getMethods();
        for (MethodInfo m : methods) {
            int loc = m.getLoc();
            if (loc < 0) {
                loc = 0;
            }
            totalLoc += loc;
            int cc = m.getCyclomaticComplexity();
            if (cc <= 0) {
                cc = 1;
            }
            totalCc += cc;
            if (cc > maxCc) {
                maxCc = cc;
            }
            if (cc >= HIGH_COMPLEXITY_THRESHOLD) {
                highComplexityMethods++;
            }
            if (m.getMaxNestingDepth() > maxNesting) {
                maxNesting = m.getMaxNestingDepth();
            }
            methodCount++;
        }

        classInfo.addMetric("LOC_CLASS", totalLoc);
        if (methodCount > 0) {
            classInfo.addMetric("CC_AVG_CLASS", (double) totalCc / methodCount);
            classInfo.addMetric("TRAD_METHOD_LOC_AVG_CLASS", (double) totalLoc / methodCount);
        } else {
            classInfo.addMetric("CC_AVG_CLASS", 0.0);
            classInfo.addMetric("TRAD_METHOD_LOC_AVG_CLASS", 0.0);
        }
        classInfo.addMetric("TRAD_CC_MAX_CLASS", maxCc);
        classInfo.addMetric("TRAD_MAX_NESTING_CLASS", maxNesting);
        classInfo.addMetric("TRAD_HIGH_CC_METHOD_COUNT_CLASS", highComplexityMethods);

        ClassRangeLineMetrics classLines = analyzeClassRange(classInfo);
        if (classLines != null) {
            classInfo.addMetric("TRAD_CLASS_LOC", classLines.totalLines);
            classInfo.addMetric("TRAD_CLASS_ELOC", classLines.effectiveLines);
            classInfo.addMetric("TRAD_CLASS_BLANK", classLines.blankLines);
            classInfo.addMetric("TRAD_CLASS_COMMENT", classLines.commentLines);
        }
    }

    @Override
    public void calculateProjectLevel(ProjectMetricsResult project) {
        int totalCc = 0;
        int totalMethods = 0;
        int totalMethodLoc = 0;
        int maxNesting = 0;
        int highComplexityMethods = 0;
        int maxCc = 1;

        for (ClassInfo c : project.getClasses()) {
            for (MethodInfo m : c.getMethods()) {
                totalMethodLoc += Math.max(m.getLoc(), 0);
                int cc = m.getCyclomaticComplexity();
                if (cc <= 0) {
                    cc = 1;
                }
                totalCc += cc;
                if (cc > maxCc) {
                    maxCc = cc;
                }
                if (cc >= HIGH_COMPLEXITY_THRESHOLD) {
                    highComplexityMethods++;
                }
                if (m.getMaxNestingDepth() > maxNesting) {
                    maxNesting = m.getMaxNestingDepth();
                }
                totalMethods++;
            }
        }

        LineMetrics lines = analyzeSourceTree(project.getProjectName());
        project.setTotalLoc(lines.totalLines);
        project.setTotalEffectiveLoc(lines.effectiveCodeLines);
        project.setTotalCommentLines(lines.commentLines);
        project.setTotalBlankLines(lines.blankLines);
        if (lines.totalLines > 0) {
            project.setCommentRate((double) lines.commentLines / (double) lines.totalLines);
        } else {
            project.setCommentRate(0.0);
        }

        if (totalMethods > 0) {
            project.setAvgCyclomaticComplexity((double) totalCc / totalMethods);
            project.setAvgMethodLoc((double) totalMethodLoc / totalMethods);
        } else {
            project.setAvgCyclomaticComplexity(0.0);
            project.setAvgMethodLoc(0.0);
        }
        project.setMaxMethodNestingDepth(maxNesting);
        project.setHighComplexityMethodCount(highComplexityMethods);
        project.setMaxCyclomaticComplexity(maxCc);
    }

    private static LineMetrics analyzeSourceTree(String sourcePath) {
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            return new LineMetrics(0, 0, 0, 0);
        }

        Path root = Paths.get(sourcePath);
        if (!Files.exists(root)) {
            return new LineMetrics(0, 0, 0, 0);
        }

        long total = 0;
        long effective = 0;
        long comments = 0;
        long blank = 0;
        try {
            try (var stream = Files.walk(root)) {
                for (Path p : (Iterable<Path>) stream
                        .filter(f -> Files.isRegularFile(f) && f.toString().endsWith(".java"))::iterator) {
                    LineMetrics m = analyzeJavaFile(p);
                    total += m.totalLines;
                    effective += m.effectiveCodeLines;
                    comments += m.commentLines;
                    blank += m.blankLines;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("遍历目录失败: " + sourcePath, e);
        }
        return new LineMetrics(total, effective, comments, blank);
    }

    private static LineMetrics analyzeJavaFile(Path file) {
        long total = 0;
        long effective = 0;
        long comments = 0;
        long blank = 0;
        boolean inBlockComment = false;

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                total++;
                if (line.trim().isEmpty()) {
                    blank++;
                }
                LineClassifyResult r = classifyLine(line, inBlockComment);
                inBlockComment = r.inBlockComment;
                if (r.hasCode) {
                    effective++;
                }
                if (r.hasComment) {
                    comments++;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + file, e);
        }

        return new LineMetrics(total, effective, comments, blank);
    }

    private static ClassRangeLineMetrics analyzeClassRange(ClassInfo c) {
        if (c == null) {
            return null;
        }
        String filePath = c.getSourceFilePath();
        int start = c.getStartLine();
        int end = c.getEndLine();
        if (filePath == null || filePath.trim().isEmpty() || start <= 0 || end <= 0 || end < start) {
            return null;
        }
        Path file = Paths.get(filePath);
        if (!Files.isRegularFile(file)) {
            return null;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }

        int fromIdx = Math.max(1, start);
        int toIdx = Math.min(lines.size(), end);
        if (fromIdx > toIdx) {
            return null;
        }

        long total = 0;
        long effective = 0;
        long comments = 0;
        long blank = 0;
        boolean inBlockComment = false;
        for (int lineNo = fromIdx; lineNo <= toIdx; lineNo++) {
            String line = lines.get(lineNo - 1);
            total++;
            if (line != null && line.trim().isEmpty()) {
                blank++;
            }
            LineClassifyResult r = classifyLine(line, inBlockComment);
            inBlockComment = r.inBlockComment;
            if (r.hasCode) {
                effective++;
            }
            if (r.hasComment) {
                comments++;
            }
        }
        return new ClassRangeLineMetrics(total, effective, comments, blank);
    }

    private static LineClassifyResult classifyLine(String line, boolean inBlockComment) {
        if (line == null) {
            return new LineClassifyResult(false, false, inBlockComment);
        }

        boolean hasCode = false;
        boolean hasComment = false;

        boolean localInBlockComment = inBlockComment;
        boolean inString = false;
        boolean inChar = false;
        boolean escape = false;

        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);

            if (localInBlockComment) {
                hasComment = true;
                if (c == '*' && i + 1 < line.length() && line.charAt(i + 1) == '/') {
                    localInBlockComment = false;
                    i += 2;
                    continue;
                }
                i++;
                continue;
            }

            if (inString) {
                hasCode = true;
                if (escape) {
                    escape = false;
                    i++;
                    continue;
                }
                if (c == '\\') {
                    escape = true;
                    i++;
                    continue;
                }
                if (c == '"') {
                    inString = false;
                }
                i++;
                continue;
            }

            if (inChar) {
                hasCode = true;
                if (escape) {
                    escape = false;
                    i++;
                    continue;
                }
                if (c == '\\') {
                    escape = true;
                    i++;
                    continue;
                }
                if (c == '\'') {
                    inChar = false;
                }
                i++;
                continue;
            }

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (c == '/' && i + 1 < line.length()) {
                char n = line.charAt(i + 1);
                if (n == '/') {
                    hasComment = true;
                    break;
                }
                if (n == '*') {
                    hasComment = true;
                    localInBlockComment = true;
                    i += 2;
                    continue;
                }
            }

            if (c == '"') {
                hasCode = true;
                inString = true;
                i++;
                continue;
            }

            if (c == '\'') {
                hasCode = true;
                inChar = true;
                i++;
                continue;
            }

            hasCode = true;
            i++;
        }

        if (!hasCode && !hasComment && line.trim().isEmpty()) {
            return new LineClassifyResult(false, false, localInBlockComment);
        }
        return new LineClassifyResult(hasCode, hasComment, localInBlockComment);
    }

    private static final class ClassRangeLineMetrics {
        private final long totalLines;
        private final long effectiveLines;
        private final long commentLines;
        private final long blankLines;

        private ClassRangeLineMetrics(long totalLines, long effectiveLines, long commentLines, long blankLines) {
            this.totalLines = totalLines;
            this.effectiveLines = effectiveLines;
            this.commentLines = commentLines;
            this.blankLines = blankLines;
        }
    }

    private static final class LineMetrics {
        private final long totalLines;
        private final long effectiveCodeLines;
        private final long commentLines;
        private final long blankLines;

        private LineMetrics(long totalLines, long effectiveCodeLines, long commentLines, long blankLines) {
            this.totalLines = totalLines;
            this.effectiveCodeLines = effectiveCodeLines;
            this.commentLines = commentLines;
            this.blankLines = blankLines;
        }
    }

    private static final class LineClassifyResult {
        private final boolean hasCode;
        private final boolean hasComment;
        private final boolean inBlockComment;

        private LineClassifyResult(boolean hasCode, boolean hasComment, boolean inBlockComment) {
            this.hasCode = hasCode;
            this.hasComment = hasComment;
            this.inBlockComment = inBlockComment;
        }
    }
}
