@echo off
echo -------- Cleaning old files and Setting up IDEA... --------
cmd /c gradlew.bat clean genIntellijRuns || exit /b
echo -------- Updating assets... -------- 
PowerShell -NoProfile -ExecutionPolicy Bypass -File "copyAssets.ps1" || exit /b
echo -------- Genning data and creating build... --------
cmd /c gradlew.bat runData build || exit /b
echo Done, output is in ./build/libs :)
timeout /t 10