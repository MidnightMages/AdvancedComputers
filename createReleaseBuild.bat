@echo off
echo -------- Removing old files... -------- 
cmd /c gradlew.bat clean || exit /b
echo -------- Setting up IDEA... --------
cmd /c gradlew.bat genIntellijRuns || exit /b
echo -------- Updating assets... -------- 
PowerShell -NoProfile -ExecutionPolicy Bypass -File "copyAssets.ps1" || exit /b
echo -------- Genning data... --------
cmd /c gradlew.bat runData || exit /b
echo -------- Creating build... --------
cmd /c gradlew.bat build || exit /b
echo Done, output is in ./build/libs :)
timeout /t 10