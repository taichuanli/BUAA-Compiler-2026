import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Executes generated MIPS so arithmetic and control-flow regressions cannot pass on IR shape alone. */
public final class CorrectnessRegressionTest {
    private final Path repoRoot;
    private final Path workRoot = Path.of("").toAbsolutePath();
    private final String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
    private final String classPath = System.getProperty("java.class.path");
    private List<String> marsOptions;

    private CorrectnessRegressionTest(Path repoRoot) {
        this.repoRoot = repoRoot;
    }

    public static void main(String[] args) throws Exception {
        new CorrectnessRegressionTest(Path.of(args[0]).toAbsolutePath()).run();
    }

    private void run() throws Exception {
        Path mars = findMars();
        String help = runProcess("MARS CLI", List.of(javaExecutable, "-jar", mars.toString(), "--help"),
                workRoot, null, "mars-help.log");
        marsOptions = help.contains("--no-copyright")
                ? List.of("--no-copyright", "--assemble-error-exit-code=1", "--simulate-error-exit-code=1")
                : List.of("nc", "ae1", "se1");
        int passed = 0;
        List<String> cases = List.of("zero_subtraction", "arithmetic_side_effects", "input_consumption",
                "switch_if_conditions", "switch_while_conditions");
        for (String name : cases) {
            try {
                runCase(name, mars);
                passed++;
                System.out.println("[PASS] " + name + " (MARS)");
            } catch (AssertionError failure) {
                System.out.println(failure.getMessage());
            }
        }
        System.out.printf("Correctness runtime regression tests: %d/%d passed.%n", passed, cases.size());
        if (passed != cases.size()) {
            throw new AssertionError("Runtime correctness regressions failed");
        }
    }

    private Path findMars() {
        String configured = System.getenv("MARS_JAR");
        if (configured != null && !configured.isBlank()) {
            Path path = Path.of(configured).toAbsolutePath();
            if (!Files.isRegularFile(path)) {
                throw new IllegalArgumentException("MARS_JAR does not name a file: " + path);
            }
            return path;
        }
        for (Path root : List.of(repoRoot.resolve("repos"), repoRoot.getParent())) {
            Path courseMars = root.resolve("testcase-2026/mars-2026-BUAA.jar");
            if (Files.isRegularFile(courseMars)) {
                return courseMars;
            }
            Path path = root.resolve("reference-2026-saitewasreset/src/test/resources/TargetCodeGeneration/tool/MARS2025+.jar");
            if (Files.isRegularFile(path)) {
                return path;
            }
        }
        Path originalMars = repoRoot.resolve("target/tools/Mars4_5.jar");
        if (Files.isRegularFile(originalMars)) {
            return originalMars;
        }
        throw new IllegalStateException("MARS is required; set MARS_JAR to the simulator jar");
    }

    private void runCase(String name, Path mars) throws Exception {
        Path source = repoRoot.resolve("tests/correctness/cases").resolve(name);
        Path work = Files.createDirectories(workRoot.resolve(name));
        Files.copy(source.resolve("testfile.txt"), work.resolve("testfile.txt"), StandardCopyOption.REPLACE_EXISTING);
        runProcess(name, List.of(javaExecutable, "-Dfile.encoding=UTF-8", "-cp", classPath, "Compiler"),
                work, null, "compiler.log");
        String errors = Files.readString(work.resolve("error.txt")).strip();
        if (!errors.isEmpty()) {
            fail(name, "unexpected compiler errors: " + errors);
        }
        Path input = Files.isRegularFile(source.resolve("in.txt")) ? source.resolve("in.txt") : null;
        java.util.ArrayList<String> command = new java.util.ArrayList<>(
                List.of(javaExecutable, "-Djava.awt.headless=true", "-jar", mars.toString()));
        command.addAll(marsOptions);
        command.add("mips.txt");
        String actual = runProcess(name, command, work, input, "runtime.log");
        String expected = normalize(Files.readString(source.resolve("expected.txt")));
        if (!expected.equals(normalize(actual))) {
            fail(name, "expected [" + expected + "] but got [" + normalize(actual) + "]");
        }
    }

    private static String runProcess(String name, List<String> command, Path work, Path input, String log)
            throws Exception {
        Path output = work.resolve(log);
        ProcessBuilder builder = new ProcessBuilder(command).directory(work.toFile())
                .redirectErrorStream(true).redirectOutput(output.toFile());
        if (input != null) {
            builder.redirectInput(input.toFile());
        }
        Process process = builder.start();
        process.getOutputStream().close();
        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor();
            fail(name, "process exceeded 15 seconds: " + command);
        }
        String text = Files.readString(output);
        if (process.exitValue() != 0) {
            fail(name, "process exited with " + process.exitValue() + ": " + text);
        }
        return text;
    }

    private static String normalize(String value) {
        return value.replace("\r\n", "\n").stripTrailing();
    }

    private static void fail(String name, String message) {
        throw new AssertionError("[FAIL] " + name + ": " + message);
    }
}
