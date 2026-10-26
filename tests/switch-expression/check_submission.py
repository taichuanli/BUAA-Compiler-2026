"""Compile and run the submitted source ZIP in isolation, then cross-check old grammar."""
import os
from pathlib import Path, PurePosixPath
import subprocess
import tempfile
import zipfile

REPO = Path(__file__).resolve().parents[2]
TARGET = (REPO / "target").resolve()
JAVA_HOME = Path(os.environ["JAVA_HOME"])
JAVA = JAVA_HOME / "bin/java.exe"
JAVAC = JAVA_HOME / "bin/javac.exe"
ARCHIVE = TARGET / "switch-expression-submission.zip"


def run(command, work):
    result = subprocess.run([str(arg) for arg in command], cwd=work, stdin=subprocess.DEVNULL,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=60)
    if result.returncode:
        raise AssertionError(result.stdout.decode("utf-8", errors="replace"))
    return result.stdout.decode("utf-8", errors="replace").replace("\r\n", "\n")


def normalized(path):
    return path.read_text(encoding="utf-8").replace("\r\n", "\n").rstrip()


with tempfile.TemporaryDirectory(prefix="switch-submission-", dir=TARGET) as temporary:
    work = Path(temporary).resolve()
    assert work.is_relative_to(TARGET)
    with zipfile.ZipFile(ARCHIVE) as archive:
        names = archive.namelist()
        assert len(names) == 11, names
        assert {name for name in names if "/" not in name} == {"Compiler.java", "config.json"}
        assert all(not PurePosixPath(name).is_absolute() and ".." not in PurePosixPath(name).parts for name in names)
        assert all(name.endswith(".java") or name == "config.json" for name in names)
        archive.extractall(work / "source")
    sources = sorted((work / "source").rglob("*.java"))
    classes = work / "classes"
    classes.mkdir()
    run([JAVAC, "--release", "17", "-encoding", "UTF-8", "-d", classes, *sources,
         REPO / "src/test/java/SwitchExpressionRegressionTest.java"], work)
    execution = work / "execution"
    execution.mkdir()
    print(run([JAVA, "-Dfile.encoding=UTF-8", "-cp", classes, "SwitchExpressionRegressionTest", REPO], execution))
    print("[PASS] ZIP-only compilation and all 22 regressions")

    reference = REPO / "repos/testcase-2026/modified_grammar/case/midterm_pre/reference/super-compiler-1.0-SNAPSHOT.jar"
    if reference.is_file():
        for number in range(1, 6):
            fixture = REPO / f"tests/switch-expression/cases/testcase{number}"
            output = work / f"reference-{number}"
            output.mkdir()
            run([JAVA, "-jar", reference, "-p", "Syntax", fixture / "testfile.txt",
                 "--lexical-output", output / "lexer.txt", "--syntax-output", output / "parser.txt",
                 "--error-output", output / "error.txt"], output)
            expected_error = normalized(fixture / "error.txt")
            actual_error = normalized(output / "error.txt") if (output / "error.txt").exists() else ""
            assert expected_error == actual_error, f"reference error mismatch in testcase{number}"
            if not expected_error:
                for name in ("lexer.txt", "parser.txt"):
                    assert normalized(fixture / name) == normalized(output / name), f"reference {name} mismatch in testcase{number}"
        print("[PASS] independent reference compiler: unchanged grammar 5/5")
    else:
        print("[SKIP] optional reference compiler is unavailable")
    assert work.is_relative_to(TARGET)  # Verify the recursive cleanup remains inside this workspace.
