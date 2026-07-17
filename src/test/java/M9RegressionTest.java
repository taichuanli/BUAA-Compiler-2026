import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class M9RegressionTest {
    private static final Pattern ADD_IMMEDIATE = Pattern.compile("^addiu\\s+\\$\\w+,\\s*\\$\\w+,\\s*(-?\\d+)$");
    private static final Pattern MEMORY_OFFSET = Pattern.compile("^(?:lw|sw|lbu|sb)\\s+\\$\\w+,\\s*(-?\\d+)\\(\\$\\w+\\)$");
    private final Path casesRoot;
    private final Path workRoot = Path.of("").toAbsolutePath();
    private final String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
    private final String classPath = System.getProperty("java.class.path");
    private int passed;

    private M9RegressionTest(Path repoRoot) {
        casesRoot = repoRoot.resolve("tests/m9/cases");
    }

    public static void main(String[] args) throws Exception {
        new M9RegressionTest(Path.of(args[0]).toAbsolutePath()).run();
    }

    private void run() throws Exception {
        for (String name : List.of("large_int_stack", "large_char_stack", "large_constant")) {
            runProcessCase(name);
        }
        runSameJvmCase();
        System.out.printf("M9 regression tests: %d/4 passed.%n", passed);
    }

    private void runProcessCase(String name) throws Exception {
        Path source = casesRoot.resolve(name);
        Path work = Files.createDirectories(workRoot.resolve(name));
        Files.copy(source.resolve("testfile.txt"), work.resolve("testfile.txt"), StandardCopyOption.REPLACE_EXISTING);
        Process process = new ProcessBuilder(javaExecutable, "-Dfile.encoding=UTF-8", "-cp", classPath, "Compiler")
                .directory(work.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        if (process.waitFor() != 0) {
            fail(name, "compiler process failed: " + output);
        }
        assertValidOutput(name, work);
        if (!name.equals("large_constant") && !Files.readString(work.resolve("mips.txt")).contains("li $at, -")) {
            fail(name, "case did not exercise expanded large stack offsets");
        }
        pass(name);
    }

    private void runSameJvmCase() throws Exception {
        Path source = casesRoot.resolve("same_jvm");
        Path input = workRoot.resolve("testfile.txt");
        Files.copy(source.resolve("first.txt"), input, StandardCopyOption.REPLACE_EXISTING);
        Compiler.main(new String[0]);
        assertValidOutput("same_jvm_first", workRoot);
        String first = Files.readString(workRoot.resolve("mips.txt"));
        if (!first.contains("runtime_get_string_done_0")) {
            fail("same_jvm", "first compilation did not create the expected runtime label");
        }

        Files.copy(source.resolve("second.txt"), input, StandardCopyOption.REPLACE_EXISTING);
        Compiler.main(new String[0]);
        assertValidOutput("same_jvm_second", workRoot);
        String second = Files.readString(workRoot.resolve("mips.txt"));
        if (!second.contains("runtime_get_string_done_0") || second.contains("runtime_get_string_done_1")) {
            fail("same_jvm", "runtime labels or backend allocation state leaked across compilations");
        }
        pass("same_jvm_reset");
    }

    private static void assertValidOutput(String name, Path work) throws Exception {
        String errors = Files.readString(work.resolve("error.txt")).strip();
        if (!errors.isEmpty()) {
            fail(name, "unexpected errors: " + errors);
        }
        for (String rawLine : Files.readAllLines(work.resolve("mips.txt"))) {
            String line = rawLine.strip();
            Matcher add = ADD_IMMEDIATE.matcher(line);
            Matcher memory = MEMORY_OFFSET.matcher(line);
            if (add.matches() && !fitsSigned16(Integer.parseInt(add.group(1)))) {
                fail(name, "out-of-range addiu remained: " + line);
            }
            if (memory.matches() && !fitsSigned16(Integer.parseInt(memory.group(1)))) {
                fail(name, "out-of-range memory offset remained: " + line);
            }
        }
    }

    private static boolean fitsSigned16(int value) {
        return value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
    }

    private void pass(String name) {
        passed++;
        System.out.println("[PASS] " + name);
    }

    private static void fail(String name, String message) {
        throw new AssertionError("[FAIL] " + name + ": " + message);
    }
}
