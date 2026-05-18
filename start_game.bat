@echo off
setlocal

:: ============================================================
:: start_game.bat — Production launcher for Ayutthaya Racing
:: Compiles and runs the game with full menu (Login -> Game)
:: ============================================================

title Ayutthaya Racing - Starting...
echo ========================================
echo   Ayutthaya Racing - Starting...
echo ========================================

:: Compile
if exist out_merged rmdir /s /q out_merged
mkdir out_merged
javac -encoding UTF-8 -d out_merged -cp "lib\*" -sourcepath src src\Main.java 2> compile_debug.log
if errorlevel 1 (
    echo [ERROR] Compilation failed! See compile_debug.log
    pause
    goto :eof
)

:: Run (no debug output)
java -cp "out_merged;lib\*" Main
