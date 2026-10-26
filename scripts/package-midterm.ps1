$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$targetRoot = Join-Path $repoRoot "target"
New-Item -ItemType Directory -Path $targetRoot -Force | Out-Null
$outputPath = Join-Path $targetRoot "switch-expression-submission.zip"
$sources = @(
    "Compiler.java",
    "frontEnd/ASTNode.java",
    "frontEnd/LeafASTNode.java",
    "frontEnd/Token.java",
    "frontEnd/ErrorCode.java",
    "frontEnd/MyError.java",
    "frontEnd/Reporter.java",
    "frontEnd/lexer/LexType.java",
    "frontEnd/lexer/Lexer.java",
    "frontEnd/parser/Parser.java"
)
$stream = [System.IO.File]::Open($outputPath, [System.IO.FileMode]::Create)
try {
    $zip = [System.IO.Compression.ZipArchive]::new($stream, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($name in @("config.json") + $sources) {
            $source = if ($name -eq "config.json") { Join-Path $repoRoot $name }
                      else { Join-Path (Join-Path $repoRoot "src/main/java") $name }
            $entry = $zip.CreateEntry($name)
            $input = [System.IO.File]::OpenRead($source)
            $destination = $entry.Open()
            try { $input.CopyTo($destination) }
            finally { $destination.Dispose(); $input.Dispose() }
        }
    }
    finally { $zip.Dispose() }
}
finally { $stream.Dispose() }
Write-Output $outputPath
