@echo off
rem Install the built ADAPT plugin jar into a local Fiji installation.
rem
rem Usage:   install-to-fiji.cmd [path\to\Fiji.app]
rem Default: C:\Users\barryd\fiji-nojre\Fiji.app
rem
rem This only installs the ADAPT plugin jar itself. Its three sibling
rem dependencies (IAClassLibrary, TrackerLibrary, AdaptDataProcessing) and the
rem rest of the ImageJ/Fiji stack are expected to already be present in the
rem Fiji installation (see DEVELOPMENT_PLAN.md Phase H).

setlocal enabledelayedexpansion

set "FIJI_DIR=%~1"
if "%FIJI_DIR%"=="" set "FIJI_DIR=C:\Users\barryd\fiji-nojre\Fiji.app"
set "PLUGINS=%FIJI_DIR%\plugins"

if not exist "%PLUGINS%" (
    echo [install-to-fiji] ERROR: Fiji plugins directory not found: "%PLUGINS%"
    exit /b 1
)

rem Build the plugin (skip tests). Run from the repo root.
pushd "%~dp0.."
call mvnw.cmd -q -DskipTests package
if errorlevel 1 (
    popd
    echo [install-to-fiji] ERROR: build failed.
    exit /b 1
)
popd

rem Remove any previously installed adapt plugin jar (avoid duplicate classes).
del /q "%PLUGINS%\adapt-*.jar" 2>nul

rem Copy the freshly built plugin jar (skip -sources / -javadoc).
set "INSTALLED="
for %%f in ("%~dp0..\target\adapt-*.jar") do (
    echo %%~nxf | findstr /i "sources javadoc" >nul
    if errorlevel 1 (
        copy /y "%%f" "%PLUGINS%\" >nul
        echo [install-to-fiji] Installed %%~nxf
        set "INSTALLED=1"
    )
)

if not defined INSTALLED (
    echo [install-to-fiji] ERROR: no plugin jar found under target\adapt-*.jar
    exit /b 1
)

echo [install-to-fiji] Done. Restart Fiji to pick up the new plugin.
endlocal
