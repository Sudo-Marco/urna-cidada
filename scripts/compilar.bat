@echo off
chcp 65001 >nul
cd /d "%~dp0.."
echo Compilando com Maven...
mvn -q clean compile
if errorlevel 1 (
  echo.
  echo ERRO ao compilar. Confira o JDK, o Maven e as mensagens acima.
  exit /b 1
)
echo Compilado com sucesso.
