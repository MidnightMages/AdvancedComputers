@echo off
echo -------- Removing old files... -------- 
cmd /c gradlew.bat clean
echo -------- Creating build... --------
cmd /c gradlew.bat build
echo -------- Setting up dev deps again... --------
cmd /c gradlew.bat setupDevDeps
echo -------- Setting up IDEA... --------
cmd /c gradlew.bat genIntellijRuns
echo Done!
timeout /t 10