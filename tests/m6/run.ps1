$runner = Join-Path $PSScriptRoot "..\run-java-regression.ps1"
& $runner -TestClass M6RegressionTest
exit $LASTEXITCODE
