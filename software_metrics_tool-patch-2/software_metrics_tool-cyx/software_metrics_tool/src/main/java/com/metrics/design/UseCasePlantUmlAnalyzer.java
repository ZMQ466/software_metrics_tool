package com.metrics.design;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Collections;

/**
 * Analyze PlantUML use-case diagrams and infer UCP category counts.
 */
public final class UseCasePlantUmlAnalyzer {

  private static final Pattern ACTOR_DECLARE_PATTERN = Pattern.compile(
      "(?i)^\\s*actor\\s+(?:\\\"([^\\\"]+)\\\"\\s+as\\s+)?(\\\"[^\\\"]+\\\"|:[^:]+:|[A-Za-z_][A-Za-z0-9_]*)?.*$");
  private static final Pattern USECASE_DECLARE_PATTERN = Pattern.compile(
      "(?i)^\\s*usecase\\s+(?:\\\"([^\\\"]+)\\\"\\s+as\\s+)?(\\\"[^\\\"]+\\\"|[A-Za-z_][A-Za-z0-9_]*)?.*$");

  private static final Pattern FORWARD_LINK_PATTERN = Pattern.compile(
      "(\\\"[^\\\"]+\\\"|:[^:]+:|[A-Za-z_][A-Za-z0-9_]*)\\s*[-.o*]+(?:left|right|up|down)?[-.o*]*>\\s*(\\([^)]+\\)|\\\"[^\\\"]+\\\"|:[^:]+:|[A-Za-z_][A-Za-z0-9_]*)",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern BACKWARD_LINK_PATTERN = Pattern.compile(
      "(\\([^)]+\\)|\\\"[^\\\"]+\\\"|:[^:]+:|[A-Za-z_][A-Za-z0-9_]*)\\s*<[-.o*]*(?:left|right|up|down)?[-.o*]+\\s*(\\\"[^\\\"]+\\\"|:[^:]+:|[A-Za-z_][A-Za-z0-9_]*)",
      Pattern.CASE_INSENSITIVE);

  private UseCasePlantUmlAnalyzer() {
  }

  public static ParseResult analyze(String rawText) {
    if (rawText == null || rawText.trim().isEmpty()) {
      throw new IllegalArgumentException("PlantUML 内容为空。");
    }

    String normalized = rawText.replace("\r\n", "\n");
    String[] lines = normalized.split("\n");

    Set<String> actorKeys = new LinkedHashSet<>();
    Map<String, String> useCaseAliasToName = new LinkedHashMap<>();
    Set<String> declaredUseCases = new LinkedHashSet<>();

    // 第一遍：解析参与者和用例声明
    for (String line : lines) {
      String t = normalizeLine(line);
      if (t.isEmpty()) {
        continue;
      }

      // 解析参与者声明
      Matcher actorMatcher = ACTOR_DECLARE_PATTERN.matcher(t);
      if (actorMatcher.matches()) {
        String actorRef = actorMatcher.group(2);
        String actorDisplay = actorMatcher.group(1);
        String actorKey = sanitizeRef(actorRef);
        if (actorKey.isEmpty()) {
          actorKey = sanitizeRef(actorDisplay);
        }
        if (!actorKey.isEmpty()) {
          actorKeys.add(actorKey);
        }
        continue;
      }

      // 解析用例声明
      Matcher useCaseMatcher = USECASE_DECLARE_PATTERN.matcher(t);
      if (useCaseMatcher.matches()) {
        String useCaseRef = sanitizeRef(useCaseMatcher.group(2));
        String displayName = sanitizeRef(useCaseMatcher.group(1));
        String canonicalName = !displayName.isEmpty() ? displayName : useCaseRef;
        if (!canonicalName.isEmpty()) {
          declaredUseCases.add(canonicalName);
        }
        if (!useCaseRef.isEmpty() && !canonicalName.isEmpty()) {
          useCaseAliasToName.put(useCaseRef, canonicalName);
        }
      }
    }

    // 第二遍：解析关系（使用 collectLinks）
    Map<String, Set<String>> actorToUseCases = new LinkedHashMap<>();
    Map<String, Set<String>> useCaseToActors = new LinkedHashMap<>();
    Set<String> linkedUseCases = new LinkedHashSet<>();

    for (String line : lines) {
      String t = normalizeLine(line);
      if (t.isEmpty()) {
        continue;
      }
      collectLinks(t, actorKeys, useCaseAliasToName, actorToUseCases, useCaseToActors, linkedUseCases);
    }

    // 合并所有用例
    Set<String> finalUseCases = new LinkedHashSet<>();
    finalUseCases.addAll(declaredUseCases);
    finalUseCases.addAll(linkedUseCases);

    if (actorKeys.isEmpty() && finalUseCases.isEmpty()) {
      throw new IllegalArgumentException("未识别到参与者或用例，请检查 PlantUML 语法。");
    }

    // 确保所有参与者和用例都有映射
    for (String actor : actorKeys) {
      actorToUseCases.computeIfAbsent(actor, k -> new LinkedHashSet<>());
    }
    for (String uc : finalUseCases) {
      useCaseToActors.computeIfAbsent(uc, k -> new LinkedHashSet<>());
    }

    // 计算用例复杂度（基于关联的参与者数量 - 标准UCP方法）
    int simpleUc = 0;
    int averageUc = 0;
    int complexUc = 0;
    for (String uc : finalUseCases) {
      int actorCount = useCaseToActors.getOrDefault(uc, Collections.emptySet()).size();
      if (actorCount <= 1) {
        simpleUc++;
      } else if (actorCount == 2) {
        averageUc++;
      } else {
        complexUc++;
      }
    }

    // 计算参与者复杂度（基于关联的用例数量 - 标准UCP方法）
    int simpleActor = 0;
    int averageActor = 0;
    int complexActor = 0;
    for (String actor : actorKeys) {
      int useCaseCount = actorToUseCases.getOrDefault(actor, Collections.emptySet()).size();
      if (useCaseCount <= 1) {
        simpleActor++;
      } else if (useCaseCount <= 3) {
        averageActor++;
      } else {
        complexActor++;
      }
    }

    return new ParseResult(simpleUc, averageUc, complexUc, simpleActor, averageActor, complexActor);
  }

  private static void collectLinks(
      String line,
      Set<String> actorKeys,
      Map<String, String> useCaseAliasToName,
      Map<String, Set<String>> actorToUseCases,
      Map<String, Set<String>> useCaseToActors,
      Set<String> linkedUseCases) {

    Matcher f = FORWARD_LINK_PATTERN.matcher(line);
    while (f.find()) {
      String left = f.group(1);
      String right = f.group(2);
      tryLink(left, right, actorKeys, useCaseAliasToName, actorToUseCases, useCaseToActors, linkedUseCases);
    }

    Matcher b = BACKWARD_LINK_PATTERN.matcher(line);
    while (b.find()) {
      String left = b.group(1);
      String right = b.group(2);
      tryLink(right, left, actorKeys, useCaseAliasToName, actorToUseCases, useCaseToActors, linkedUseCases);
    }
  }

  private static void tryLink(
      String actorRefToken,
      String useCaseRefToken,
      Set<String> actorKeys,
      Map<String, String> useCaseAliasToName,
      Map<String, Set<String>> actorToUseCases,
      Map<String, Set<String>> useCaseToActors,
      Set<String> linkedUseCases) {

    String actorKey = resolveActor(actorRefToken, actorKeys, useCaseAliasToName);
    String useCaseName = resolveUseCase(useCaseRefToken, useCaseAliasToName);
    if (actorKey.isEmpty() || useCaseName.isEmpty()) {
      return;
    }

    actorKeys.add(actorKey);
    linkedUseCases.add(useCaseName);
    actorToUseCases.computeIfAbsent(actorKey, k -> new LinkedHashSet<>()).add(useCaseName);
    useCaseToActors.computeIfAbsent(useCaseName, k -> new LinkedHashSet<>()).add(actorKey);
  }

  private static String resolveActor(String token, Set<String> actorKeys, Map<String, String> useCaseAliasToName) {
    String key = sanitizeRef(token);
    if (key.isEmpty()) {
      return "";
    }
    if (useCaseAliasToName.containsKey(key) || isUseCaseParenToken(token)) {
      return "";
    }
    if (isPlantKeyword(key)) {
      return "";
    }
    if (actorKeys.contains(key)) {
      return key;
    }
    return key;
  }

  private static String resolveUseCase(String token, Map<String, String> useCaseAliasToName) {
    if (token == null) {
      return "";
    }
    String t = token.trim();
    if (isUseCaseParenToken(t)) {
      return sanitizeUseCase(t.substring(1, t.length() - 1));
    }
    String ref = sanitizeRef(t);
    if (ref.isEmpty()) {
      return "";
    }
    if (useCaseAliasToName.containsKey(ref)) {
      return useCaseAliasToName.get(ref);
    }
    if (isPlantKeyword(ref)) {
      return "";
    }
    return ref;
  }

  private static boolean isUseCaseParenToken(String token) {
    String t = token == null ? "" : token.trim();
    return t.startsWith("(") && t.endsWith(")") && t.length() >= 2;
  }

  private static String normalizeLine(String line) {
    if (line == null) {
      return "";
    }
    String t = line.trim();
    if (t.isEmpty()) {
      return "";
    }
    int cidx = t.indexOf('\'');
    if (cidx >= 0) {
      t = t.substring(0, cidx).trim();
    }
    if (t.isEmpty()) {
      return "";
    }
    String low = t.toLowerCase(Locale.ROOT);
    if (low.startsWith("@startuml") || low.startsWith("@enduml") || low.startsWith("left to right direction")
        || low.startsWith("top to bottom direction") || low.startsWith("skinparam")
        || low.startsWith("title")) {
      return "";
    }
    return t;
  }

  private static boolean isPlantKeyword(String ref) {
    String low = ref.toLowerCase(Locale.ROOT);
    return "left".equals(low) || "right".equals(low) || "up".equals(low) || "down".equals(low)
        || "include".equals(low) || "extend".equals(low);
  }

  private static String sanitizeRef(String raw) {
    if (raw == null) {
      return "";
    }
    String v = raw.trim();
    if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) {
      v = v.substring(1, v.length() - 1);
    }
    if (v.startsWith(":") && v.endsWith(":") && v.length() >= 2) {
      v = v.substring(1, v.length() - 1);
    }
    return v.trim();
  }

  private static String sanitizeUseCase(String raw) {
    return sanitizeRef(raw);
  }

  public static final class ParseResult {
    public final int simpleUseCases;
    public final int averageUseCases;
    public final int complexUseCases;
    public final int simpleActors;
    public final int averageActors;
    public final int complexActors;

    public ParseResult(int simpleUseCases,
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

    @Override
    public String toString() {
      return String.format("用例(简单/平均/复杂)=%d/%d/%d，参与者(简单/平均/复杂)=%d/%d/%d",
          simpleUseCases, averageUseCases, complexUseCases,
          simpleActors, averageActors, complexActors);
    }
  }
}