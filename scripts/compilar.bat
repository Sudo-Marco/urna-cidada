@echo off
chcp 65001 >nul
cd /d "%~dp0.."
if not exist out mkdir out
javac -encoding UTF-8 -d out -sourcepath src src\com\urnacidada\Main.java
if errorlevel 1 (
  echo.
  echo ERRO ao compilar. Veja as mensagens acima.
  exit /b 1
)
echo Compilado com sucesso.
