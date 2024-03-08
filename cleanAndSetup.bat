@echo off
echo -------- Removing old files... -------- 
cmd /c gradlew.bat clean
echo -------- Setting up luajava for dev... --------
cmd /c gradlew.bat setupDevDeps
echo -------- Setting up IDEA... --------
cmd /c gradlew.bat genIntellijRuns
echo Done!
timeout /t 10