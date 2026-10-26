import frontEnd.Reporter;
import frontEnd.lexer.Lexer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class SwitchExpressionRegressionTest {
    private final Path repoRoot;
    private final Path workRoot = Path.of("").toAbsolutePath();
    private final String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
    private final String classPath = System.getProperty("java.class.path");
    private int passed;

    private SwitchExpressionRegressionTest(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    public static void main(String[] args) throws Exception {
        new SwitchExpressionRegressionTest(Path.of(args[0]).toAbsolutePath()).run();
    }

    private void run() throws Exception {
        List<Path> cases;
        try (var paths = Files.list(repoRoot.resolve("tests/switch-expression/cases"))) {
            cases = paths.filter(Files::isDirectory).sorted().toList();
        }
        for (Path source : cases) {
            runCase(source);
        }
        checkLexerBoundaries();
        checkReporterReset();
        System.out.printf("Switch expression regression tests: %d/%d passed.%n", passed, cases.size() + 2);
    }

    private void runCase(Path source) throws Exception {
        String name = source.getFileName().toString();
        Path work = Files.createDirectories(workRoot.resolve(name));
        Files.copy(source.resolve("testfile.txt"), work.resolve("testfile.txt"), StandardCopyOption.REPLACE_EXISTING);
        if (name.equals("crlf_sample")) {
            String input = Files.readString(source.resolve("testfile.txt")).replace("\r\n", "\n");
            Files.writeString(work.resolve("testfile.txt"), input.replace("\n", "\r\n"));
        }
        Path log = work.resolve("compiler.log");
        Process process = new ProcessBuilder(javaExecutable, "-Dfile.encoding=UTF-8", "-cp", classPath, "Compiler")
                .directory(work.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        process.getOutputStream().close();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor();
            fail(name, "compiler exceeded 10 seconds");
        }
        if (process.exitValue() != 0) {
            fail(name, "compiler failed: " + Files.readString(log));
        }
        compare(name, source.resolve("error.txt"), work.resolve("error.txt"));
        if (Files.isRegularFile(source.resolve("parser.txt"))) {
            compare(name, source.resolve("lexer.txt"), work.resolve("lexer.txt"));
            compare(name, source.resolve("parser.txt"), work.resolve("parser.txt"));
        } else if (!Files.readString(work.resolve("parser.txt")).stripTrailing().endsWith("<CompUnit>")) {
            fail(name, "error recovery did not finish the compilation unit");
        }
        for (String forbidden : List.of("symbol.txt", "llvm_ir.txt", "mips.txt")) {
            if (Files.exists(work.resolve(forbidden))) {
                fail(name, "exam entry point reached a later compiler phase: " + forbidden);
            }
        }
        pass(name);
    }

    private void checkLexerBoundaries() throws Exception {
        Reporter.REPORTER.reset();
        Path output = workRoot.resolve("arrow-boundaries.txt");
        new Lexer("-> - > -/*gap*/> --> ->> '>' \"->\"").analyze(output.toString());
        List<String> expected = List.of("ARROW ->", "MINU -", "GRE >", "MINU -", "GRE >",
                "MINU -", "ARROW ->", "ARROW ->", "GRE >", "CHARCON '>'", "STRCON \"->\"");
        if (!Files.readAllLines(output).equals(expected) || Reporter.REPORTER.hasError()) {
            fail("lexer_boundaries", "arrow longest-match, whitespace/comment, or literal handling differs");
        }
        pass("lexer_boundaries");
    }

    private void checkReporterReset() throws Exception {
        Path cases = repoRoot.resolve("tests/switch-expression/cases");
        Files.copy(cases.resolve("testcase11/testfile.txt"), workRoot.resolve("testfile.txt"), StandardCopyOption.REPLACE_EXISTING);
        Compiler.main(new String[0]);
        compare("reporter_reset_first", cases.resolve("testcase11/error.txt"), workRoot.resolve("error.txt"));
        Files.copy(cases.resolve("testcase1/testfile.txt"), workRoot.resolve("testfile.txt"), StandardCopyOption.REPLACE_EXISTING);
        Compiler.main(new String[0]);
        compare("reporter_reset_second", cases.resolve("testcase1/error.txt"), workRoot.resolve("error.txt"));
        compare("reporter_reset_second", cases.resolve("testcase1/parser.txt"), workRoot.resolve("parser.txt"));
        pass("reporter_reset");
    }

    private static void compare(String name, Path expectedFile, Path actualFile) throws Exception {
        String expected = normalize(Files.readString(expectedFile));
        String actual = normalize(Files.readString(actualFile));
        if (!expected.equals(actual)) {
            String[] wanted = expected.split("\n", -1);
            String[] found = actual.split("\n", -1);
            int line = 0;
            while (line < Math.min(wanted.length, found.length) && wanted[line].equals(found[line])) {
                line++;
            }
            fail(name, expectedFile.getFileName() + " differs at line " + (line + 1)
                    + "; expected " + (line < wanted.length ? wanted[line] : "<EOF>")
                    + ", got " + (line < found.length ? found[line] : "<EOF>"));
        }
    }

    private static String normalize(String value) {
        return value.replace("\r\n", "\n").stripTrailing();
    }

    private void pass(String name) {
        passed++;
        System.out.println("[PASS] " + name);
    }

    private static void fail(String name, String message) {
        throw new AssertionError("[FAIL] " + name + ": " + message);
    }
}
