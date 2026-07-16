import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class M7RegressionTest {
    private final Path casesRoot;
    private final Path workRoot = Path.of("").toAbsolutePath();
    private final String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
    private final String classPath = System.getProperty("java.class.path");
    private int passed;

    private M7RegressionTest(Path repoRoot) {
        casesRoot = repoRoot.resolve("tests/m7/cases");
    }

    public static void main(String[] args) throws Exception {
        new M7RegressionTest(Path.of(args[0]).toAbsolutePath()).run();
    }

    private void run() throws Exception {
        for (String name : List.of("printf_and_put", "get_string", "truncated_input",
                "zero_length", "string_type_errors")) {
            runCase(name);
        }
        System.out.printf("M7 regression tests: %d/5 passed.%n", passed);
    }

    private void runCase(String name) throws Exception {
        Path source = casesRoot.resolve(name);
        Path work = Files.createDirectories(workRoot.resolve(name));
        Files.copy(source.resolve("testfile.txt"), work.resolve("testfile.txt"), StandardCopyOption.REPLACE_EXISTING);
        Process process = new ProcessBuilder(javaExecutable, "-Dfile.encoding=UTF-8", "-cp", classPath, "Compiler")
                .directory(work.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        if (process.waitFor() != 0) {
            fail(name, "compiler process failed: " + output);
        }
        String expected = normalize(Files.readString(source.resolve("error.txt")));
        String actual = normalize(Files.readString(work.resolve("error.txt")));
        if (!expected.equals(actual)) {
            fail(name, "expected [" + expected + "] but got [" + actual + "]");
        }
        if (expected.isEmpty()) {
            String ir = Files.readString(work.resolve("llvm_ir.txt"));
            String mips = Files.readString(work.resolve("mips.txt"));
            if (!ir.contains("declare void @get_string(i8*, i32)") || !ir.contains("@put_string(i8*")) {
                fail(name, "string runtime declarations/calls are missing");
            }
            if (name.contains("get_string") || name.equals("truncated_input") || name.equals("zero_length")) {
                if (!mips.contains("li $v0, 8") || !mips.contains("blez $a1")) {
                    fail(name, "syscall 8 or max_len guard is missing");
                }
            }
        }
        passed++;
        System.out.println("[PASS] " + name);
    }

    private static String normalize(String value) {
        return value.replace("\r\n", "\n").strip();
    }

    private static void fail(String name, String message) {
        throw new AssertionError("[FAIL] " + name + ": " + message);
    }
}
