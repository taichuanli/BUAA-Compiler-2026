import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class M2RegressionTest {
    private final Path casesRoot;
    private final Path workRoot;
    private final String javaExecutable;
    private final String classPath;
    private int passed;

    private M2RegressionTest(Path repoRoot) {
        this.casesRoot = repoRoot.resolve("tests/m2/cases");
        this.workRoot = Path.of("").toAbsolutePath();
        this.javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
        this.classPath = System.getProperty("java.class.path");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: M2RegressionTest <repo-root>");
        }
        new M2RegressionTest(Path.of(args[0]).toAbsolutePath()).run();
    }

    private void run() throws Exception {
        runCase("while_break_continue");
        runCase("nested_while");
        runCase("for_is_identifier");
        runCase("missing_right_parenthesis");
        runCase("invalid_control_flow");
        System.out.printf("M2 regression tests: %d/5 passed.%n", passed);
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
        if (name.equals("while_break_continue")) {
            assertCount(name, lexerLines, "WHILETK while", 2);
            String ir = Files.readString(caseWork.resolve("llvm_ir.txt"));
            if (!ir.contains("br i1") || !ir.contains("br label")) {
                fail(name, "generated IR does not contain conditional and direct loop branches");
            }
        } else if (name.equals("nested_while")) {
            assertCount(name, lexerLines, "WHILETK while", 2);
        } else if (name.equals("for_is_identifier")) {
            assertCount(name, lexerLines, "IDENFR for", 2);
            if (lexerLines.stream().anyMatch(line -> line.startsWith("FORTK "))) {
                fail(name, "for is still treated as a reserved word");
            }
        }
    }

    private static void assertGenerated(Path caseWork, String fileName) throws IOException {
        Path file = caseWork.resolve(fileName);
        if (!Files.exists(file) || Files.size(file) == 0) {
            fail(caseWork.getFileName().toString(), fileName + " was not generated");
        }
    }

    private static void assertCount(String name, List<String> lines, String token, long expected) {
        long actual = lines.stream().filter(token::equals).count();
        if (actual != expected) {
            fail(name, "expected " + expected + " occurrences of [" + token + "] but got " + actual);
        }
    }

    private static String normalize(String text) {
        return text.replace("\r\n", "\n").strip();
    }

    private static void fail(String name, String message) {
        throw new AssertionError("[FAIL] " + name + ": " + message);
    }
}
