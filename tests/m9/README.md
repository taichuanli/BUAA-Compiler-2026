# M9 backend boundary regression tests

Run `tests\m9\run.cmd` from any directory. The suite crosses the signed 16-bit
immediate boundary with large int and char stack arrays, checks large constants
and calls with more than four arguments, and invokes `Compiler.main` twice in
one JVM to detect leaked backend state.
