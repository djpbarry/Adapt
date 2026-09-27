@echo off
rem Headless smoke test: verify the local Fiji runs on the JDK and that all three
rem ADAPT plugin classes (plus their sibling-library dependencies) are loadable.
rem
rem Usage:   smoke-test-fiji.cmd [path\to\Fiji.app]
rem Default: C:\Users\barryd\fiji-nojre\Fiji.app
rem
rem This is the scripted version of DEVELOPMENT_PLAN.md Phase H2. It does not
rem run the GUI; it only confirms the classpath and plugin wiring.

setlocal

set "FIJI_DIR=%~1"
if "%FIJI_DIR%"=="" set "FIJI_DIR=C:\Users\barryd\Fiji"

set "JAVA_HOME=%JAVA_HOME%"
if "%JAVA_HOME%"=="" set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"

set "JAVA=%JAVA_HOME%\bin\java.exe"
if not exist "%JAVA%" (
    echo [smoke-test-fiji] ERROR: java not found under "%JAVA_HOME%\bin"
    exit /b 1
)

set "CHECK=%TEMP%\AdaptPluginCheck.java"
> "%CHECK%" (
    echo public class AdaptPluginCheck {
    echo     public static void main(String[] a^) throws Exception {
    echo         String[] plugins = {
    echo             "net.calm.adapt.Adapt.Analyse_Movie",
    echo             "net.calm.adapt.Adapt.Analyse_Batch",
    echo             "net.calm.adapt.Adapt.Bleb_Data_Analysis"
    echo         };
    echo         for (String p : plugins^) {
    echo             System.out.println("OK  " + Class.forName(p^).getName(^)^);
    echo         }
    echo         System.out.println("ALL_ADAPT_PLUGINS_LOADABLE"^);
    echo     }
    echo }
)

pushd "%FIJI_DIR%"
"%JAVA%" -cp "jars\*;plugins\*" "%CHECK%" 2>&1
set "RC=%ERRORLEVEL%"
popd

del /q "%CHECK%" 2>nul
exit /b %RC%
