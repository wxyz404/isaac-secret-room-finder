@echo off
setlocal
set "PROJECT=%~dp0"

set "MAVEN_CMD="
for /f "delims=" %%M in ('where mvn.cmd 2^>nul') do if not defined MAVEN_CMD set "MAVEN_CMD=%%M"

if not defined MAVEN_CMD if defined MAVEN_HOME if exist "%MAVEN_HOME%\bin\mvn.cmd" set "MAVEN_CMD=%MAVEN_HOME%\bin\mvn.cmd"
if not defined MAVEN_CMD for %%M in ("%ProgramFiles%\Apache\maven\*\bin\mvn.cmd") do if exist "%%~fM" set "MAVEN_CMD=%%~fM"

if not defined MAVEN_CMD (
  echo Maven 3.9+ was not found. Add Maven's bin folder to PATH, or set MAVEN_HOME.
  pause
  exit /b 1
)

cd /d "%PROJECT%"
echo Building Secret Room Finder...
call "%MAVEN_CMD%" --batch-mode package
if errorlevel 1 (
  echo.
  echo Build failed. Review the Maven message above, then press any key to close.
  pause
  exit /b 1
)

echo Starting Secret Room Finder...
java -cp target\classes com.example.isaacfinder.SecretRoomFinderApp
if errorlevel 1 (
  echo.
  echo The application exited with an error. Press any key to close.
  pause
)

