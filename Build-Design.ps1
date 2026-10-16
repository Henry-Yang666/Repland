param([switch]$SkipTests)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/../Environment/Activate-Repland.ps1"
$designProject = Join-Path $PSScriptRoot 'android'
$designTasks = @(':app:assembleDesignPreview', ':app:verifyOfflineMvpBoundary')
if (!$SkipTests) { $designTasks += ':app:testDesignPreviewUnitTest' }
& "$designProject/gradlew.bat" -p $designProject --offline --no-daemon --max-workers=2 '-Pkotlin.compiler.execution.strategy=in-process' @designTasks
if ($LASTEXITCODE -ne 0) { throw "Design build failed with exit code $LASTEXITCODE" }
