$runner = Join-Path $PSScriptRoot "..\run-java-regression.ps1"
& $runner -TestClass CorrectnessRegressionTest
exit $LASTEXITCODE
