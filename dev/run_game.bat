@echo off
setlocal

:: ============================================================
:: dev/run_game.bat — Dev: launches game mode only (skip menu)
:: ============================================================

cd /d "%~dp0.."

if exist out_merged rmdir /s /q out_merged
mkdir out_merged
javac -encoding UTF-8 -d out_merged -cp "lib\*" -sourcepath src src\Main.java
if errorlevel 1 (
    echo [ERROR] Compilation failed!
    pause
    goto :eof
)

java -cp "out_merged;lib\*" Main game > dev\run_game.log 2>&1
