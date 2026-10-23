# 期中考核回归脚本
# 编译 src/main/java 下的全部源码，逐个运行 testcases/*.c，并与同名的期望输出比对：
#   xxx.c + xxx.parser.txt  -> 正确样例，比对 parser.txt
#   xxx.c + xxx.error.txt   -> 错误样例，比对 error.txt
param(
    [string]$Jdk = $env:JAVA_HOME,
    [string]$WorkDir = (Join-Path $env:TEMP 'buaa-compiler-tests')
)

$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
$testDir = Join-Path $repo 'testcases'
$classes = Join-Path $WorkDir 'classes'
$runDir = Join-Path $WorkDir 'run'

foreach ($tool in @('javac.exe', 'java.exe')) {
    if (-not (Test-Path (Join-Path $Jdk "bin\$tool"))) {
        throw "找不到 $tool，请用 -Jdk 指定 JDK 目录（当前为 '$Jdk'）"
    }
}

Remove-Item -Recurse -Force $classes -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $classes, $runDir | Out-Null

$sources = Get-ChildItem -Path (Join-Path $repo 'src\main\java') -Filter '*.java' -Recurse |
    Select-Object -ExpandProperty FullName
& (Join-Path $Jdk 'bin\javac.exe') -encoding UTF-8 -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw '编译失败' }

function Read-Lines([string]$path) {
    if (-not (Test-Path $path)) { return @() }
    $lines = [System.IO.File]::ReadAllLines($path)
    $end = $lines.Length
    while ($end -gt 0 -and $lines[$end - 1].Trim() -eq '') { $end-- }
    if ($end -eq 0) { return @() }
    return $lines[0..($end - 1)]
}

$failed = 0
$passed = 0
foreach ($case in Get-ChildItem -Path $testDir -Filter '*.c' | Sort-Object Name) {
    $name = $case.BaseName
    Copy-Item $case.FullName (Join-Path $runDir 'testfile.txt') -Force
    Remove-Item (Join-Path $runDir 'parser.txt'), (Join-Path $runDir 'error.txt') -ErrorAction SilentlyContinue
    Push-Location $runDir
    try {
        & (Join-Path $Jdk 'bin\java.exe') -cp $classes Compiler
        if ($LASTEXITCODE -ne 0) { throw "运行失败（exit $LASTEXITCODE）" }
    } finally {
        Pop-Location
    }

    $expectedParser = Join-Path $testDir "$name.parser.txt"
    $expectedError = Join-Path $testDir "$name.error.txt"
    if (Test-Path $expectedParser) {
        $want = @(Read-Lines $expectedParser)
        $got = @(Read-Lines (Join-Path $runDir 'parser.txt'))
        $label = 'parser.txt'
    } elseif (Test-Path $expectedError) {
        $want = @(Read-Lines $expectedError)
        $got = @(Read-Lines (Join-Path $runDir 'error.txt'))
        $label = 'error.txt'
    } else {
        # 没有期望文件时只检查是否有意料之外的错误
        $want = @()
        $got = @(Read-Lines (Join-Path $runDir 'error.txt'))
        $label = 'error.txt（期望为空）'
    }

    $ok = ([string]::Join([char]10, $want) -eq [string]::Join([char]10, $got))
    if ($ok) {
        $passed++
        Write-Output "PASS $name ($label)"
    } else {
        $failed++
        Write-Output "FAIL $name ($label)"
        $diff = Compare-Object -ReferenceObject $want -DifferenceObject $got -SyncWindow 0
        foreach ($d in $diff) {
            $side = '实际'
            if ($d.SideIndicator -eq '<=') { $side = '期望' }
            Write-Output "     ${side}: $($d.InputObject)"
        }
    }
}

Write-Output '----'
Write-Output "PASS=$passed FAIL=$failed"
if ($failed -ne 0) { exit 1 }
