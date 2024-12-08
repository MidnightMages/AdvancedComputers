@echo off
echo -------- Removing old files, Setting up IDEA..., Genning data... -------- 
cmd /c gradlew.bat clean genIntellijRuns runData || exit /b
echo Done!
timeout /t 10