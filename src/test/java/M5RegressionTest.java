import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class M5RegressionTest {
    private final Path casesRoot;
    private final Path workRoot = Path.of("").toAbsolutePath();
    private final String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
    private final String classPath = System.getProperty("java.class.path");
    private int passed;

    private M5RegressionTest(Path repoRoot) {
        casesRoot = repoRoot.resolve("tests/m5/cases");
    }

    public static void main(String[] args) throws Exception {
        new M5RegressionTest(Path.of(args[0]).toAbsolutePath()).run();
    }

    private void run() throws Exception {
        for (String name : List.of("fallthrough", "char_switch", "nested_control",
                "duplicate_labels", "case_type_mismatch", "invalid_control")) {
            runCase(name);
        }
        System.out.printf("M5 regression tests: %d/6 passed.%n", passed);
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
            if (!ir.contains("icmp eq") || !ir.contains("br i1")) {
                fail(name, "switch comparison/branch IR is missing");
            }
        }
        if (name.equals("char_switch")) {
            String parser = Files.readString(work.resolve("parser.txt"));
            if (!parser.contains("<Number>") || parser.contains("<Character>")) {
                fail(name, "CharConst was not normalized to the 2026 Number node");
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
