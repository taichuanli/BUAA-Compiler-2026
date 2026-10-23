# BUAA Compiler 2026

本项目从 `CompilerTheory-2024/lab/5-ObjectCodeGeneration` 中提取有效 Java 源码后建立，作为将个人 2024 SysY 编译器升级到 2026 课程文法的独立开发仓库。

## 当前状态

目标文法为《2026 编译技术实验期中考核 —— switch 表达式与 yield》给出的修改后完整文法。
期中只考察词法分析与语法分析，`Compiler` 中的 `SYNTAX_ONLY` 为 `true` 时，程序在解析结束、
写出 `error.txt` 后立即退出，不再进入语义分析和后端。

已实现：

- 词法：`->`（ARROW）、`yield`（YIELDTK）等全部类别码；保留字与《完整类别码表》一致；
- 语法：完整 2026 文法，含 `while`、`static`、`switch` 语句（`:`）、`switch` 表达式（`->`）、
  `yield` 语句、`'(' BType ')' UnaryExp` 显式类型转换，`Number` 同时接受整数与字符常量；
- 错误处理：i / j / k 沿用课下实现，新增 o（分支缺 `->` 或 case 缺 `:`）。

尚未实现：语义分析、IR 与 MIPS 对 `while` / `switch` / `switch` 表达式 / `yield` / `static` 的支持。
取舍与后续事项记录在 `REFACTOR_TODO.txt`。

## 编译流水线

```text
testfile.txt
  -> Lexer
  -> Parser / AST
  -> (SYNTAX_ONLY 在此返回)
  -> Visitor / SymbolTable
  -> Trimmer
  -> LLVM-like IR
  -> MIPS
```

程序默认在当前工作目录读取 `testfile.txt`，并生成：

- `lexer.txt`
- `parser.txt`
- `error.txt`
- `symbol.txt`、`llvm_ir.txt`、`mips.txt`（仅在 `SYNTAX_ONLY = false` 时生成）

## 环境要求

- JDK 17 或更高版本
- Maven 3.8 或更高版本

源码使用了 Java 17 的 switch 表达式和文本块，JDK 8 无法编译。

## 构建与运行

```shell
mvn clean package
java -jar target/buaa-compiler-2026-0.1.0-SNAPSHOT.jar
```

## 测试

`testcases/` 下保存期中考核的样例与回归用例，期望输出与被测源文件同名：

```shell
powershell -NoProfile -ExecutionPolicy Bypass -File testcases/run-tests.ps1 -Jdk "<JDK 目录>"
```

- `xxx.c` + `xxx.parser.txt`：正确样例，比对 `parser.txt`；
- `xxx.c` + `xxx.error.txt`：错误样例，比对 `error.txt`；
- 只有 `xxx.c`：只检查没有意料之外的错误。

脚本会把用例复制成 `testfile.txt` 后在临时目录运行，不污染仓库根目录。

## 源码结构

```text
src/main/java/
├── Compiler.java       # 编译器入口
├── frontEnd/           # Lexer、Parser、AST、符号表与语义检查
├── middleEnd/          # LLVM-like IR 与中间代码生成
└── backEnd/            # MIPS 表示与目标代码生成
```
