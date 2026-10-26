# 本地构建与验证环境

本机使用 Windows x64、Temurin **JDK 17.0.20.1+1** 和 Maven **3.9.11**。
JDK 使用 [Adoptium 官方 API](https://github.com/adoptium/api.adoptium.net/blob/main/docs/cookbook.adoc)
提供的 Windows x64 JDK 17 压缩包，并核对了官方 SHA-256。

- JDK：`%USERPROFILE%\.jdks\jdk-17.0.20.1+1`
- Maven：`%USERPROFILE%\.local\share\apache-maven-3.9.11`
- 已配置用户级 `JAVA_HOME`、`MAVEN_HOME` 和 `PATH`。
- GCC 使用现有 WSL `Ubuntu-22.04` 中的 GCC 11.4.0。

重新打开终端后验证：

```powershell
java -version
javac -version
mvn -v
```

已打开的终端可在当前会话刷新环境：

```powershell
$env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User')
$env:MAVEN_HOME = [Environment]::GetEnvironmentVariable('MAVEN_HOME', 'User')
$env:Path = "$env:JAVA_HOME\bin;$env:MAVEN_HOME\bin;$env:Path"
```

## 验证依赖

外部仓库放在本项目的 `repos/` 下，该目录已忽略，不提交其源码或工具：

```text
repos/
  testcase-2026/
    mars-2026-BUAA.jar
  reference-2026-saitewasreset/     # 可选，兼容旧 MARS 路径
```

M10 和正确性回归优先使用 `testcase-2026` 附带的课程 MARS；也可设置
`MARS_JAR` 指向指定模拟器。保留同级目录的旧布局支持。原版 MARS 可从
[官方仓库](https://github.com/dpetersanderson/MARS) 获取，用作固定用例的备用模拟器。

M10 优先检测 Windows GCC，随后检测 WSL Ubuntu/Debian 中的 GCC，并用
`wslpath` 转换路径。当前官方仓库中仅检查 `error/err_2026` 的错误用例，
不混入其它年份的错误回归；正确程序来自 `testcase/2026`，经
`SUPER_PRODUCTION` 预处理后编译。

## 常用命令

```powershell
# 构建；也可用 mvn package
.\mvnw.cmd package

# 阶段回归
1..10 | ForEach-Object { cmd /c "tests\m$_\run.cmd"; if ($LASTEXITCODE) { throw "M$_ failed" } }

# 必须执行 MIPS 并核对输出的正确性回归
tests\correctness\run.cmd
```

现有 Java 回归使用独立 `main` 入口；`mvn package` 只编译这些测试类，
不会自动执行它们，验证需运行上述脚本。
