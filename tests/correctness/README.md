# Correctness runtime regression

Run `tests\correctness\run.cmd`. This suite compiles each source and compares
its actual MIPS output with `expected.txt`; it fails if MARS is unavailable.
Each compiler or simulator process has a 15-second timeout.

The five cases cover integer zero subtraction, function-call side effects in
multiply-by-zero and remainder-by-one expressions (including nested expressions
and returns), input consumption, and compound conditions inside switch cases,
blocks, nested switches and while statements.

MARS is discovered in this order:

1. The `MARS_JAR` environment variable, when set.
2. `repos/testcase-2026/mars-2026-BUAA.jar` and the reference compiler's
   `MARS2025+.jar`; sibling repositories are also supported.
3. `target/tools/Mars4_5.jar`, for an original MARS installation.

Both the original MARS CLI and the 2026-BUAA long-option CLI are supported.
These tests reproduced 0/5 passes before the fixes and pass 5/5 after them.
