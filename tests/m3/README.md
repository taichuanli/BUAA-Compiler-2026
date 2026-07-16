# M3 回归测试

运行：

```powershell
.\tests\m3\run.cmd
```

这组测试验收 2026 标量运行时函数的普通调用链：

- `get_int`、`get_char`、`put_int`、`put_char` 均作为普通 `IDENFR` 解析。
- 内建函数可用于初始化、嵌套表达式和表达式语句，但不写入 `symbol.txt`。
- 参数个数不匹配继续报告 `d`，缺少右小括号继续报告 `j`。
- 旧名称 `getint`、`getchar` 不再是保留字，可作为普通标识符。
- LLVM IR 和 MIPS 使用 2026 运行时名称，复用已有系统调用降低。

`get_string`、用户直接调用的 `put_string`、`printf` 的 `%s` 和严格参数类型检查不属于 M3。
