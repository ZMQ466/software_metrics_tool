package com.metrics.parser;

import com.metrics.model.ClassInfo;
import com.metrics.model.MethodInfo;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
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

public class EclipseJdtCodeParser implements CodeParser {
    @Override
    public List<ClassInfo> parseFile(String filePath) {
        try {
            String source = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
            return parseCompilationUnit(source);
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

    private List<ClassInfo> parseCompilationUnit(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(false);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<ClassInfo> results = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(TypeDeclaration node) {
                ClassInfo classInfo = new ClassInfo(node.getName().getIdentifier());
                if (node.getSuperclassType() != null) {
                    classInfo.setSuperClassName(node.getSuperclassType().toString());
                }
                for (Object itf : node.superInterfaceTypes()) {
                    classInfo.getInterfaces().add(itf.toString());
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

                    classInfo.getMethods().add(methodInfo);
                }

                results.add(classInfo);
                return false;
            }
        });

        return results;
    }

    private void addTypeAsCalledClass(ClassInfo classInfo, Type type) {
        if (type == null) {
            return;
        }
        String t = type.toString();
        if (t == null) {
            return;
        }
        t = t.trim();
        if (t.isEmpty()) {
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

        private MethodBodyVisitor(Set<String> fieldNames, ClassInfo classInfo, MethodInfo methodInfo) {
            this.fieldNames = fieldNames;
            this.classInfo = classInfo;
            this.methodInfo = methodInfo;
        }

        int getCyclomaticComplexity() {
            return 1 + decisionPoints;
        }

        @Override
        public boolean visit(IfStatement node) {
            decisionPoints++;
            return true;
        }

        @Override
        public boolean visit(ForStatement node) {
            decisionPoints++;
            return true;
        }

        @Override
        public boolean visit(EnhancedForStatement node) {
            decisionPoints++;
            return true;
        }

        @Override
        public boolean visit(WhileStatement node) {
            decisionPoints++;
            return true;
        }

        @Override
        public boolean visit(DoStatement node) {
            decisionPoints++;
            return true;
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
            return true;
        }

        @Override
        public boolean visit(ConditionalExpression node) {
            decisionPoints++;
            return true;
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
                String t = node.getType().toString();
                if (t != null && !t.trim().isEmpty() && !classInfo.getCalledClasses().contains(t)) {
                    classInfo.getCalledClasses().add(t);
                }
            }
            return true;
        }

        @Override
        public boolean visit(VariableDeclarationStatement node) {
            if (node.getType() != null) {
                String t = node.getType().toString();
                if (t != null && !t.trim().isEmpty() && !classInfo.getCalledClasses().contains(t)) {
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
