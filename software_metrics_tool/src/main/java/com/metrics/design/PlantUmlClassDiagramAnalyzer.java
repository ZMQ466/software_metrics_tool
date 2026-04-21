package com.metrics.design;

import com.metrics.model.ClassInfo;
import com.metrics.model.MethodInfo;
import com.metrics.model.ProjectMetricsResult;
import com.metrics.modules.CKLkMetricsCalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse PlantUML class-diagram text and compute CK/LK metrics
 * using the existing CKLkMetricsCalculator.
 */
public final class PlantUmlClassDiagramAnalyzer {
    private static final Pattern CLASS_DECL = Pattern.compile(
            "^(?:abstract\\s+)?(?:class|interface|enum)\\s+(?:\"([^\"]+)\"|([\\w$.]+))(?:\\s+as\\s+([\\w$.]+))?(?:\\s+extends\\s+([\\w$.]+))?.*?(\\{)?\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static final List<String> REL_TOKENS = List.of(
            "<|--", "--|>", "<|..", "..|>",
            "*--", "--*", "o--", "--o",
            "-->", "<--", "..>", "<..",
            "--", ".."
    );

    private PlantUmlClassDiagramAnalyzer() {
    }

    public static ProjectMetricsResult analyze(String plantUmlText) {
        if (plantUmlText == null || plantUmlText.trim().isEmpty()) {
            throw new IllegalArgumentException("PlantUML text is empty.");
        }

        ParseState state = parsePlantUml(plantUmlText);
        applyMethodFieldHeuristic(state.classesById);

        ProjectMetricsResult project = new ProjectMetricsResult("plantuml-class-diagram");
        for (ClassInfo c : state.classesById.values()) {
            project.addClassInfo(c);
        }

        CKLkMetricsCalculator calculator = new CKLkMetricsCalculator();
        for (ClassInfo c : project.getClasses()) {
            calculator.calculate(c);
        }
        calculator.calculateProjectLevel(project);
        return project;
    }

    private static ParseState parsePlantUml(String plantUmlText) {
        Map<String, ClassInfo> classesById = new HashMap<>();
        Map<String, String> aliasToId = new HashMap<>();

        String[] lines = plantUmlText.replace("\r\n", "\n").split("\n");
        String currentClassId = null;
        int methodCounterInClass = 0;

        for (String rawLine : lines) {
            String line = cleanupLine(rawLine);
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("@startuml") || line.startsWith("@enduml")) {
                continue;
            }

            if (currentClassId == null) {
                Matcher m = CLASS_DECL.matcher(line);
                if (m.matches()) {
                    String quotedName = m.group(1);
                    String plainName = m.group(2);
                    String alias = m.group(3);
                    String extendsRaw = m.group(4);
                    boolean opensBlock = m.group(5) != null;

                    String displayName = quotedName != null ? quotedName : plainName;
                    String id = alias != null ? alias : displayName;
                    ClassInfo ci = getOrCreateClass(classesById, id, displayName);
                    aliasToId.put(id, id);
                    aliasToId.put(displayName, id);

                    if (extendsRaw != null && !extendsRaw.trim().isEmpty()) {
                        ci.setSuperClassName(resolveId(extendsRaw.trim(), aliasToId));
                    }

                    if (opensBlock) {
                        currentClassId = id;
                        methodCounterInClass = 0;
                    }
                    continue;
                }

                parseRelationLine(line, classesById, aliasToId);
            } else {
                if (line.startsWith("}")) {
                    currentClassId = null;
                    continue;
                }
                ClassInfo current = classesById.get(currentClassId);
                if (current == null) {
                    continue;
                }
                parseClassMemberLine(line, current, methodCounterInClass, aliasToId);
                if (looksLikeMethod(line)) {
                    methodCounterInClass++;
                }
            }
        }

        return new ParseState(classesById, aliasToId);
    }

    private static void parseClassMemberLine(String line, ClassInfo current, int methodIndex, Map<String, String> aliasToId) {
        String normalized = line.trim();
        if (normalized.isEmpty()) {
            return;
        }
        while (!normalized.isEmpty()
                && (normalized.charAt(0) == '+'
                || normalized.charAt(0) == '-'
                || normalized.charAt(0) == '#'
                || normalized.charAt(0) == '~')) {
            normalized = normalized.substring(1).trim();
        }
        normalized = normalized.replace("{static}", "").replace("{abstract}", "").trim();
        if (normalized.isEmpty()) {
            return;
        }

        if (looksLikeMethod(normalized)) {
            String methodName = extractMethodName(normalized, methodIndex);
            MethodInfo mi = new MethodInfo(methodName);
            mi.setCyclomaticComplexity(1);
            current.getMethods().add(mi);

            String returnType = extractReturnType(normalized);
            if (!returnType.isEmpty()) {
                maybeAddCoupling(current, returnType, aliasToId);
            }
            for (String t : extractParameterTypes(normalized)) {
                maybeAddCoupling(current, t, aliasToId);
            }
            return;
        }

        String fieldName = extractFieldName(normalized);
        if (!fieldName.isEmpty()) {
            current.getFields().add(fieldName);
        }
        String fieldType = extractFieldType(normalized);
        if (!fieldType.isEmpty()) {
            maybeAddCoupling(current, fieldType, aliasToId);
        }
    }

    private static void parseRelationLine(String line, Map<String, ClassInfo> classesById, Map<String, String> aliasToId) {
        String token = null;
        int index = -1;
        for (String t : REL_TOKENS) {
            int i = line.indexOf(t);
            if (i >= 0) {
                token = t;
                index = i;
                break;
            }
        }
        if (token == null) {
            return;
        }

        String leftRaw = line.substring(0, index).trim();
        String rightRaw = line.substring(index + token.length()).trim();
        int colonIdx = rightRaw.indexOf(':');
        if (colonIdx >= 0) {
            rightRaw = rightRaw.substring(0, colonIdx).trim();
        }

        String leftId = resolveRelationEndpointId(leftRaw, aliasToId);
        String rightId = resolveRelationEndpointId(rightRaw, aliasToId);
        if (leftId.isEmpty() || rightId.isEmpty()) {
            return;
        }

        ClassInfo left = getOrCreateClass(classesById, leftId, leftId);
        ClassInfo right = getOrCreateClass(classesById, rightId, rightId);

        switch (token) {
            case "<|--":
            case "<|..":
                right.setSuperClassName(left.getQualifiedName());
                right.getCalledClasses().add(left.getQualifiedName());
                break;
            case "--|>":
            case "..|>":
                left.setSuperClassName(right.getQualifiedName());
                left.getCalledClasses().add(right.getQualifiedName());
                break;
            case "-->":
            case "..>":
                left.getCalledClasses().add(right.getQualifiedName());
                break;
            case "<--":
            case "<..":
                right.getCalledClasses().add(left.getQualifiedName());
                break;
            default:
                left.getCalledClasses().add(right.getQualifiedName());
                right.getCalledClasses().add(left.getQualifiedName());
                break;
        }
    }

    private static void applyMethodFieldHeuristic(Map<String, ClassInfo> classesById) {
        for (ClassInfo ci : classesById.values()) {
            if (ci.getFields().isEmpty() || ci.getMethods().isEmpty()) {
                continue;
            }
            for (int i = 0; i < ci.getMethods().size(); i++) {
                MethodInfo m = ci.getMethods().get(i);
                String f = ci.getFields().get(i % ci.getFields().size());
                m.getAccessedFields().add(f);
            }
        }
    }

    private static ClassInfo getOrCreateClass(Map<String, ClassInfo> classesById, String id, String displayName) {
        ClassInfo existing = classesById.get(id);
        if (existing != null) {
            return existing;
        }
        String className = sanitizeClassName(displayName.isEmpty() ? id : displayName);
        ClassInfo ci = new ClassInfo(className);
        ci.setQualifiedName(id);
        classesById.put(id, ci);
        return ci;
    }

    private static String sanitizeClassName(String s) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            return "AnonymousClass";
        }
        return t.replaceAll("[^\\w$]", "_");
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

    private static boolean looksLikeMethod(String line) {
        int l = line.indexOf('(');
        int r = line.indexOf(')');
        return l > 0 && r > l;
    }

    private static String extractMethodName(String line, int fallbackIndex) {
        int l = line.indexOf('(');
        if (l <= 0) {
            return "method" + fallbackIndex;
        }
        String prefix = line.substring(0, l).trim();
        String[] tokens = prefix.split("\\s+");
        String candidate = tokens[tokens.length - 1].trim();
        candidate = candidate.replaceAll("[^\\w$]", "");
        if (candidate.isEmpty()) {
            return "method" + fallbackIndex;
        }
        return candidate;
    }

    private static String extractReturnType(String line) {
        int colon = line.lastIndexOf(':');
        if (colon < 0) {
            return "";
        }
        return normalizeTypeToken(line.substring(colon + 1).trim());
    }

    private static List<String> extractParameterTypes(String line) {
        List<String> types = new ArrayList<>();
        int l = line.indexOf('(');
        int r = line.indexOf(')', l + 1);
        if (l < 0 || r < 0 || r <= l + 1) {
            return types;
        }
        String inside = line.substring(l + 1, r).trim();
        if (inside.isEmpty()) {
            return types;
        }
        String[] parts = inside.split(",");
        for (String part : parts) {
            String p = part.trim();
            if (p.isEmpty()) {
                continue;
            }
            String[] tokens = p.split("\\s+");
            if (tokens.length == 1) {
                types.add(normalizeTypeToken(tokens[0]));
            } else {
                types.add(normalizeTypeToken(tokens[0]));
            }
        }
        return types;
    }

    private static String extractFieldName(String line) {
        String noInit = line;
        int eq = noInit.indexOf('=');
        if (eq >= 0) {
            noInit = noInit.substring(0, eq).trim();
        }

        if (noInit.contains(":")) {
            String left = noInit.substring(0, noInit.indexOf(':')).trim();
            String[] tokens = left.split("\\s+");
            String n = tokens[tokens.length - 1];
            return n.replaceAll("[^\\w$]", "");
        }

        String[] tokens = noInit.split("\\s+");
        if (tokens.length == 0) {
            return "";
        }
        String n = tokens[tokens.length - 1];
        return n.replaceAll("[^\\w$]", "");
    }

    private static String extractFieldType(String line) {
        String noInit = line;
        int eq = noInit.indexOf('=');
        if (eq >= 0) {
            noInit = noInit.substring(0, eq).trim();
        }
        if (noInit.contains(":")) {
            String right = noInit.substring(noInit.indexOf(':') + 1).trim();
            return normalizeTypeToken(right);
        }
        String[] tokens = noInit.split("\\s+");
        if (tokens.length <= 1) {
            return "";
        }
        return normalizeTypeToken(tokens[0]);
    }

    private static void maybeAddCoupling(ClassInfo current, String rawType, Map<String, String> aliasToId) {
        String t = normalizeTypeToken(rawType);
        if (t.isEmpty()) {
            return;
        }
        if (isPrimitiveLike(t)) {
            return;
        }
        String id = resolveId(t, aliasToId);
        if (!id.equals(current.getQualifiedName())) {
            current.getCalledClasses().add(id);
        }
    }

    private static boolean isPrimitiveLike(String t) {
        String x = t.toLowerCase(Locale.ROOT);
        Set<String> primitives = new HashSet<>(Set.of(
                "byte", "short", "int", "long", "float", "double", "boolean", "char",
                "string", "void", "object", "list", "map", "set"
        ));
        return primitives.contains(x);
    }

    private static String normalizeTypeToken(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        int lt = t.indexOf('<');
        if (lt >= 0) {
            t = t.substring(0, lt).trim();
        }
        t = t.replace("[]", "").replace("...", "");
        t = t.replaceAll("[^\\w$.]", "");
        return t;
    }

    private static String resolveId(String token, Map<String, String> aliasToId) {
        if (token == null || token.trim().isEmpty()) {
            return "";
        }
        String t = token.trim();
        return aliasToId.getOrDefault(t, t);
    }

    private static String resolveRelationEndpointId(String endpointRaw, Map<String, String> aliasToId) {
        String raw = endpointRaw.trim();
        if (raw.isEmpty()) {
            return "";
        }
        String[] tokens = raw.split("\\s+");
        for (int i = tokens.length - 1; i >= 0; i--) {
            String c = tokens[i].replace("\"", "").trim();
            c = c.replaceAll("[^\\w$.]", "");
            if (!c.isEmpty()) {
                return resolveId(c, aliasToId);
            }
        }
        return "";
    }

    private static class ParseState {
        private final Map<String, ClassInfo> classesById;
        @SuppressWarnings("unused")
        private final Map<String, String> aliasToId;

        private ParseState(Map<String, ClassInfo> classesById, Map<String, String> aliasToId) {
            this.classesById = classesById;
            this.aliasToId = aliasToId;
        }
    }
}
