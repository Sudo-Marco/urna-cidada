@echo off
chcp 65001 >nul
cd /d "%~dp0.."
echo Compilando os testes (nao precisa de Maven nem de internet)...
if exist out\testes rmdir /s /q out\testes
mkdir out\testes
javac -encoding UTF-8 -d out\testes -sourcepath "src;tests" tests\com\urnacidada\testes\ExecutarTestes.java
if errorlevel 1 (
  echo.
  echo ERRO ao compilar os testes. Confira o JDK e as mensagens acima.
  exit /b 1
)
echo.
java -Djava.awt.headless=true -cp out\testes com.urnacidada.testes.ExecutarTestes
