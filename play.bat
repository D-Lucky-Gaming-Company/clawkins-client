@echo off
REM Clawkins: Dawn of the Primal - Quick Play Script
REM Run this file to start the game immediately

setlocal enabledelayedexpansion

echo.
echo ========================================
echo  Clawkins: Dawn of the Primal
echo  Launching Game...
echo ========================================
echo.

REM Run the game using Gradle
call .\gradlew.bat lwjgl3:run

if errorlevel 1 (
    echo.
    echo ERROR: Game launch failed
    echo.
    pause
    exit /b 1
)

echo.
echo Game closed successfully
echo.
pause
exit /b 0
