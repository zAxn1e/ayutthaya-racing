@echo off
setlocal

if exist out_merged rmdir /s /q out_merged
mkdir out_merged

javac -encoding UTF-8 -d out_merged -cp "lib\*" -sourcepath src src\Main.java
if errorlevel 1 goto :eof

java -cp "out_merged;lib\*" Main > run_merged.log 2>&1
