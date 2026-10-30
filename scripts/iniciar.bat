@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."
where java >nul 2>nul
if errorlevel 1 goto :java_error
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VERSION=%%v"
set "JAVA_VERSION=%JAVA_VERSION:~1,2%"
if not defined JAVA_VERSION goto :java_error
if %JAVA_VERSION% LSS 17 goto :java_error
if not exist dist\urna-cidada.jar call scripts\compilar.bat
if errorlevel 1 exit /b 1
java -jar dist\urna-cidada.jar %*
if errorlevel 1 goto :fail
exit /b 0
:java_error
echo ERRO: Java 17 ou superior (JDK) nao encontrado. Instale o Temurin JDK 17+, reabra o terminal e confira java -version.
pause
exit /b 1
:fail
echo ERRO ao executar a urna. Consulte logs\urna.log.
pause
exit /b 1
