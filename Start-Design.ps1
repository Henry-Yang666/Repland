param([switch]$ShowSchedulePreview)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/../Environment/Activate-Repland.ps1"
$designAdb = Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'
$designApk = Join-Path $PSScriptRoot 'android/app/build/outputs/apk/designPreview/app-designPreview.apk'
if (!(Test-Path -LiteralPath $designApk)) { & "$PSScriptRoot/Build-Design.ps1" -SkipTests }
function Get-DesignDevice {
    $designDevices = & $designAdb devices
    foreach ($deviceLine in $designDevices) {
        if ($deviceLine -match '^(emulator-\d+)\s+device\b') {
            $candidateSerial = $Matches[1]
            $candidateAvd = & $designAdb -s $candidateSerial emu avd name
            if ($candidateAvd -contains 'Repland_API_36') { return $candidateSerial }
        }
    }
}
$designSerial = Get-DesignDevice
if (!$designSerial) {
    $existingEmulator = Get-Process -Name 'qemu-system-x86_64' -ErrorAction SilentlyContinue
    if ($existingEmulator -and !($existingEmulator | Where-Object { $_.MainWindowTitle -match 'Repland_API_36' })) {
        throw 'Another emulator is already running. Close it before opening this copy to avoid running two emulators.'
    }
    if (!$existingEmulator) {
        $designLogs = Join-Path $PSScriptRoot '.design-preview'
        New-Item -ItemType Directory -Force -Path $designLogs | Out-Null
        Start-Process -FilePath "$env:ANDROID_HOME/emulator/emulator.exe" -ArgumentList '-avd','Repland_API_36','-memory','1536','-no-snapshot-load' -RedirectStandardOutput "$designLogs/emulator.stdout.log" -RedirectStandardError "$designLogs/emulator.stderr.log" | Out-Null
    }
    $designDeadline = [DateTime]::UtcNow.AddMinutes(3)
    do {
        Start-Sleep -Seconds 2
        $designSerial = Get-DesignDevice
    } until ($designSerial -or [DateTime]::UtcNow -gt $designDeadline)
    if (!$designSerial) { throw 'Emulator did not become available.' }
}
$designBootDeadline = [DateTime]::UtcNow.AddMinutes(3)
while ((& $designAdb -s $designSerial shell getprop sys.boot_completed).Trim() -ne '1') {
    if ([DateTime]::UtcNow -gt $designBootDeadline) { throw 'Emulator boot timed out.' }
    Start-Sleep -Seconds 2
}
& $designAdb -s $designSerial install -r $designApk
if ($LASTEXITCODE -ne 0) { throw 'Design copy installation failed; no packages were uninstalled.' }
$designActivity = if ($ShowSchedulePreview) { 'com.swan1127.repland.designpreview.SchedulePreviewActivity' } else { 'com.swan1127.repland.MainActivity' }
& $designAdb -s $designSerial shell am start -n "com.swan1127.repland.design/$designActivity"
if ($LASTEXITCODE -ne 0) { throw 'Design copy launch failed.' }
