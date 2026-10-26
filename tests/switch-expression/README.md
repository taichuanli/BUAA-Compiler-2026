# switch 表达式验题集

```powershell
tests\switch-expression\run.cmd
```

20 个文件用例加两项直接检查，共 **22 项**。正确用例逐行比对 lexer/parser
完整输出；错误用例比对完整 error.txt，并确认恢复后仍完成 CompUnit。
全部用例确认 Compiler 未生成 symbol/IR/MIPS 文件。

| 用例 | 覆盖内容 |
| --- | --- |
| testcase1–3 | 原有声明、算术、函数、char、循环、语句 switch；语义非法但语法正确的程序 |
| testcase4–5 | 原有 i/j/k/a，错误行号和恢复 |
| testcase6 | 题面正确样例及原文 parser.txt，完整逐行比较 |
| testcase7 | 运算优先级、强转、函数参数、紧邻的 `->-` 和 `->+` |
| testcase8 | selector 和分支值内嵌套 switch、空 switch |
| testcase9 | 数组下标中的 switch 表达式、跨行赋值、表达式语句 |
| testcase10 | ConstExp、数组长度、static 初值、条件、带括号的表达式语句 |
| testcase11–13 | 缺少箭头、分支分号、selector 右括号 |
| testcase14 | 嵌套 j/o/i、default 缺箭头、同一行多个错误、恢复后继续下一分支 |
| testcase15 | 循环、语句/表达式 switch、数组赋值、函数及 printf 的综合用例 |
| empty_and_duplicate_arms | 零分支、重复 case/default 在本题不触发语义错误 |
| deep_nesting | 12 层表达式 switch |
| multiline_character_arrow | char 标签与 default 的跨行 o 定位 |
| missing_index_bracket | 新赋值前瞻保留旧 k 恢复 |
| crlf_sample | CRLF 输入与题面样例输出一致 |
| lexer_boundaries | 箭头最长匹配，空白/注释不合并，字符串和字符中的符号 |
| reporter_reset | 同一 JVM 先报 o 再编译正确程序，错误状态不残留 |

`manifest.json` 对应前 20 个文件用例。正确答案来自题面原样例及显式的文法
推导树，不读取被测编译器的输出生成答案；错误答案按缺失符号前的终结符
行号人工指定。`build_cases.py` 可确定性重建这些输入和答案：

```powershell
python tests/switch-expression/build_cases.py
```

提交包单独验证：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/package-midterm.ps1
$env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
python tests/switch-expression/check_submission.py
```

该检查仅使用 ZIP 内的 10 个 Java 源文件加测试入口进行 JDK 17 编译，重跑
22 项回归。存在 `repos/testcase-2026` 的参考 JAR 时，还会独立对照原文法
5 个用例的词法、语法或错误输出。本地通过结果不等同于平台隐藏测试成绩。
