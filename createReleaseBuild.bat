@echo off
echo -------- Removing old files... -------- 
cmd /c gradlew.bat clean || exit /b
echo -------- Updating assets... -------- 
PowerShell -NoProfile -ExecutionPolicy Bypass -File "copyAssets.ps1" || exit /b
echo -------- Genning data... --------
cmd /c gradlew.bat runData || exit /b
echo -------- Creating build... --------
cmd /c gradlew.bat build || exit /b
rem echo -------- Setting up dev deps again... --------
rem cmd /c gradlew.bat setupDevDeps
echo -------- Setting up IDEA... --------
cmd /c gradlew.bat genIntellijRuns || exit /b
echo Done!
timeout /t 10