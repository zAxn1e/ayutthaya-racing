@echo off
setlocal

:: ============================================================
:: dev/run_menu.bat — Dev: launches menu only
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

java -Ddev.mode=true -cp "out_merged;lib\*" Main > dev\run_menu.log 2>&1
