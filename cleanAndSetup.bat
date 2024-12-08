@echo off
echo -------- Removing old files... -------- 
cmd /c gradlew.bat clean || exit /b
echo -------- Setting up IDEA... --------
cmd /c gradlew.bat genIntellijRuns || exit /b
echo -------- Genning data... --------
cmd /c gradlew.bat runData || exit /b
echo Done!
timeout /t 10