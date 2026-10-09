@echo off
rem ------------------------------------------------------------
rem  LABSTOCK - compile and run (Windows)
rem  Needs a JDK (Java 8 or newer) with javac on the PATH.
rem ------------------------------------------------------------
cd /d "%~dp0"

echo Compiling...
if not exist out mkdir out
rem -sourcepath lets javac find and compile every class that Main uses
javac -encoding UTF-8 -d out -sourcepath src src\labstock\Main.java
if errorlevel 1 (
    echo.
    echo Compilation failed. Check that a JDK is installed: javac -version
    pause
    exit /b 1
)

echo Starting LABSTOCK...
java -cp out labstock.Main
