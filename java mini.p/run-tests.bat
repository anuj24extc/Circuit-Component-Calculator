@echo off
rem ------------------------------------------------------------
rem  LABSTOCK - run the automatic tests (Windows)
rem  Uses a temporary folder, so your data\ folder is not touched.
rem ------------------------------------------------------------
cd /d "%~dp0"
if not exist out-test mkdir out-test
javac -encoding UTF-8 -d out-test -sourcepath src;test test\labstock\LabStockTest.java
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)
java -cp out-test labstock.LabStockTest
pause
