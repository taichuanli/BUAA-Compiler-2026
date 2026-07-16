# BUAA Compiler 2026

本项目从 `CompilerTheory-2024/lab/5-ObjectCodeGeneration` 中提取有效 Java 源码后建立，作为将个人 2024 SysY 编译器升级到 2026 课程文法的独立开发仓库。

## 当前状态

- 已迁移 2024 最终阶段的前端、中端和 MIPS 后端源码；
- 已保留原工作区中尚未提交的 `TruncInst` / `ANDIInst` 字符截断修复；
- 已排除 IDE 产物、编译输出、压缩包、提交材料和旧测试输出；
- M1 已完成课程错误码枚举化，以及 2026 新关键字和冒号的词法接入；
- M2 已完成 `while` 的 Parser、Visitor、IR 和 MIPS 链路，并删除旧 `for` 文法；
- M3 已将 `get_int`、`get_char`、`put_int`、`put_char` 接入普通函数调用链，并删除旧 `getint/getchar` 特殊语句；
- 字符串 I/O、`switch`、`static` 和严格类型系统仍待后续迭代。

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

1. 重构类型系统和表达式类型推导，加入显式类型转换；
2. 实现静态局部变量和 `switch`；
3. 修正 MIPS 字符数组的字节存储与寻址，再接入 `get_string`、`put_string` 和 `%s`；
4. 接入 `coverage-2026`、`testcase-2026` 和 `pal` 进行回归与对拍。
