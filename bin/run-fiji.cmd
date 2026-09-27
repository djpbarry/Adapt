@echo off
rem Launch Fiji so the ADAPT plugin can be used interactively.
rem
rem Usage:   run-fiji.cmd [path\to\Fiji]
rem Default: C:\Users\barryd\Fiji
rem
rem This Fiji is a nojre build (no bundled JRE). A `jre` junction to the JDK 21
rem must exist inside the Fiji directory so the launcher can find Java; see
rem DEVELOPMENT_PLAN.md Phase H1. Launching via the bundled launcher (rather than
rem direct java) gives the full Fiji/ImageJ2 environment.

setlocal

set "FIJI_DIR=%~1"
if "%FIJI_DIR%"=="" set "FIJI_DIR=C:\Users\barryd\Fiji"

set "LAUNCHER=%FIJI_DIR%\fiji-windows-x64.exe"
if not exist "%LAUNCHER%" (
    echo [run-fiji] ERROR: launcher not found: "%LAUNCHER%"
    exit /b 1
)

pushd "%FIJI_DIR%"
start "Fiji" "%LAUNCHER%"
popd

endlocal
