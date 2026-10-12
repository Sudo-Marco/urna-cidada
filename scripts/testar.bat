@echo off
chcp 65001 >nul
call "%~dp0compilar.bat"
if errorlevel 1 exit /b 1
cd /d "%~dp0.."
javac -encoding UTF-8 -d out -sourcepath src tests\TesteUrna.java
if errorlevel 1 (
  echo ERRO ao compilar os testes.
  exit /b 1
)
echo.
java -cp out TesteUrna
