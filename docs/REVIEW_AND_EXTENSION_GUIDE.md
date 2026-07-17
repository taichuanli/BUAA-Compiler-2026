# 编译器重新上手与后续迭代指南

这份指南面向“已经较长时间没有接触项目”的维护者。目标不是一次读懂所有类，而是先恢复一条完整心智链路，再按功能纵向深入。

## 1. 第一次回顾：先运行，再读代码

1. 在仓库根目录准备一个最小 `testfile.txt`，运行编译器。
2. 依次查看 `lexer.txt`、`parser.txt`、`symbol.txt`、`error.txt`、`llvm_ir.txt` 和 `mips.txt`。
3. 执行 `tests\m10\run.cmd`，确认本机能完成最终兼容性审计。

不要从 `Compiler.java` 开始逐文件顺序阅读。更高效的方式是选择源程序中的一个结构，例如 `while`、函数调用或数组赋值，然后沿输出文件反向追踪它经过的阶段。

## 2. 一条功能经过哪些代码

```text
源代码
  -> frontEnd/lexer/Lexer.java          Token
  -> frontEnd/parser/Parser.java        AST
  -> frontEnd/visitor/Visitor.java      符号表、类型与错误 a-n
  -> frontEnd/Trimmer.java              条件表达式树整理
  -> middleEnd/IRGenerator.java         LLVM-like IR
  -> middleEnd/Insts/*                  单条 IR 的 MIPS 降低
  -> backEnd/MIPSGenerator.java         数据段、函数和基本块组装
```

关键辅助结构：

- `SymbolTable` / `Symbol`：源语言名字、作用域和声明信息；
- `SemanticType`：语义分析使用的 `int/char/int[]/char[]/void` 类型；
- `LLVMSymbolTable`：源语言名字到 IR 值或地址的绑定；
- `ConstCalculator`：常量表达式、显式转换和 char 模 256 计算；
- `RuntimeLibrary`：运行时库函数的统一签名；
- `MIPSManager`：栈槽、临时寄存器和跨函数调用现场。

## 3. 推荐阅读顺序

### 第一轮：恢复整体认识

只阅读以下入口及其数据结构，不追求每个分支都看懂：

1. `Compiler.main`：确认阶段顺序和“有错误则不生成 IR”的边界；
2. `LexType`、`ASTNode`、`Symbol`、`SemanticType`、`LLVMType`；
3. `Parser.parseCompUnit`、`Visitor.visitCompUnit`、`IRGenerator.translateModule`；
4. 一个简单表达式从 `parseAddExp` 到 `BinaryInst.toMIPS` 的链路。

### 第二轮：按 2026 新功能纵向阅读

- `while`：Stmt 解析、循环上下文、break/continue 标签栈；
- 普通运行库调用：`RuntimeLibrary`、函数调用类型检查、`CallInst`；
- 类型系统：`SemanticType` 与 Visitor 中表达式返回类型；
- `switch`：case/default 去重、fall-through 和 break 目标；
- char：i8 IR、模 256、`.byte`、`lbu/sb`、数组步长 1；
- `static`：源作用域查找与唯一全局存储名称的分离；
- 后端边界：大栈偏移展开和每次生成前的状态重置。

### 第三轮：带着测试定位

每个 `tests/mX` 目录对应一次可独立回顾的改造。先读用例和 README，再读对应提交：

```powershell
git show 85c38c0   # M2 while
git show 22e2096   # M4 类型系统
git show 141b501   # M5 switch
git show 9c98923   # M6 char 后端
git show 6c53ce1   # M8 static
git show fe2e9e5   # M9 后端边界
```

## 4. 修改一条文法时的固定流程

1. 在官方文法中确认产生式、类型约束和可能错误码。
2. 判断是否需要新 Token；不要仅因为 Parser 需要就忘记 Lexer 输出兼容。
3. 在 Parser 只构造结构，不混入符号查找和 IR 生成。
4. 在 Visitor 中明确每个表达式的 `SemanticType`，并只报告课程规定的 a-n。
5. 在 IR 中先保证类型和控制流正确，再实现目标指令降低。
6. 同时准备正常、错误和底层边界用例；先跑当前 Mx，再跑 M1 到最新 Mx。
7. 将阶段性兼容取舍写入 `REFACTOR_TODO.txt`，验证后提交一次独立 commit。

若新功能只需局部补丁，不要为“架构更漂亮”提前拆分整个 Compiler。只有当相同规则已经出现三处以上且容易不一致时，再提取共享结构；`LiteralValue` 就是这种情况。

## 5. 常见问题的定位方法

### 错误行号或错误码不对

先看 `error.txt`，再定位 Reporter 调用发生在 Lexer、Parser 还是 Visitor。i/j/k 的行号来自缺失符号前一个非终结符；其他错误按官方表中的确定位置报告。

### 正确程序被报 e

打印或断点观察 Visitor 中左右操作数的 `SemanticType`。重点区分：

- 数组整体与数组元素；
- 函数标识符与函数调用结果；
- char 与 `(int)` 转换后的 int；
- void 调用是否被误用为值。

### IR 正确但 MIPS 输出错误

先在 `llvm_ir.txt` 判断地址和值是否混淆，再查看对应 `middleEnd/Insts` 的 `toMIPS()`。char 地址步长为 1，int 为 4；char 数据使用字节访存，但指针、参数和溢出槽仍为 4 字节。

### 小程序正确、大程序错误

检查 MIPS 的 `addiu`、`lw/sw`、`lbu/sb` 是否留下超出 `[-32768, 32767]` 的立即数，并使用 M9 大数组用例复现。

## 6. 验证命令

```powershell
# 单阶段
tests\m10\run.cmd

# 全量阶段回归
1..10 | ForEach-Object { cmd /c "tests\m$_\run.cmd" }
```

M10 在同级目录存在 `testcase-2026` 时，会自动核对 58 个官方错误用例并预处理、编译 48 个官方正确用例；存在参考 MARS jar 时，还会执行仓库内固定运行用例。

## 7. 当前非目标

当前版本以 2026 文法和语义正确性为完成标准，没有实现高级目标码优化。官方百万循环递归 `cr/perf` 能编译，缩小输入后与 GCC 输出一致，但未优化 MIPS 在 240 秒内未完成官方百万输入。若课程后续要求性能，再将优化作为独立阶段处理，不要与语义维护混在一起。
