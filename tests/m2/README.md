# M2 回归测试

运行：

```powershell
.\tests\m2\run.cmd
```

这组测试验收 `while` 的完整链路：

- Parser 能识别 `while '(' Cond ')' Stmt`，并对缺少右小括号报告 `j`。
- Visitor 允许 `while` 内的 `break` 和 `continue`，在循环外仍报告 `m`。
- IR 为基本循环、嵌套循环、`break` 和 `continue` 生成正确类型的分支与标签。
- MIPS 文件能够由既有 IR 降低流程生成。
- 删除旧 `for` 后，`for` 按 2026 文法重新作为普通标识符使用。

每个完整编译用例都在独立 JVM 和独立临时目录中运行，避免跨用例共享编译器单例状态。
