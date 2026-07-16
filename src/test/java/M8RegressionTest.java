import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class M8RegressionTest {
    private final Path casesRoot;
    private final Path workRoot = Path.of("").toAbsolutePath();
    private final String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
    private final String classPath = System.getProperty("java.class.path");
    private int passed;

    private M8RegressionTest(Path repoRoot) {
        casesRoot = repoRoot.resolve("tests/m8/cases");
    }

    public static void main(String[] args) throws Exception {
        new M8RegressionTest(Path.of(args[0]).toAbsolutePath()).run();
    }

    private void run() throws Exception {
        for (String name : List.of("scalar_persistence", "multiple_and_zero", "char_persistence",
                "array_persistence", "nested_shadow", "constant_and_global")) {
            runCase(name);
        }
        System.out.printf("M8 regression tests: %d/6 passed.%n", passed);
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
        String errors = normalize(Files.readString(work.resolve("error.txt")));
        if (!errors.isEmpty()) {
            fail(name, "unexpected errors: " + errors);
        }
        String ir = Files.readString(work.resolve("llvm_ir.txt"));
        String mips = Files.readString(work.resolve("mips.txt"));
        int expectedStaticCount = name.equals("array_persistence") || name.equals("nested_shadow") ? 2
                : name.equals("multiple_and_zero") ? 2 : name.equals("constant_and_global") ? 1 : 1;
        if (count(ir, "@static_") < expectedStaticCount) {
            fail(name, "missing unique module-level static storage");
        }
        if (!mips.contains("static_")) {
            fail(name, "static storage is missing from MIPS data declarations");
        }
        if (name.equals("nested_shadow") && distinctStaticDefinitions(ir) < 2) {
            fail(name, "same source name in nested scopes did not receive distinct storage");
        }
        passed++;
        System.out.println("[PASS] " + name);
    }

    private static int count(String value, String needle) {
        int result = 0;
        for (int at = value.indexOf(needle); at >= 0; at = value.indexOf(needle, at + needle.length())) {
            result++;
        }
        return result;
    }

    private static int distinctStaticDefinitions(String ir) {
        return (int) ir.lines().filter(line -> line.startsWith("@static_") && line.contains(" = dso_local global ")).count();
    }

    private static String normalize(String value) {
        return value.replace("\r\n", "\n").strip();
    }

    private static void fail(String name, String message) {
        throw new AssertionError("[FAIL] " + name + ": " + message);
    }
}
