$runner = Join-Path $PSScriptRoot "..\run-java-regression.ps1"
& $runner -TestClass M9RegressionTest
exit $LASTEXITCODE
