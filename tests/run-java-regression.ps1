param(
    [Parameter(Mandatory = $true)]
    [string]$TestClass
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$workRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("buaa-compiler-" + $TestClass.ToLowerInvariant() + "-" + [guid]::NewGuid())

function Find-Jdk17Bin {
    $candidates = @()
    $vscodeSettings = Join-Path $repoRoot ".vscode\settings.json"
    if (Test-Path $vscodeSettings) {
        $settings = Get-Content -Raw -Encoding UTF8 $vscodeSettings | ConvertFrom-Json
        $candidates += @($settings.'java.configuration.runtimes' | ForEach-Object { $_.path })
    }
    if ($env:JAVA_HOME) {
        $candidates += $env:JAVA_HOME
    }
    $pathJavac = Get-Command javac -ErrorAction SilentlyContinue
    if ($pathJavac) {
        $candidates += (Split-Path -Parent (Split-Path -Parent $pathJavac.Source))
    }
    $userJdks = Join-Path $HOME ".jdks"
    if (Test-Path $userJdks) {
        $candidates += @(Get-ChildItem $userJdks -Directory | Select-Object -ExpandProperty FullName)
    }

    foreach ($homePath in $candidates | Where-Object { $_ } | Select-Object -Unique) {
        $binPath = Join-Path $homePath "bin"
        $javacPath = Join-Path $binPath "javac.exe"
        if (Test-Path $javacPath) {
            $versionText = (& $javacPath -version 2>&1 | Out-String)
            if ($versionText -match 'javac\s+(?:1\.)?(\d+)' -and [int]$Matches[1] -ge 17) {
                return $binPath
            }
        }
    }
    throw "JDK 17 or newer was not found"
}

try {
    $jdkBin = Find-Jdk17Bin
    $javac = Join-Path $jdkBin "javac.exe"
    $java = Join-Path $jdkBin "java.exe"
    $classesRoot = Join-Path $workRoot "classes"
    New-Item -ItemType Directory -Path $classesRoot | Out-Null

    $mainSources = Get-ChildItem (Join-Path $repoRoot "src\main\java") -Recurse -Filter "*.java"
    $testSource = Join-Path $repoRoot ("src\test\java\" + $TestClass + ".java")
    $sourceFiles = @($mainSources.FullName) + @($testSource)
    & $javac --release 17 -encoding UTF-8 -d $classesRoot $sourceFiles
    if ($LASTEXITCODE -ne 0) {
        throw "Java test compilation failed with exit code $LASTEXITCODE"
    }

    Push-Location $workRoot
    try {
        & $java -cp $classesRoot $TestClass $repoRoot
        if ($LASTEXITCODE -ne 0) {
            throw "$TestClass failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}
finally {
    if (Test-Path $workRoot) {
        Remove-Item -LiteralPath $workRoot -Recurse -Force
    }
}
