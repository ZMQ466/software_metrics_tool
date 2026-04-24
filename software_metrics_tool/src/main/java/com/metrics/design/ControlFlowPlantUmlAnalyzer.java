package com.metrics.design;

import java.util.Locale;

/**
 * Heuristic parser for PlantUML control-flow/activity diagrams.
 * It extracts branch/loop counts and estimates cyclomatic complexity.
 */
public final class ControlFlowPlantUmlAnalyzer {

    private ControlFlowPlantUmlAnalyzer() {
    }

    public static Result analyze(String plantUmlText) {
        if (plantUmlText == null || plantUmlText.trim().isEmpty()) {
            throw new IllegalArgumentException("PlantUML text is empty.");
        }

        String[] lines = plantUmlText.replace("\r\n", "\n").split("\n");
        int branches = 0;
        int loops = 0;

        for (String raw : lines) {
            String line = cleanupLine(raw);
            if (line.isEmpty()) {
                continue;
            }
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("@startuml") || lower.startsWith("@enduml")) {
                continue;
            }

            if (isLoopLine(lower)) {
                loops++;
            }
            branches += branchContribution(lower);
        }

        int cyclomaticComplexity = Math.max(1, 1 + branches + loops);
        String riskLevel = classifyRisk(cyclomaticComplexity);
        return new Result(cyclomaticComplexity, branches, loops, riskLevel);
    }

    private static String cleanupLine(String raw) {
        if (raw == null) {
            return "";
        }
        String line = raw.trim();
        int tick = line.indexOf('\'');
        if (tick >= 0) {
            line = line.substring(0, tick).trim();
        }
        int slashes = line.indexOf("//");
        if (slashes >= 0) {
            line = line.substring(0, slashes).trim();
        }
        return line;
    }

    private static boolean isLoopLine(String lower) {
        return lower.startsWith("while ")
                || lower.startsWith("while(")
                || lower.startsWith("repeat")
                || lower.contains("repeat while")
                || lower.contains("backward");
    }

    private static int branchContribution(String lower) {
        int count = 0;
        if (lower.startsWith("if ") || lower.startsWith("if(")) {
            count++;
        }
        if (lower.startsWith("elseif ") || lower.startsWith("elseif(")
                || lower.startsWith("else if ") || lower.startsWith("else if(")) {
            count++;
        }
        if (lower.startsWith("switch ") || lower.startsWith("switch(")) {
            count++;
        }
        if (lower.startsWith("case ") || lower.startsWith("case(")) {
            count++;
        }
        return count;
    }

    private static String classifyRisk(int cyclomaticComplexity) {
        if (cyclomaticComplexity <= 10) {
            return "Low";
        }
        if (cyclomaticComplexity <= 20) {
            return "Medium";
        }
        if (cyclomaticComplexity <= 30) {
            return "High";
        }
        return "Very High";
    }

    public static final class Result {
        public final int cyclomaticComplexity;
        public final int branchCount;
        public final int loopCount;
        public final String riskLevel;

        public Result(int cyclomaticComplexity, int branchCount, int loopCount, String riskLevel) {
            this.cyclomaticComplexity = cyclomaticComplexity;
            this.branchCount = branchCount;
            this.loopCount = loopCount;
            this.riskLevel = riskLevel;
        }
    }
}
