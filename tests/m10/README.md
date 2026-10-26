# M10 final compatibility audit

Run `tests\m10\run.cmd` from any directory. The fixed suite executes three
representative 2026 programs with MARS when the workspace reference jar is
available. If the sibling `testcase-2026` repository and GCC are available, it
also compares all official error cases and verifies that all official 2026
correct programs are accepted after preprocessing with `SUPER_PRODUCTION`.

The million-iteration `cr/perf` program is compiled in the official audit but
is not run by this suite. Its reduced-input runtime behavior is covered during
the M10 release audit; full input is intentionally left to performance testing.

Repositories under `repos/` are discovered before sibling repositories. The
course `testcase-2026/mars-2026-BUAA.jar`, both MARS CLI versions, and an explicit
`MARS_JAR` override are supported. GCC may be native Windows GCC or an existing
WSL Ubuntu/Debian installation. For the current repository layout, only
`error/err_2026` is audited; older error-regression suites are excluded.
