@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Start-Design.ps1" -ShowSchedulePreview
if errorlevel 1 pause
