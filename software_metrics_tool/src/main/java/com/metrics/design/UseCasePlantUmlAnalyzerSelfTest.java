package com.metrics.design;

public class UseCasePlantUmlAnalyzerSelfTest {
    public static void main(String[] args) {
        caseBasicSample();
        caseSharedUseCase();
        caseAliasAndQuoted();
        caseIncludeExtendNoise();
        System.out.println("UseCasePlantUmlAnalyzer self-tests passed.");
    }

    private static void caseBasicSample() {
        String puml = "@startuml\n"
                + "left to right direction\n"
                + "actor User\n"
                + "actor Admin\n"
                + "User --> (Login)\n"
                + "User --> (Search Book)\n"
                + "Admin --> (Manage Order)\n"
                + "@enduml\n";
        UseCasePlantUmlAnalyzer.ParseResult r = UseCasePlantUmlAnalyzer.analyze(puml);
        assertEq("case1 simple UC", 3, r.simpleUseCases);
        assertEq("case1 avg UC", 0, r.averageUseCases);
        assertEq("case1 complex UC", 0, r.complexUseCases);
        assertEq("case1 simple actor", 1, r.simpleActors);
        assertEq("case1 avg actor", 1, r.averageActors);
        assertEq("case1 complex actor", 0, r.complexActors);
    }

    private static void caseSharedUseCase() {
        String puml = "@startuml\n"
                + "actor User\n"
                + "actor Admin\n"
                + "actor Guest\n"
                + "User --> (Login)\n"
                + "Admin --> (Login)\n"
                + "Admin --> (Manage User)\n"
                + "Guest --> (Browse)\n"
                + "@enduml\n";
        UseCasePlantUmlAnalyzer.ParseResult r = UseCasePlantUmlAnalyzer.analyze(puml);
        assertEq("case2 simple UC", 2, r.simpleUseCases);
        assertEq("case2 avg UC", 1, r.averageUseCases);
        assertEq("case2 complex UC", 0, r.complexUseCases);
        assertEq("case2 simple actor", 2, r.simpleActors);
        assertEq("case2 avg actor", 1, r.averageActors);
        assertEq("case2 complex actor", 0, r.complexActors);
    }

    private static void caseAliasAndQuoted() {
        String puml = "@startuml\n"
                + "actor \"Library Member\" as Member\n"
                + "actor Librarian\n"
                + "usecase \"Borrow Book\" as UC_Borrow\n"
                + "usecase \"Return Book\" as UC_Return\n"
                + "Member --> UC_Borrow\n"
                + "Member --> UC_Return\n"
                + "Librarian --> UC_Borrow\n"
                + "@enduml\n";
        UseCasePlantUmlAnalyzer.ParseResult r = UseCasePlantUmlAnalyzer.analyze(puml);
        assertEq("case3 simple UC", 1, r.simpleUseCases);
        assertEq("case3 avg UC", 1, r.averageUseCases);
        assertEq("case3 complex UC", 0, r.complexUseCases);
        assertEq("case3 simple actor", 1, r.simpleActors);
        assertEq("case3 avg actor", 1, r.averageActors);
        assertEq("case3 complex actor", 0, r.complexActors);
    }

    private static void caseIncludeExtendNoise() {
        String puml = "@startuml\n"
                + "actor User\n"
                + "User --> (Search)\n"
                + "(Search) .> (Filter) : <<include>>\n"
                + "(Search) .> (Sort) : <<extend>>\n"
                + "@enduml\n";
        UseCasePlantUmlAnalyzer.ParseResult r = UseCasePlantUmlAnalyzer.analyze(puml);
        assertEq("case4 simple UC", 1, r.simpleUseCases);
        assertEq("case4 avg UC", 0, r.averageUseCases);
        assertEq("case4 complex UC", 0, r.complexUseCases);
        assertEq("case4 simple actor", 1, r.simpleActors);
        assertEq("case4 avg actor", 0, r.averageActors);
        assertEq("case4 complex actor", 0, r.complexActors);
    }

    private static void assertEq(String label, int expected, int actual) {
        if (expected != actual) {
            throw new IllegalStateException(label + ": expected=" + expected + ", actual=" + actual);
        }
    }
}
