@echo off
rem Starts LABSTOCK from the ready-made LabStock.jar (needs Java 8 or newer).
cd /d "%~dp0"
java -jar LabStock.jar
if errorlevel 1 (
    echo.
    echo LABSTOCK could not start. Is Java installed? Check with:  java -version
    echo Download Java from https://adoptium.net if needed.
    pause
)
