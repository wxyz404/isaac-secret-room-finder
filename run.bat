@echo off
setlocal
set "PROJECT=%~dp0"

where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven 3.9+ and a JDK 17+ are required. Install them, then run this file again.
  pause
  exit /b 1
)

cd /d "%PROJECT%"
mvn --batch-mode package
if errorlevel 1 (
  pause
  exit /b 1
)

java -cp target\classes com.example.isaacfinder.SecretRoomFinderApp

