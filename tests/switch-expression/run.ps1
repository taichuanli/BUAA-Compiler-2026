$runner = Join-Path $PSScriptRoot "..\run-java-regression.ps1"
& $runner -TestClass SwitchExpressionRegressionTest
exit $LASTEXITCODE
