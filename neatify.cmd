@REM ---------------------------------------------------------------------------
@REM Neatify launcher for Windows (cmd.exe and PowerShell).
@REM
@REM Runs the built uber-jar (target\neatify.jar) with all arguments forwarded:
@REM   .\neatify.cmd                                     interactive mode
@REM   .\neatify.cmd --source %USERPROFILE%\Downloads --use-default-rules
@REM   .\neatify.cmd --source %USERPROFILE%\Downloads --use-default-rules --apply
@REM
@REM Requires only a JDK 21+. Build the jar first with: .\mvnw.cmd package
@REM ---------------------------------------------------------------------------
@echo off
setlocal

set "APP_DIR=%~dp0"
set "JAR=%APP_DIR%target\neatify.jar"

@REM Determine the Java command: prefer %JAVA_HOME%\bin\java.exe, else `java` on PATH.
if not "%JAVA_HOME%" == "" (
  set "JAVACMD=%JAVA_HOME%\bin\java.exe"
  if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Error: JAVA_HOME is set to an invalid directory: %JAVA_HOME% >&2
    echo        expected java.exe at %JAVA_HOME%\bin\java.exe >&2
    echo Please point JAVA_HOME at a valid JDK 21+ installation. >&2
    exit /b 1
  )
) else (
  set "JAVACMD=java"
  where java >nul 2>&1
  if errorlevel 1 (
    echo Error: no 'java' command found on PATH and JAVA_HOME is not set. >&2
    echo Please install a JDK 21+ or set JAVA_HOME. >&2
    exit /b 1
  )
)

if not exist "%JAR%" (
  echo Error: "%JAR%" not found. >&2
  echo Build it first:  .\mvnw.cmd package >&2
  exit /b 1
)

"%JAVACMD%" -jar "%JAR%" %*
exit /b %ERRORLEVEL%
