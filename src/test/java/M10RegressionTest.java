import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class M10RegressionTest {
    private final Path repoRoot;
    private final Path casesRoot;
    private final Path workRoot = Path.of("").toAbsolutePath();
    private final String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java.exe").toString();
    private final String classPath = System.getProperty("java.class.path");
    private int passed;

    private M10RegressionTest(Path repoRoot) {
        this.repoRoot = repoRoot;
        this.casesRoot = repoRoot.resolve("tests/m10/cases");
    }

    public static void main(String[] args) throws Exception {
        new M10RegressionTest(Path.of(args[0]).toAbsolutePath()).run();
    }

    private void run() throws Exception {
        Path mars = repoRoot.getParent().resolve(
                "reference-2026-saitewasreset/src/test/resources/TargetCodeGeneration/tool/MARS2025+.jar");
        for (String name : List.of("escape_literals", "short_circuit", "full_grammar")) {
            runFixedCase(name, Files.isRegularFile(mars) ? mars : null);
        }
        auditOfficialRepository();
        System.out.printf("M10 fixed regression tests: %d/3 passed.%n", passed);
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
            ProcessResult result = runProcess(List.of(javaExecutable, "-jar", mars.toString(), "nc", "mips.txt"), work);
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
        Path testcaseRoot = repoRoot.getParent().resolve("testcase-2026");
        if (!Files.isDirectory(testcaseRoot)) {
            System.out.println("[SKIP] sibling testcase-2026 repository is unavailable");
            return;
        }
        auditOfficialErrors(testcaseRoot);
        Path gcc = findGcc();
        if (gcc == null) {
            System.out.println("[SKIP] GCC is unavailable; official correct-source preprocessing was skipped");
            return;
        }
        auditOfficialCorrectPrograms(testcaseRoot, gcc);
    }

    private void auditOfficialErrors(Path testcaseRoot) throws Exception {
        Path root = testcaseRoot.resolve("error");
        List<Path> answers;
        try (Stream<Path> paths = Files.walk(root)) {
            answers = paths.filter(path -> path.getFileName().toString().equals("error.txt"))
                    .sorted(Comparator.comparing(Path::toString)).toList();
        }
        Path work = Files.createDirectories(workRoot.resolve("official-error"));
        for (Path answer : answers) {
            String name = root.relativize(answer.getParent()).toString();
            runCompiler(answer.getParent().resolve("testfile.txt"), work, name);
            String expected = normalize(Files.readString(answer));
            String actual = normalize(Files.readString(work.resolve("error.txt")));
            if (!expected.equals(actual)) {
                fail(name, "official errors expected [" + expected + "] but got [" + actual + "]");
            }
        }
        System.out.printf("[PASS] official error audit: %d/%d matched.%n", answers.size(), answers.size());
    }

    private void auditOfficialCorrectPrograms(Path testcaseRoot, Path gcc) throws Exception {
        Path root = testcaseRoot.resolve("testcase/2026");
        List<Path> sources;
        try (Stream<Path> paths = Files.walk(root)) {
            sources = paths.filter(path -> path.getFileName().toString().equals("testfile.c"))
                    .sorted(Comparator.comparing(Path::toString)).toList();
        }
        Path work = Files.createDirectories(workRoot.resolve("official-correct"));
        for (Path source : sources) {
            String name = root.relativize(source.getParent()).toString();
            ProcessResult preprocessor = runProcess(List.of(gcc.toString(), "-E", "-P", "-DSUPER_PRODUCTION",
                    source.toString(), "-o", work.resolve("testfile.txt").toString()), work);
            if (preprocessor.exitCode != 0) {
                fail(name, "GCC preprocessing failed: " + preprocessor.output);
            }
            runCompiler(null, work, name);
            assertNoErrors(name, work);
        }
        System.out.printf("[PASS] official correct-source audit: %d/%d accepted.%n", sources.size(), sources.size());
    }

    private Path findGcc() throws Exception {
        Path known = Path.of("C:/mingw64/bin/gcc.exe");
        if (Files.isExecutable(known)) {
            return known;
        }
        ProcessResult result = runProcess(List.of("where.exe", "gcc.exe"), workRoot);
        if (result.exitCode != 0 || result.output.isBlank()) {
            return null;
        }
        Path candidate = Path.of(result.output.lines().findFirst().orElse("").strip());
        return Files.isExecutable(candidate) ? candidate : null;
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
        Process process = new ProcessBuilder(command).directory(work.toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        return new ProcessResult(process.waitFor(), output);
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
}
