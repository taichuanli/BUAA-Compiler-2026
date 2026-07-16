import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class M4RegressionTest {
    private final Path casesRoot;
    private final Path workRoot = Path.of("").toAbsolutePath();
    private final String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
    private final String classPath = System.getProperty("java.class.path");
    private int passed;

    private M4RegressionTest(Path repoRoot) {
        casesRoot = repoRoot.resolve("tests/m4/cases");
    }

    public static void main(String[] args) throws Exception {
        new M4RegressionTest(Path.of(args[0]).toAbsolutePath()).run();
    }

    private void run() throws Exception {
        List<String> cases = List.of("explicit_casts", "missing_cast_rparen", "mixed_scalars",
                "array_access", "function_types", "printf_types", "char_array_init", "identifier_misuse");
        for (String name : cases) {
            runCase(name);
        }
        System.out.printf("M4 regression tests: %d/%d passed.%n", passed, cases.size());
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
        if (name.equals("explicit_casts")) {
            String ir = Files.readString(work.resolve("llvm_ir.txt"));
            if (!ir.contains("trunc i32") || !ir.contains("zext i8")) {
                fail(name, "explicit casts did not produce trunc/zext IR");
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
