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
- M10 已完成字符常量求值收口、官方测试集审计与最终交接文档，本轮 2026 兼容性重构完成。

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

项目也提供不依赖 Maven 下载插件的 javac 回归入口：

```powershell
tests\m10\run.cmd
1..10 | ForEach-Object { cmd /c "tests\m$_\run.cmd" }
```

重新阅读项目时从 [重新上手与后续迭代指南](docs/REVIEW_AND_EXTENSION_GUIDE.md) 开始；最终功能边界和验证结果见 [2026 兼容性记录](docs/2026_COMPATIBILITY.md)。

## 源码结构

```text
src/main/java/
├── Compiler.java       # 编译器入口
├── frontEnd/           # Lexer、Parser、AST、符号表与语义检查
├── middleEnd/          # LLVM-like IR 与中间代码生成
└── backEnd/            # MIPS 表示与目标代码生成
```

## 可选的后续升级方向

1. 为百万级运行用例增加中端或后端优化；
2. 将 M10 的 GCC/MARS 差分流程接入持续集成；
3. 在课程文法变化时按阶段继续扩展，而不是改写已稳定的流水线。
