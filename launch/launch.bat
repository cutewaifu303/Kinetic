@echo off
title Kinetic Client
rem Starts the Kinetic client without the launcher.
rem Java 8: client\jre, then the launcher's data folder (downloaded on first launcher start), then JAVA_HOME / PATH.
rem Assets: client\assets, else the launcher's data folder.
cd /d "%~dp0"
set "CLIENT=%~dp0client"
set "DATA=%LOCALAPPDATA%\KineticClient"
set "NATIVES=%CLIENT%\natives\windows"
set "GAMEDIR=%APPDATA%\.minecraft"
set "JAVA="
if exist "%CLIENT%\jre\bin\java.exe" set "JAVA=%CLIENT%\jre\bin\java.exe"
if not defined JAVA if exist "%DATA%\jre\bin\java.exe" set "JAVA=%DATA%\jre\bin\java.exe"
if not defined JAVA if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"
if not defined JAVA set "JAVA=java"
set "ASSETS="
if exist "%CLIENT%\assets\indexes" set "ASSETS=%CLIENT%\assets"
if not defined ASSETS if exist "%DATA%\assets\indexes" set "ASSETS=%DATA%\assets"
if not defined ASSETS (
    echo [Kinetic] No assets found. Start Kinetic.jar once, it downloads them to %DATA%\assets.
    pause
    exit /b 1
)
if not exist "%CLIENT%\Kinetic.jar" (
    echo [Kinetic] client\Kinetic.jar not found.
    pause
    exit /b 1
)
if not exist "%GAMEDIR%" mkdir "%GAMEDIR%"
echo [Kinetic] Java: %JAVA%
echo [Kinetic] Assets: %ASSETS%
echo [Kinetic] Game dir: %GAMEDIR%
set "PATH=%NATIVES%;%PATH%"
"%JAVA%" -Dorg.lwjgl.librarypath="%NATIVES%" -Xmx4G -Xms2G -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=20 -XX:G1HeapRegionSize=32M -XX:+ParallelRefProcEnabled -XX:-UsePerfData -jar "%CLIENT%\Kinetic.jar" --version Kinetic --accessToken 0 --assetsDir "%ASSETS%" --assetIndex 1.8 --gameDir "%GAMEDIR%" --userProperties {}
pause
