$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$build = Join-Path $root "build-test"
New-Item -ItemType Directory -Force -Path $build | Out-Null
Remove-Item -Path (Join-Path $root "data\catalog.txt") -Force -ErrorAction SilentlyContinue
Remove-Item -Path (Join-Path $root "catalog-test.txt") -Force -ErrorAction SilentlyContinue
Remove-Item -Path (Join-Path $root "clash-approval-test.txt") -Force -ErrorAction SilentlyContinue
$sources = @(Get-ChildItem (Join-Path $root "src\*.java")).FullName
$tests = @(Get-ChildItem (Join-Path $root "tests\*.java")).FullName
javac -d $build -encoding UTF-8 @sources @tests
java -cp $build AnasCoreSmokeTest
java -cp $build ExtendedWorkflowTest
java -cp $build CourseUnitTest
java -cp $build SectionUnitTest
java -cp $build StudentUnitTest
Remove-Item -Path (Join-Path $root "data\catalog.txt") -Force -ErrorAction SilentlyContinue




