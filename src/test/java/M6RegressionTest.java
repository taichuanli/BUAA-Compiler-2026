import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class M6RegressionTest {
    private final Path casesRoot;
    private final Path workRoot = Path.of("").toAbsolutePath();
    private final String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
    private final String classPath = System.getProperty("java.class.path");
    private int passed;

    private M6RegressionTest(Path repoRoot) {
        casesRoot = repoRoot.resolve("tests/m6/cases");
    }

    public static void main(String[] args) throws Exception {
        new M6RegressionTest(Path.of(args[0]).toAbsolutePath()).run();
    }

    private void run() throws Exception {
        for (String name : List.of("local_bytes", "global_bytes", "array_parameter",
                "mixed_stack_alignment", "adjacent_writes", "char_arithmetic")) {
            runCase(name);
        }
        System.out.printf("M6 regression tests: %d/6 passed.%n", passed);
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
        String errors = Files.readString(work.resolve("error.txt")).strip();
        if (!errors.isEmpty()) {
            fail(name, "unexpected errors: " + errors);
        }
        String mips = Files.readString(work.resolve("mips.txt"));
        if (!mips.contains("lbu ") || !mips.contains("sb ")) {
            fail(name, "byte load/store instructions are missing");
        }
        if (name.equals("global_bytes") && !mips.contains("global_chars: .byte")) {
            fail(name, "global char array was not emitted with .byte");
        }
        passed++;
        System.out.println("[PASS] " + name);
    }

    private static void fail(String name, String message) {
        throw new AssertionError("[FAIL] " + name + ": " + message);
    }
}
