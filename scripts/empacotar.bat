@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."
call scripts\compilar.bat
if errorlevel 1 exit /b 1
copy /y scripts\iniciar-pacote.bat dist\iniciar.bat >nul
copy /y urna.properties.example dist\urna.properties.example >nul
(
 echo URNA CIDADA - SIMULACAO EDUCATIVA - NAO E UMA URNA OFICIAL
 echo Requer JDK 17 ou superior. Execute iniciar.bat.
 echo Para configurar o Firebase, copie urna.properties.example para urna.properties e siga docs/firebase-configuracao.md.
 echo Os dados ficam na pasta dados ao lado do JAR ou no perfil do usuario quando nao houver permissao.
 echo Nunca compartilhe urna.properties; mantenha a chave fora do Git.
) > dist\LEIA-ME.txt
echo Pacote pronto em dist\
