# M1 回归测试

运行：

```powershell
.\tests\m1\run.cmd
```

脚本直接使用 JDK 17 的 `javac`，不需要下载 Maven 插件。编译产物和编译器输出均写入系统临时目录，运行结束后自动删除，不会覆盖仓库根目录中的 `testfile.txt` 或输出文件。

这组测试只验收 M1 已完成的边界：

- `lexer/2026_keywords`：用一份合法的 2026 SysY 程序检查 `while`、`switch`、`case`、`default`、`static` 和 `:` 的 Token。
- `lexer/keyword_boundaries`：检查包含关键字前缀的合法标识符不会被误识别为关键字。
- `error/*`：用课程允许的错误形式检查 `ErrorCode` 枚举迁移后，已有错误码仍按“行号 + 字母”输出。
- `compiler_reset`：在同一个 JVM 中连续调用两次 `Compiler.main`，检查第二次编译不会残留第一次的错误。

`while`、`switch` 和 `static` 当前只做词法验收。它们的 Parser、语义分析和代码生成尚未实现，不属于 M1 的通过条件。
