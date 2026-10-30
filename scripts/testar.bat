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
echo Compilando testes Java puros (sem Maven nem internet)...
if not exist build mkdir build
if exist build\testes rmdir /s /q build\testes
mkdir build\testes
javac --release 17 -encoding UTF-8 -d build\testes -sourcepath "src;tests" tests\com\urnacidada\testes\ExecutarTestes.java
if errorlevel 1 goto :fail
java -Djava.awt.headless=true -cp build\testes com.urnacidada.testes.ExecutarTestes
if errorlevel 1 goto :fail
exit /b 0
:java_error
echo ERRO: Java 17 ou superior (JDK) nao encontrado. Instale o Temurin JDK 17+, abra um novo terminal e confira com: java -version
pause
exit /b 1
:fail
echo ERRO durante os testes. Leia as mensagens acima.
pause
exit /b 1
