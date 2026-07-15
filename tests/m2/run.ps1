$runner = Join-Path $PSScriptRoot "..\run-java-regression.ps1"
& $runner -TestClass M2RegressionTest
exit $LASTEXITCODE
