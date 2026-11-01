@echo off
setlocal
cd /d "%~dp0"
if not exist lib mkdir lib
if not exist lib\mongo-java-driver-3.12.14.jar (
  echo MongoDB Java driver not found. Downloading it now...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/mongodb/mongo-java-driver/3.12.14/mongo-java-driver-3.12.14.jar' -OutFile 'lib\mongo-java-driver-3.12.14.jar'"
  if errorlevel 1 (
    echo.
    echo Could not download MongoDB driver. Check internet access and try again.
    echo You can manually download mongo-java-driver-3.12.14.jar and place it in:
    echo %~dp0lib\
    pause
    exit /b 1
  )
)
if exist bin rmdir /s /q bin
mkdir bin
if not exist data mkdir data
echo Compiling CivicPulse with MongoDB Atlas support...
javac -encoding UTF-8 -cp "lib/*" -d bin src\civicpulse\Main.java src\civicpulse\MongoStore.java
if errorlevel 1 (
  echo.
  echo COMPILATION FAILED. Check that JDK 17 or later is installed.
  pause
  exit /b 1
)
echo Compilation successful.
endlocal
