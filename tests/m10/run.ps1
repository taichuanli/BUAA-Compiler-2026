$runner = Join-Path $PSScriptRoot "..\run-java-regression.ps1"
& $runner -TestClass M10RegressionTest
exit $LASTEXITCODE
