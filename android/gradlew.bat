@echo off
setlocal
set GRADLE_VERSION=8.11.1
set GRADLE_SHA256=f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6
if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set DIST_ROOT=%GRADLE_USER_HOME%\manual-wrapper
set GRADLE_HOME=%DIST_ROOT%\gradle-%GRADLE_VERSION%
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%DIST_ROOT%" mkdir "%DIST_ROOT%"
  echo Downloading Gradle %GRADLE_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%DIST_ROOT%\gradle.zip'; $actual=(Get-FileHash -Algorithm SHA256 '%DIST_ROOT%\gradle.zip').Hash.ToLowerInvariant(); if ($actual -ne '%GRADLE_SHA256%') { throw 'Gradle distribution SHA-256 mismatch' }; Expand-Archive -Path '%DIST_ROOT%\gradle.zip' -DestinationPath '%DIST_ROOT%' -Force"
  if errorlevel 1 exit /b 1
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
endlocal
