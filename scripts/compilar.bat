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
echo Compilando com JDK 17 ou superior...
if not exist build mkdir build
if exist build\classes rmdir /s /q build\classes
mkdir build\classes
javac --release 17 -encoding UTF-8 -d build\classes -sourcepath src src\com\urnacidada\Main.java
if errorlevel 1 goto :fail
if not exist dist mkdir dist
jar --create --file dist\urna-cidada.jar --main-class com.urnacidada.Main -C build\classes .
if errorlevel 1 goto :fail
echo Aplicativo compilado: dist\urna-cidada.jar
exit /b 0
:java_error
echo ERRO: Java 17 ou superior (JDK) nao encontrado. Instale o Temurin JDK 17+, abra um novo terminal e confira com: java -version
pause
exit /b 1
:fail
echo ERRO ao compilar. Confira o JDK 17+ e as mensagens acima.
pause
exit /b 1
