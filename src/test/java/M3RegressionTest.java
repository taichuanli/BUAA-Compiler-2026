import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class M3RegressionTest {
    private final Path casesRoot;
    private final Path workRoot;
    private final String javaExecutable;
    private final String classPath;
    private int passed;

    private M3RegressionTest(Path repoRoot) {
        this.casesRoot = repoRoot.resolve("tests/m3/cases");
        this.workRoot = Path.of("").toAbsolutePath();
        this.javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        this.classPath = System.getProperty("java.class.path");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: M3RegressionTest <repo-root>");
        }
        new M3RegressionTest(Path.of(args[0]).toAbsolutePath()).run();
    }

    private void run() throws Exception {
        runCase("scalar_calls");
        runCase("calls_in_expressions");
        runCase("argument_count_mismatch");
        runCase("legacy_names_are_identifiers");
        runCase("missing_right_parenthesis");
        System.out.printf("M3 regression tests: %d/5 passed.%n", passed);
    }

    private void runCase(String name) throws Exception {
        Path caseRoot = casesRoot.resolve(name);
        Path caseWork = Files.createDirectories(workRoot.resolve(name));
        Files.copy(caseRoot.resolve("testfile.txt"), caseWork.resolve("testfile.txt"),
                StandardCopyOption.REPLACE_EXISTING);

        Process process = new ProcessBuilder(javaExecutable, "-Dfile.encoding=UTF-8", "-cp", classPath, "Compiler")
                .directory(caseWork.toFile())
                .redirectErrorStream(true)
                .start();
        String processOutput = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            fail(name, "Compiler exited with " + exitCode + ": " + processOutput);
        }

        String expectedError = normalize(Files.readString(caseRoot.resolve("error.txt")));
        String actualError = normalize(Files.readString(caseWork.resolve("error.txt")));
        if (!actualError.equals(expectedError)) {
            fail(name, "expected errors [" + expectedError + "] but got [" + actualError + "]");
        }

        if (expectedError.isEmpty()) {
            assertGenerated(caseWork, "llvm_ir.txt");
            assertGenerated(caseWork, "mips.txt");
        }
        assertCaseSpecificOutput(name, caseWork);
        passed++;
        System.out.println("[PASS] " + name);
    }

    private void assertCaseSpecificOutput(String name, Path caseWork) throws IOException {
        List<String> lexerLines = Files.readAllLines(caseWork.resolve("lexer.txt"));
        if (name.equals("scalar_calls")) {
            assertContains(name, lexerLines, "IDENFR get_int");
            assertContains(name, lexerLines, "IDENFR get_char");
            assertContains(name, lexerLines, "IDENFR put_int");
            assertContains(name, lexerLines, "IDENFR put_char");

            String ir = Files.readString(caseWork.resolve("llvm_ir.txt"));
            assertContains(name, ir, "declare i32 @get_int()");
            assertContains(name, ir, "declare i8 @get_char()");
            assertContains(name, ir, "declare void @put_int(i32)");
            assertContains(name, ir, "declare void @put_char(i8)");
            assertContains(name, ir, "call i32 @get_int()");
            assertContains(name, ir, "call i8 @get_char()");

            String symbols = Files.readString(caseWork.resolve("symbol.txt"));
            if (symbols.contains("get_int IntFunc") || symbols.contains("get_char CharFunc")
                    || symbols.contains("put_int VoidFunc") || symbols.contains("put_char VoidFunc")) {
                fail(name, "runtime functions leaked into symbol.txt");
            }
        } else if (name.equals("calls_in_expressions")) {
            String ir = Files.readString(caseWork.resolve("llvm_ir.txt"));
            long calls = countOccurrences(ir, "call i32 @get_int()");
            if (calls != 2) {
                fail(name, "expected 2 get_int calls in IR but got " + calls);
            }
        } else if (name.equals("legacy_names_are_identifiers")) {
            assertContains(name, lexerLines, "IDENFR getint");
            assertContains(name, lexerLines, "IDENFR getchar");
            if (lexerLines.stream().anyMatch(line -> line.startsWith("GETINTTK ")
                    || line.startsWith("GETCHARTK "))) {
                fail(name, "legacy input names are still reserved words");
            }
        }
    }

    private static void assertGenerated(Path caseWork, String fileName) throws IOException {
        Path file = caseWork.resolve(fileName);
        if (!Files.exists(file) || Files.size(file) == 0) {
            fail(caseWork.getFileName().toString(), fileName + " was not generated");
        }
    }

    private static void assertContains(String name, List<String> lines, String expected) {
        if (!lines.contains(expected)) {
            fail(name, "missing line: " + expected);
        }
    }

    private static void assertContains(String name, String text, String expected) {
        if (!text.contains(expected)) {
            fail(name, "missing text: " + expected);
        }
    }

    private static long countOccurrences(String text, String expected) {
        return text.lines().filter(line -> line.contains(expected)).count();
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n").strip();
    }

    private static void fail(String name, String message) {
        throw new AssertionError("[FAIL] " + name + ": " + message);
    }
}
