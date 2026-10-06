@echo off
rem Compute a SHA-256 baseline manifest for the deterministic text outputs.
rem
rem Usage:   compute-baseline.cmd <outputDir> <manifestFile>
rem
rem Hashes the UTF-8 CSVs and parameters.json under <outputDir> and writes
rem "<relative-path>  <sha256>" lines to <manifestFile>. Binary outputs
rem (TIFF/PNG) and labels.zip are excluded (pixel/ROI normalisation deferred).

setlocal

rem Load machine-specific JAVA_HOME if present.
call "%~dp0local-env.cmd" 2>nul

if "%JAVA_HOME%"=="" (
    echo [compute-baseline] ERROR: JAVA_HOME not set. Create bin\local-env.cmd.
    exit /b 1
)

set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "JAVA=%JAVA_HOME%\bin\java.exe"
set "OUT=%~dp0..\target\baseline"

if not exist "%OUT%" mkdir "%OUT%"
"%JAVAC%" -d "%OUT%" "%~dp0BaselineTool.java"
if errorlevel 1 (
    echo [compute-baseline] ERROR: failed to compile BaselineTool.java
    exit /b 1
)

"%JAVA%" -cp "%OUT%" BaselineTool compute %1 %2
endlocal
