$runner = Join-Path $PSScriptRoot "..\run-java-regression.ps1"
& $runner -TestClass M8RegressionTest
exit $LASTEXITCODE
