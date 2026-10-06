@echo off
rem Verify the current output against a committed SHA-256 baseline manifest.
rem
rem Usage:   verify-baseline.cmd <outputDir> <manifestFile>
rem
rem Recomputes SHA-256 for the deterministic text outputs (UTF-8 CSVs and
rem parameters.json) under <outputDir> and compares against <manifestFile>.
rem Exits non-zero on any missing, unexpected, or mismatched file.

setlocal

rem Load machine-specific JAVA_HOME if present.
call "%~dp0local-env.cmd" 2>nul

if "%JAVA_HOME%"=="" (
    echo [verify-baseline] ERROR: JAVA_HOME not set. Create bin\local-env.cmd.
    exit /b 1
)

set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "JAVA=%JAVA_HOME%\bin\java.exe"
set "OUT=%~dp0..\target\baseline"

if not exist "%OUT%" mkdir "%OUT%"
"%JAVAC%" -d "%OUT%" "%~dp0BaselineTool.java"
if errorlevel 1 (
    echo [verify-baseline] ERROR: failed to compile BaselineTool.java
    exit /b 1
)

"%JAVA%" -cp "%OUT%" BaselineTool verify %1 %2
endlocal
