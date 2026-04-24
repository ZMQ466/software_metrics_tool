package com.metrics.design;

import java.util.ArrayList;
import java.util.List;

/**
 * 需求/设计度量计算引擎。
 *
 * <p>
 * 将根目录下的功能点、用例点、特征点计算逻辑接入到 Maven 项目中，
 * 供 GUI 与后续实验分析直接调用。
 * </p>
 */
public final class RequirementDesignMetricsEngine {
  private RequirementDesignMetricsEngine() {
  }

  public static ValidationResult validateFunctionPointInput(FunctionPointInput input) {
    List<String> errors = new ArrayList<>();
    if (input.ei < 0 || input.eo < 0 || input.eq < 0 || input.ilf < 0 || input.eif < 0) {
      errors.add("Function point counts must be >= 0.");
    }
    if (input.gscTotal < 0 || input.gscTotal > 70) {
      errors.add("GSC total must be between 0 and 70.");
    }
    return ValidationResult.of(errors);
  }

  public static FunctionPointResult calculateFunctionPoint(FunctionPointInput input) {
    ValidationResult validation = validateFunctionPointInput(input);
    if (!validation.valid) {
      throw new IllegalArgumentException(String.join(" ", validation.errors));
    }

    int ufp = (input.ei * 4)
        + (input.eo * 5)
        + (input.eq * 4)
        + (input.ilf * 10)
        + (input.eif * 7);
    double vaf = 0.6 + (0.01 * input.gscTotal);
    double adjustedFp = ufp * vaf;

    return new FunctionPointResult(ufp, vaf, adjustedFp);
  }

  public static ValidationResult validateUseCasePointInput(UseCasePointInput input) {
    List<String> errors = new ArrayList<>();

    if (input.simpleUseCases < 0 || input.averageUseCases < 0 || input.complexUseCases < 0) {
      errors.add("Use case counts must be >= 0.");
    }
    if (input.simpleActors < 0 || input.averageActors < 0 || input.complexActors < 0) {
      errors.add("Actor counts must be >= 0.");
    }
    if (input.tcf <= 0 || input.ecf <= 0) {
      errors.add("TCF and ECF must be > 0.");
    }

    return ValidationResult.of(errors);
  }

  public static UseCasePointResult calculateUseCasePoint(UseCasePointInput input) {
    ValidationResult validation = validateUseCasePointInput(input);
    if (!validation.valid) {
      throw new IllegalArgumentException(String.join(" ", validation.errors));
    }

    int uucw = (input.simpleUseCases * 5)
        + (input.averageUseCases * 10)
        + (input.complexUseCases * 15);
    int uaw = (input.simpleActors)
        + (input.averageActors * 2)
        + (input.complexActors * 3);
    double ucp = (uucw + uaw) * input.tcf * input.ecf;

    return new UseCasePointResult(uucw, uaw, ucp);
  }

  public static ValidationResult validateFeaturePointInput(FeaturePointInput input) {
    List<String> errors = new ArrayList<>();
    if (input.algorithmicWeight <= 0) {
      errors.add("Algorithmic weight must be > 0.");
    }
    return ValidationResult.of(errors);
  }

  public static FeaturePointResult calculateFeaturePoint(FunctionPointResult fpResult, FeaturePointInput input) {
    ValidationResult validation = validateFeaturePointInput(input);
    if (!validation.valid) {
      throw new IllegalArgumentException(String.join(" ", validation.errors));
    }

    // Feature Point 以调整后功能点 FP 为基准，再乘以算法复杂度权重。
    double featurePoint = fpResult.adjustedFp * input.algorithmicWeight;
    return new FeaturePointResult(fpResult.adjustedFp, input.algorithmicWeight, featurePoint);
  }

  public static final class FunctionPointInput {
    public final int ei;
    public final int eo;
    public final int eq;
    public final int ilf;
    public final int eif;
    public final int gscTotal;

    public FunctionPointInput(int ei, int eo, int eq, int ilf, int eif, int gscTotal) {
      this.ei = ei;
      this.eo = eo;
      this.eq = eq;
      this.ilf = ilf;
      this.eif = eif;
      this.gscTotal = gscTotal;
    }
  }

  public static final class UseCasePointInput {
    public final int simpleUseCases;
    public final int averageUseCases;
    public final int complexUseCases;
    public final int simpleActors;
    public final int averageActors;
    public final int complexActors;
    public final double tcf;
    public final double ecf;

    public UseCasePointInput(
        int simpleUseCases,
        int averageUseCases,
        int complexUseCases,
        int simpleActors,
        int averageActors,
        int complexActors,
        double tcf,
        double ecf) {
      this.simpleUseCases = simpleUseCases;
      this.averageUseCases = averageUseCases;
      this.complexUseCases = complexUseCases;
      this.simpleActors = simpleActors;
      this.averageActors = averageActors;
      this.complexActors = complexActors;
      this.tcf = tcf;
      this.ecf = ecf;
    }
  }

  public static final class FeaturePointInput {
    public final double algorithmicWeight;

    public FeaturePointInput(double algorithmicWeight) {
      this.algorithmicWeight = algorithmicWeight;
    }
  }

  public static final class FunctionPointResult {
    public final int ufp;
    public final double vaf;
    public final double adjustedFp;

    public FunctionPointResult(int ufp, double vaf, double adjustedFp) {
      this.ufp = ufp;
      this.vaf = vaf;
      this.adjustedFp = adjustedFp;
    }
  }

  public static final class UseCasePointResult {
    public final int uucw;
    public final int uaw;
    public final double ucp;

    public UseCasePointResult(int uucw, int uaw, double ucp) {
      this.uucw = uucw;
      this.uaw = uaw;
      this.ucp = ucp;
    }
  }

  public static final class FeaturePointResult {
    public final double baseAdjustedFp;
    public final double algorithmicWeight;
    public final double featurePoint;

    public FeaturePointResult(double baseAdjustedFp, double algorithmicWeight, double featurePoint) {
      this.baseAdjustedFp = baseAdjustedFp;
      this.algorithmicWeight = algorithmicWeight;
      this.featurePoint = featurePoint;
    }
  }

  public static final class ValidationResult {
    public final boolean valid;
    public final List<String> errors;

    private ValidationResult(boolean valid, List<String> errors) {
      this.valid = valid;
      this.errors = errors;
    }

    public static ValidationResult of(List<String> errors) {
      return new ValidationResult(errors.isEmpty(), errors);
    }
  }
}