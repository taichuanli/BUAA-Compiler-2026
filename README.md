# BUAA Compiler 2026

本项目从 `CompilerTheory-2024/lab/5-ObjectCodeGeneration` 中提取有效 Java 源码后建立，作为将个人 2024 SysY 编译器升级到 2026 课程文法的独立开发仓库。

## 当前状态

- 已迁移 2024 最终阶段的前端、中端和 MIPS 后端源码；
- 已保留原工作区中尚未提交的 `TruncInst` / `ANDIInst` 字符截断修复；
- 已排除 IDE 产物、编译输出、压缩包、提交材料和旧测试输出；
- M1 已完成课程错误码枚举化，以及 2026 新关键字和冒号的词法接入；
- M2 已完成 `while` 的 Parser、Visitor、IR 和 MIPS 链路，并删除旧 `for` 文法；
- M3 已将 `get_int`、`get_char`、`put_int`、`put_char` 接入普通函数调用链，并删除旧 `getint/getchar` 特殊语句；
- M4 已加入独立表达式类型系统、完整课程类型错误 `e` 与 `(int)` / `(char)` 显式转换；
- M5 已实现 `switch/case/default`、fall-through 以及错误 `e/m/n`；
- M6 已实现字符 `.byte` 数据、`lbu/sb` 访存、步长 1 的数组寻址及模 256 字符算术；
- M7 已实现 `get_string`、`put_string` 与 `printf %s`；
- M8 已实现局部 `static` 的零/常量初始化、持久化标量与数组存储及嵌套作用域唯一命名；
- M9 已实现跨次编译后端状态重置，并加固超过 16 位范围的栈调整与字/字节访存偏移；
- 完整测试集审计与教程整理仍待最终迭代。

## 编译流水线

```text
testfile.txt
  -> Lexer
  -> Parser / AST
  -> Visitor / SymbolTable
  -> Trimmer
  -> LLVM-like IR
  -> MIPS
```

程序默认在当前工作目录读取 `testfile.txt`，并生成：

- `lexer.txt`
- `parser.txt`
- `symbol.txt`
- `error.txt`
- `llvm_ir.txt`
- `mips.txt`

## 环境要求

- JDK 17 或更高版本
- Maven 3.8 或更高版本

源码使用了 Java 17 的 switch 表达式和文本块，JDK 8 无法编译。

## 构建与运行

```shell
mvn clean package
java -jar target/buaa-compiler-2026-0.1.0-SNAPSHOT.jar
```

## 源码结构

```text
src/main/java/
├── Compiler.java       # 编译器入口
├── frontEnd/           # Lexer、Parser、AST、符号表与语义检查
├── middleEnd/          # LLVM-like IR 与中间代码生成
└── backEnd/            # MIPS 表示与目标代码生成
```

## 后续升级方向

1. 接入 `coverage-2026`、`testcase-2026` 和 `pal` 进行回归与对拍；
2. 整理符号表、类型系统与语义分析教程。
