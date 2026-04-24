package com.metrics.parser;

import com.metrics.model.ClassInfo;
import com.metrics.model.MethodInfo;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于 Eclipse JDT {@link ASTParser} 的 Java 源码解析：提取类、字段、方法、
 * 继承关系、方法调用与字段访问，供 CK / LK 等面向对象度量使用。
 */
public class EclipseJdtCodeParser implements CodeParser {
    @Override
    public List<ClassInfo> parseFile(String filePath) {
        try {
            String source = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
            return parseCompilationUnit(source, filePath);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + filePath, e);
        }
    }

    @Override
    public List<ClassInfo> parseDirectory(String directoryPath) {
        Path root = Paths.get(directoryPath);
        if (!Files.exists(root)) {
            throw new IllegalArgumentException("目录不存在: " + directoryPath);
        }

        try {
            List<Path> javaFiles = Files.walk(root)
                    .filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".java"))
                    .collect(Collectors.toList());

            List<ClassInfo> classes = new ArrayList<>();
            for (Path file : javaFiles) {
                classes.addAll(parseFile(file.toString()));
            }
            return classes;
        } catch (IOException e) {
            throw new RuntimeException("遍历目录失败: " + directoryPath, e);
        }
    }

    private List<ClassInfo> parseCompilationUnit(String source, String filePath) {
        ASTParser parser = ASTParser.newParser(AST.JLS14);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(false);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        String packagePrefix = "";
        if (cu.getPackage() != null) {
            packagePrefix = cu.getPackage().getName().getFullyQualifiedName();
        }

        List<ClassInfo> results = new ArrayList<>();
        for (Object t : cu.types()) {
            if (t instanceof TypeDeclaration) {
                parseTypeDeclaration((TypeDeclaration) t, cu, packagePrefix, "", results, filePath);
            }
        }
        return results;
    }

    /**
     * 递归解析顶层/成员类；{@code binaryNameQualifier} 为外层简单名链，如 {@code Outer} 或 {@code Outer$Inner}。
     */
    private void parseTypeDeclaration(TypeDeclaration node, CompilationUnit cu, String packagePrefix,
                                      String binaryNameQualifier, List<ClassInfo> results, String filePath) {
        String simple = node.getName().getIdentifier();
        String nestedBinary = binaryNameQualifier.isEmpty() ? simple : binaryNameQualifier + "$" + simple;
        String qualifiedName = packagePrefix.isEmpty() ? nestedBinary : packagePrefix + "." + nestedBinary;

        ClassInfo classInfo = new ClassInfo(nestedBinary);
        classInfo.setQualifiedName(qualifiedName);
        classInfo.setSourceFilePath(filePath);
        int classStartLine = cu.getLineNumber(node.getStartPosition());
        int classEndLine = cu.getLineNumber(node.getStartPosition() + node.getLength());
        classInfo.setStartLine(classStartLine);
        classInfo.setEndLine(classEndLine);

        if (node.getSuperclassType() != null) {
            String sup = stripTypeArguments(node.getSuperclassType().toString());
            classInfo.setSuperClassName(sup);
            addTypeAsCalledClass(classInfo, node.getSuperclassType());
        }
        for (Object itf : node.superInterfaceTypes()) {
            String itfStr = stripTypeArguments(itf.toString());
            classInfo.getInterfaces().add(itfStr);
            addTypeAsCalledClass(classInfo, (Type) itf);
        }

        Set<String> fieldNames = new HashSet<>();
        for (FieldDeclaration fieldDecl : node.getFields()) {
            for (Object frag : fieldDecl.fragments()) {
                if (frag instanceof VariableDeclarationFragment) {
                    String name = ((VariableDeclarationFragment) frag).getName().getIdentifier();
                    fieldNames.add(name);
                    classInfo.getFields().add(name);
                }
            }
            addTypeAsCalledClass(classInfo, fieldDecl.getType());
        }

        for (MethodDeclaration methodDecl : node.getMethods()) {
            MethodInfo methodInfo = new MethodInfo(methodDecl.getName().getIdentifier());
            if (methodDecl.getReturnType2() != null) {
                methodInfo.setReturnType(methodDecl.getReturnType2().toString());
                addTypeAsCalledClass(classInfo, methodDecl.getReturnType2());
            } else {
                methodInfo.setReturnType("void");
            }

            for (Object p : methodDecl.parameters()) {
                if (p instanceof SingleVariableDeclaration) {
                    SingleVariableDeclaration svd = (SingleVariableDeclaration) p;
                    methodInfo.getParameters().add(svd.getType() + " " + svd.getName());
                    addTypeAsCalledClass(classInfo, svd.getType());
                }
            }

            int startLine = cu.getLineNumber(methodDecl.getStartPosition());
            int endLine = cu.getLineNumber(methodDecl.getStartPosition() + methodDecl.getLength());
            methodInfo.setStartLine(startLine);
            methodInfo.setEndLine(endLine);
            if (startLine > 0 && endLine > 0 && endLine >= startLine) {
                methodInfo.setLoc(endLine - startLine + 1);
            } else {
                methodInfo.setLoc(0);
            }

            MethodBodyVisitor bodyVisitor = new MethodBodyVisitor(fieldNames, classInfo, methodInfo);
            if (methodDecl.getBody() != null) {
                methodDecl.getBody().accept(bodyVisitor);
            }
            methodInfo.setCyclomaticComplexity(bodyVisitor.getCyclomaticComplexity());
            methodInfo.setMaxNestingDepth(bodyVisitor.getMaxNestingDepth());

            classInfo.getMethods().add(methodInfo);
        }

        results.add(classInfo);

        for (BodyDeclaration bd : (List<BodyDeclaration>) node.bodyDeclarations()) {
            if (bd instanceof TypeDeclaration) {
                parseTypeDeclaration((TypeDeclaration) bd, cu, packagePrefix, nestedBinary, results, filePath);
            }
        }
    }

    private static String stripTypeArguments(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        int depth = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '<') {
                depth++;
                if (depth == 1) {
                    break;
                }
            }
            if (depth == 0) {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    private void addTypeAsCalledClass(ClassInfo classInfo, Type type) {
        if (type == null) {
            return;
        }
        String t = stripTypeArguments(type.toString());
        if (t == null || t.isEmpty()) {
            return;
        }
        if (!classInfo.getCalledClasses().contains(t)) {
            classInfo.getCalledClasses().add(t);
        }
    }

    private static class MethodBodyVisitor extends ASTVisitor {
        private final Set<String> fieldNames;
        private final ClassInfo classInfo;
        private final MethodInfo methodInfo;
        private int decisionPoints = 0;
        private int currentNestingDepth = 0;
        private int maxNestingDepth = 0;

        private MethodBodyVisitor(Set<String> fieldNames, ClassInfo classInfo, MethodInfo methodInfo) {
            this.fieldNames = fieldNames;
            this.classInfo = classInfo;
            this.methodInfo = methodInfo;
        }

        int getCyclomaticComplexity() {
            return 1 + decisionPoints;
        }

        int getMaxNestingDepth() {
            return maxNestingDepth;
        }

        private void enterNesting() {
            currentNestingDepth++;
            if (currentNestingDepth > maxNestingDepth) {
                maxNestingDepth = currentNestingDepth;
            }
        }

        private void exitNesting() {
            currentNestingDepth--;
            if (currentNestingDepth < 0) {
                currentNestingDepth = 0;
            }
        }

        @Override
        public boolean visit(IfStatement node) {
            decisionPoints++;
            enterNesting();
            return true;
        }

        @Override
        public void endVisit(IfStatement node) {
            exitNesting();
        }

        @Override
        public boolean visit(ForStatement node) {
            decisionPoints++;
            enterNesting();
            return true;
        }

        @Override
        public void endVisit(ForStatement node) {
            exitNesting();
        }

        @Override
        public boolean visit(EnhancedForStatement node) {
            decisionPoints++;
            enterNesting();
            return true;
        }

        @Override
        public void endVisit(EnhancedForStatement node) {
            exitNesting();
        }

        @Override
        public boolean visit(WhileStatement node) {
            decisionPoints++;
            enterNesting();
            return true;
        }

        @Override
        public void endVisit(WhileStatement node) {
            exitNesting();
        }

        @Override
        public boolean visit(DoStatement node) {
            decisionPoints++;
            enterNesting();
            return true;
        }

        @Override
        public void endVisit(DoStatement node) {
            exitNesting();
        }

        @Override
        public boolean visit(SwitchStatement node) {
            enterNesting();
            return true;
        }

        @Override
        public void endVisit(SwitchStatement node) {
            exitNesting();
        }

        @Override
        public boolean visit(SwitchCase node) {
            if (!node.isDefault()) {
                decisionPoints++;
            }
            return true;
        }

        @Override
        public boolean visit(CatchClause node) {
            decisionPoints++;
            enterNesting();
            return true;
        }

        @Override
        public void endVisit(CatchClause node) {
            exitNesting();
        }

        @Override
        public boolean visit(ConditionalExpression node) {
            decisionPoints++;
            enterNesting();
            return true;
        }

        @Override
        public void endVisit(ConditionalExpression node) {
            exitNesting();
        }

        @Override
        public boolean visit(InfixExpression node) {
            InfixExpression.Operator op = node.getOperator();
            if (op == InfixExpression.Operator.CONDITIONAL_AND || op == InfixExpression.Operator.CONDITIONAL_OR) {
                decisionPoints++;
            }
            return true;
        }

        @Override
        public boolean visit(MethodInvocation node) {
            methodInfo.getMethodCalls().add(node.getName().getIdentifier());
            return true;
        }

        @Override
        public boolean visit(SuperMethodInvocation node) {
            methodInfo.getMethodCalls().add(node.getName().getIdentifier());
            return true;
        }

        @Override
        public boolean visit(ClassInstanceCreation node) {
            if (node.getType() != null) {
                String t = stripTypeArguments(node.getType().toString());
                if (!t.isEmpty() && !classInfo.getCalledClasses().contains(t)) {
                    classInfo.getCalledClasses().add(t);
                }
            }
            return true;
        }

        @Override
        public boolean visit(VariableDeclarationStatement node) {
            if (node.getType() != null) {
                String t = stripTypeArguments(node.getType().toString());
                if (!t.isEmpty() && !classInfo.getCalledClasses().contains(t)) {
                    classInfo.getCalledClasses().add(t);
                }
            }
            return true;
        }

        @Override
        public boolean visit(SimpleName node) {
            String name = node.getIdentifier();
            if (fieldNames.contains(name) && !methodInfo.getAccessedFields().contains(name)) {
                methodInfo.getAccessedFields().add(name);
            }
            return true;
        }

        @Override
        public boolean visit(TryStatement node) {
            return true;
        }

        @Override
        public boolean visit(ThrowStatement node) {
            return true;
        }

        @Override
        public boolean visit(ReturnStatement node) {
            return true;
        }
    }
}
