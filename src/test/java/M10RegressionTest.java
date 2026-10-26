import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class M10RegressionTest {
    private final Path repoRoot;
    private final Path casesRoot;
    private final Path workRoot = Path.of("").toAbsolutePath();
    private final String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
    private final String classPath = System.getProperty("java.class.path");
    private List<String> marsOptions;
    private int passed;

    private M10RegressionTest(Path repoRoot) {
        this.repoRoot = repoRoot;
        this.casesRoot = repoRoot.resolve("tests/m10/cases");
    }

    public static void main(String[] args) throws Exception {
        new M10RegressionTest(Path.of(args[0]).toAbsolutePath()).run();
    }

    private void run() throws Exception {
        Path mars = findMars();
        if (mars != null) {
            ProcessResult help = runProcess(List.of(javaExecutable, "-jar", mars.toString(), "--help"), workRoot);
            marsOptions = help.output.contains("--no-copyright")
                    ? List.of("--no-copyright", "--assemble-error-exit-code=1", "--simulate-error-exit-code=1")
                    : List.of("nc", "ae1", "se1");
        }
        for (String name : List.of("escape_literals", "short_circuit", "full_grammar")) {
            runFixedCase(name, mars);
        }
        auditOfficialRepository();
        System.out.printf("M10 fixed regression tests: %d/3 passed.%n", passed);
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
            for (String relative : List.of("testcase-2026/mars-2026-BUAA.jar",
                    "reference-2026-saitewasreset/src/test/resources/TargetCodeGeneration/tool/MARS2025+.jar")) {
                Path path = root.resolve(relative);
                if (Files.isRegularFile(path)) {
                    return path;
                }
            }
        }
        Path originalMars = repoRoot.resolve("target/tools/Mars4_5.jar");
        return Files.isRegularFile(originalMars) ? originalMars : null;
    }

    private void runFixedCase(String name, Path mars) throws Exception {
        Path source = casesRoot.resolve(name);
        Path work = Files.createDirectories(workRoot.resolve("fixed").resolve(name));
        runCompiler(source.resolve("testfile.txt"), work, name);
        assertNoErrors(name, work);
        if (name.equals("escape_literals")) {
            String ir = Files.readString(work.resolve("llvm_ir.txt"));
            if (!ir.contains("i8 13")) {
                fail(name, "carriage-return character was not lowered to value 13");
            }
        }
        if (mars != null) {
            List<String> command = new ArrayList<>(List.of(javaExecutable, "-jar", mars.toString()));
            command.addAll(marsOptions);
            command.add("mips.txt");
            ProcessResult result = runProcess(command, work);
            if (result.exitCode != 0) {
                fail(name, "MARS failed: " + result.output);
            }
            String expected = normalizeRuntime(Files.readString(source.resolve("expected.txt")));
            String actual = normalizeRuntime(result.output);
            if (!expected.equals(actual)) {
                fail(name, "expected runtime output [" + expected + "] but got [" + actual + "]");
            }
        }
        passed++;
        System.out.println("[PASS] " + name + (mars == null ? " (compile only; MARS unavailable)" : " (MARS)"));
    }

    private void auditOfficialRepository() throws Exception {
        Path testcaseRoot = repoRoot.resolve("repos/testcase-2026");
        if (!Files.isDirectory(testcaseRoot)) {
            testcaseRoot = repoRoot.getParent().resolve("testcase-2026");
        }
        if (!Files.isDirectory(testcaseRoot)) {
            System.out.println("[SKIP] sibling testcase-2026 repository is unavailable");
            return;
        }
        auditOfficialErrors(testcaseRoot);
        CCompiler gcc = findGcc();
        if (gcc == null) {
            System.out.println("[SKIP] GCC is unavailable; official correct-source preprocessing was skipped");
            return;
        }
        auditOfficialCorrectPrograms(testcaseRoot, gcc);
    }

    private void auditOfficialErrors(Path testcaseRoot) throws Exception {
        Path root = testcaseRoot.resolve("error");
        if (Files.isDirectory(root.resolve("err_2026"))) {
            root = root.resolve("err_2026");
        }
        List<Path> answers;
        try (Stream<Path> paths = Files.walk(root)) {
            answers = paths.filter(path -> path.getFileName().toString().equals("error.txt"))
                    .sorted(Comparator.comparing(Path::toString)).toList();
        }
        Path work = Files.createDirectories(workRoot.resolve("official-error"));
        int checked = 0;
        for (Path answer : answers) {
            String name = root.relativize(answer.getParent()).toString();
            runCompiler(answer.getParent().resolve("testfile.txt"), work, name);
            String expected = normalize(Files.readString(answer));
            String actual = normalize(Files.readString(work.resolve("error.txt")));
            if (!expected.equals(actual)) {
                fail(name, "official errors expected [" + expected + "] but got [" + actual + "]");
            }
            if (++checked % 100 == 0) {
                System.out.printf("[INFO] official errors checked: %d/%d%n", checked, answers.size());
            }
        }
        System.out.printf("[PASS] official error audit: %d/%d matched.%n", answers.size(), answers.size());
    }

    private void auditOfficialCorrectPrograms(Path testcaseRoot, CCompiler gcc) throws Exception {
        Path root = testcaseRoot.resolve("testcase/2026");
        List<Path> sources;
        try (Stream<Path> paths = Files.walk(root)) {
            sources = paths.filter(path -> path.getFileName().toString().equals("testfile.c"))
                    .sorted(Comparator.comparing(Path::toString)).toList();
        }
        Path work = Files.createDirectories(workRoot.resolve("official-correct"));
        int checked = 0;
        for (Path source : sources) {
            String name = root.relativize(source.getParent()).toString();
            List<String> command = new ArrayList<>(gcc.command());
            command.addAll(List.of("-E", "-P", "-DSUPER_PRODUCTION", compilerPath(gcc, source),
                    "-o", compilerPath(gcc, work.resolve("testfile.txt"))));
            ProcessResult preprocessor = runProcess(command, work);
            if (preprocessor.exitCode != 0) {
                fail(name, "GCC preprocessing failed: " + preprocessor.output);
            }
            runCompiler(null, work, name);
            assertNoErrors(name, work);
            if (++checked % 100 == 0) {
                System.out.printf("[INFO] official correct sources checked: %d/%d%n", checked, sources.size());
            }
        }
        System.out.printf("[PASS] official correct-source audit: %d/%d accepted.%n", sources.size(), sources.size());
    }

    private CCompiler findGcc() throws Exception {
        Path known = Path.of("C:/mingw64/bin/gcc.exe");
        if (Files.isExecutable(known)) {
            return new CCompiler(List.of(known.toString()), null);
        }
        ProcessResult result = runProcess(List.of("where.exe", "gcc.exe"), workRoot);
        if (result.exitCode == 0 && !result.output.isBlank()) {
            Path candidate = Path.of(result.output.lines().findFirst().orElse("").strip());
            if (Files.isExecutable(candidate)) {
                return new CCompiler(List.of(candidate.toString()), null);
            }
        }
        ProcessResult distributions = runProcess(List.of("wsl.exe", "--list", "--quiet"), workRoot);
        for (String raw : distributions.output.replace("\u0000", "").split("\\R")) {
            String distro = raw.strip();
            if (!distro.startsWith("Ubuntu") && !distro.startsWith("Debian")) {
                continue;
            }
            List<String> command = List.of("wsl.exe", "-d", distro, "--exec", "gcc");
            List<String> version = new ArrayList<>(command);
            version.add("--version");
            if (runProcess(version, workRoot).exitCode == 0) {
                System.out.println("[INFO] using GCC in WSL distribution " + distro);
                return new CCompiler(command, distro);
            }
        }
        return null;
    }

    private String compilerPath(CCompiler compiler, Path path) throws Exception {
        if (compiler.wslDistribution() == null) {
            return path.toString();
        }
        ProcessResult result = runProcess(List.of("wsl.exe", "-d", compiler.wslDistribution(),
                "--exec", "wslpath", "-a", path.toAbsolutePath().toString()), workRoot);
        if (result.exitCode != 0) {
            throw new IOException("Cannot translate Windows path for GCC: " + path);
        }
        return result.output.strip();
    }

    private void runCompiler(Path source, Path work, String name) throws Exception {
        if (source != null) {
            Files.copy(source, work.resolve("testfile.txt"), StandardCopyOption.REPLACE_EXISTING);
        }
        ProcessResult result = runProcess(List.of(javaExecutable, "-Dfile.encoding=UTF-8", "-cp", classPath, "Compiler"), work);
        if (result.exitCode != 0) {
            fail(name, "compiler process failed: " + result.output);
        }
    }

    private static ProcessResult runProcess(List<String> command, Path work) throws IOException, InterruptedException {
        Path log = Files.createTempFile(work, "process-", ".log");
        Process process = new ProcessBuilder(command).directory(work.toFile()).redirectErrorStream(true)
                .redirectOutput(log.toFile()).start();
        process.getOutputStream().close();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor();
            throw new IOException("Process exceeded 30 seconds: " + command);
        }
        byte[] bytes = Files.readAllBytes(log);
        String output = new String(bytes, StandardCharsets.UTF_8);
        if (output.indexOf('\u0000') >= 0) {
            output = new String(bytes, StandardCharsets.UTF_16LE).replace("\uFEFF", "");
        }
        Files.delete(log);
        return new ProcessResult(process.exitValue(), output);
    }

    private static void assertNoErrors(String name, Path work) throws Exception {
        String errors = normalize(Files.readString(work.resolve("error.txt")));
        if (!errors.isEmpty()) {
            fail(name, "unexpected errors: " + errors);
        }
    }

    private static String normalize(String value) {
        return value.replace("\r\n", "\n").strip();
    }

    private static String normalizeRuntime(String value) {
        return value.replace("\r\n", "\n").stripTrailing();
    }

    private static void fail(String name, String message) {
        throw new AssertionError("[FAIL] " + name + ": " + message);
    }

    private record ProcessResult(int exitCode, String output) {
    }

    private record CCompiler(List<String> command, String wslDistribution) {
    }
}
