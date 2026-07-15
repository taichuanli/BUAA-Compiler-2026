import frontEnd.ASTNode;
import frontEnd.Reporter;
import frontEnd.SymbolTable;
import frontEnd.Token;
import frontEnd.lexer.Lexer;
import frontEnd.parser.Parser;
import frontEnd.visitor.Visitor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class M1RegressionTest {
    private final Path repoRoot;
    private final Path casesRoot;
    private final Path workRoot;
    private int passed;

    private M1RegressionTest(Path repoRoot) {
        this.repoRoot = repoRoot;
        this.casesRoot = repoRoot.resolve("tests/m1/cases");
        this.workRoot = Path.of("").toAbsolutePath();
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: M1RegressionTest <repo-root>");
        }
        M1RegressionTest suite = new M1RegressionTest(Path.of(args[0]).toAbsolutePath());
        suite.run();
    }

    private void run() throws Exception {
        runLexerCase("2026_keywords");
        runLexerCase("keyword_boundaries");

        runErrorCase("illegal_logical_operator");
        runErrorCase("missing_semicolon");
        runErrorCase("missing_right_parenthesis");
        runErrorCase("missing_right_bracket");
        runErrorCase("semantic_error_codes");

        runCompilerResetCase();
        System.out.printf("M1 regression tests: %d/8 passed.%n", passed);
    }

    private void runLexerCase(String name) throws Exception {
        Path caseDir = casesRoot.resolve("lexer").resolve(name);
        Reporter.REPORTER.reset();
        String source = Files.readString(caseDir.resolve("testfile.txt"));
        Path output = workRoot.resolve(name + "-lexer.txt");
        new Lexer(source).analyze(output.toString());

        List<String> actual = Files.readAllLines(output);
        for (String expected : Files.readAllLines(caseDir.resolve("expected_tokens.txt"))) {
            if (!expected.isBlank() && !actual.contains(expected)) {
                fail(name, "missing token: " + expected);
            }
        }
        if (Reporter.REPORTER.hasError()) {
            fail(name, "valid 2026 lexer case unexpectedly reported an error");
        }
        pass(name);
    }

    private void runErrorCase(String name) throws Exception {
        Path caseDir = casesRoot.resolve("error").resolve(name);
        Reporter.REPORTER.reset();
        String source = Files.readString(caseDir.resolve("testfile.txt"));

        ArrayList<Token> tokens = new Lexer(source).analyze(workRoot.resolve(name + "-lexer.txt").toString());
        ASTNode root = new Parser(tokens).analyze(workRoot.resolve(name + "-parser.txt").toString());
        HashMap<Integer, SymbolTable> ignored =
                new Visitor(root).analyze(workRoot.resolve(name + "-symbol.txt").toString());
        Reporter.REPORTER.report();

        String expected = normalize(Files.readString(caseDir.resolve("error.txt")));
        String actual = normalize(Files.readString(workRoot.resolve("error.txt")));
        if (!actual.equals(expected)) {
            fail(name, "expected [" + expected + "] but got [" + actual + "]");
        }
        pass(name);
    }

    private void runCompilerResetCase() throws Exception {
        Path caseDir = casesRoot.resolve("compiler_reset");
        Path input = workRoot.resolve("testfile.txt");

        Files.copy(caseDir.resolve("first_error.txt"), input, StandardCopyOption.REPLACE_EXISTING);
        Compiler.main(new String[0]);
        String firstExpected = normalize(Files.readString(caseDir.resolve("first_error_expected.txt")));
        String firstActual = normalize(Files.readString(workRoot.resolve("error.txt")));
        if (!firstActual.equals(firstExpected)) {
            fail("compiler_reset", "first run expected [" + firstExpected + "] but got [" + firstActual + "]");
        }

        Files.copy(caseDir.resolve("second_valid.txt"), input, StandardCopyOption.REPLACE_EXISTING);
        Compiler.main(new String[0]);
        String secondActual = normalize(Files.readString(workRoot.resolve("error.txt")));
        if (!secondActual.isEmpty()) {
            fail("compiler_reset", "second run retained stale errors: [" + secondActual + "]");
        }
        pass("compiler_reset");
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n").strip();
    }

    private void pass(String name) {
        passed++;
        System.out.println("[PASS] " + name);
    }

    private static void fail(String name, String message) {
        throw new AssertionError("[FAIL] " + name + ": " + message);
    }
}
