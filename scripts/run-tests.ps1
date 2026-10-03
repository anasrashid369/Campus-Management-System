$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$build = Join-Path $root "build-test"
New-Item -ItemType Directory -Force -Path $build | Out-Null
Remove-Item -Path (Join-Path $root "catalog-test.txt") -Force -ErrorAction SilentlyContinue
Remove-Item -Path (Join-Path $root "clash-approval-test.txt") -Force -ErrorAction SilentlyContinue
$sources = @(Get-ChildItem (Join-Path $root "src\*.java")).FullName
$tests = @(Get-ChildItem (Join-Path $root "tests\*.java")).FullName
javac -d $build -encoding UTF-8 @sources @tests
if ($LASTEXITCODE -ne 0) { throw "Compilation failed." }

# Tests use their own catalog files and never modify the user's data\catalog.txt.
$suites = @("AnasCoreSmokeTest", "ExtendedWorkflowTest", "CourseUnitTest",
        "SectionUnitTest", "StudentUnitTest", "RequestUnitTest")
$failed = @()
Push-Location $root
try {
    foreach ($suite in $suites) {
        java -cp $build $suite
        if ($LASTEXITCODE -ne 0) { $failed += $suite }
    }
} finally {
    Pop-Location
}
if ($failed.Count -gt 0) { throw "Failed test suites: $($failed -join ', ')" }
Write-Output "All $($suites.Count) test suites passed."
