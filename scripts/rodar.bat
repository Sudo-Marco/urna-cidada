@echo off
chcp 65001 >nul
call "%~dp0compilar.bat"
if errorlevel 1 exit /b 1
cd /d "%~dp0.."
echo.
mvn -q exec:java -Dexec.args="%*"
