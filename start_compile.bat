@echo off
setlocal

:: ============================================================
:: start_game.bat — Production launcher for Ayutthaya Racing
:: Compiles and runs the game with full menu (Login -> Game)
:: ============================================================

title Ayutthaya Racing - Compiling...
echo ========================================
echo   Ayutthaya Racing - Compiling...
echo ========================================

:: Compile
if exist out_latest rmdir /s /q out_latest
mkdir out_latest
javac -encoding UTF-8 -d out_latest -cp "lib\*" -sourcepath src src\Main.java 2> compile_debug.log
if errorlevel 1 (
    echo [ERROR] Compilation failed! See compile_debug.log
    pause
    goto :eof
)

:: Run (no debug output)
title Ayutthaya Racing - Started...
java -cp "out_latest;lib\*" Main
