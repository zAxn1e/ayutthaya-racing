@echo off
setlocal

:: ============================================================
:: dev/run_test.bat — Dev launcher with full console logging
:: Compiles and runs the game with diagnostics visible
:: ============================================================

title [DEV] Ayutthaya Racing - Test Run
echo ========================================
echo   [DEV] Ayutthaya Racing - Test Run
echo   Console output will be shown here.
echo   Press F1 in-game to toggle Debug Mode.
echo ========================================
echo.

cd /d "%~dp0.."

:: Compile
if exist out_merged rmdir /s /q out_merged
mkdir out_merged
javac -encoding UTF-8 -d out_merged -cp "lib\*" -sourcepath src src\Main.java
if errorlevel 1 (
    echo [ERROR] Compilation failed!
    pause
    goto :eof
)

echo [OK] Compilation succeeded
echo [..] Launching game...
echo.

:: Run with full console logging (no redirect)
java -cp "out_merged;lib\*" Main

echo.
echo ========================================
echo   Game exited.
echo ========================================
pause
