@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 goto :java_error
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VERSION=%%v"
set "JAVA_VERSION=%JAVA_VERSION:~1,2%"
if not defined JAVA_VERSION goto :java_error
if %JAVA_VERSION% LSS 17 goto :java_error
java -jar urna-cidada.jar %*
if errorlevel 1 goto :fail
exit /b 0
:java_error
echo ERRO: instale o Temurin JDK 17 ou superior e confira java -version.
pause
exit /b 1
:fail
echo ERRO ao executar. Consulte logs\urna.log.
pause
exit /b 1
