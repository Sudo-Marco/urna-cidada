@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."
call scripts\compilar.bat
if errorlevel 1 exit /b 1
java --add-modules jdk.httpserver -cp build\classes com.urnacidada.WebMain %*
