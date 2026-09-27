@echo off
rem Launch Fiji (ImageJ 1.x) with the local JDK so the ADAPT plugin can be used
rem interactively.
rem
rem Usage:   run-fiji.cmd [path\to\Fiji.app]
rem Default: C:\Users\barryd\fiji-nojre\Fiji.app
rem
rem This Fiji installation is a "nojre" build (no bundled JRE) and its bundled
rem launcher (fiji-windows-x64.exe) cannot find Java, so we launch ImageJ
rem directly via the JDK. ADAPT is an ImageJ 1.x plugin, so the ij.ImageJ entry
rem point is sufficient.

setlocal

set "FIJI_DIR=%~1"
if "%FIJI_DIR%"=="" set "FIJI_DIR=C:\Users\barryd\fiji-nojre\Fiji.app"

set "JAVA_HOME=%JAVA_HOME%"
if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"

set "JAVA=%JAVA_HOME%\bin\javaw.exe"
if not exist "%JAVA%" set "JAVA=%JAVA_HOME%\bin\java.exe"
if not exist "%JAVA%" (
    echo [run-fiji] ERROR: java not found under "%JAVA_HOME%\bin"
    exit /b 1
)

pushd "%FIJI_DIR%"
start "Fiji" "%JAVA%" -cp "jars\*;plugins\*" ij.ImageJ
popd

endlocal
