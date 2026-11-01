@echo off
cd /d "%~dp0"
call compile.bat
if errorlevel 1 exit /b 1
echo.
echo Starting CivicPulse website...
echo MongoDB Atlas URI must be set as CIVICPULSE_MONGODB_URI in this same terminal.
echo Open http://localhost:8082 in Chrome/Edge.
echo Keep this window open. Press Ctrl+C to stop.
start "" http://localhost:8082
java -cp "bin;lib/*" civicpulse.Main
