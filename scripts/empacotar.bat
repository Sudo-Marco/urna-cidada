@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."
if not defined URNA_APP_VERSION set "URNA_APP_VERSION=1.0.4"

call scripts\compilar.bat
if errorlevel 1 exit /b 1

where jpackage >nul 2>nul
if errorlevel 1 goto :jpackage_error

if exist dist\entrada-portatil rmdir /s /q dist\entrada-portatil
if exist dist\pacote-portatil rmdir /s /q dist\pacote-portatil
if exist dist\UrnaCidada-Windows-x64.zip del /q dist\UrnaCidada-Windows-x64.zip
mkdir dist\entrada-portatil
copy /y dist\urna-cidada.jar dist\entrada-portatil\urna-cidada.jar >nul

echo Criando aplicativo portatil com Java incluido...
jpackage --type app-image --name UrnaCidada --input dist\entrada-portatil --main-jar urna-cidada.jar --main-class com.urnacidada.Main --add-launcher "UrnaCidadaAdmin=scripts\administrador.properties" --app-version "%URNA_APP_VERSION%" --vendor "Projeto Urna Cidada" --dest dist\pacote-portatil
if errorlevel 1 goto :package_error

copy /y urna.properties.example dist\pacote-portatil\UrnaCidada\app\urna.properties.example >nul
if /i "%INCLUIR_FIREBASE_RELEASE%"=="1" if exist urna.properties copy /y urna.properties dist\pacote-portatil\UrnaCidada\app\urna.properties >nul
(
  echo URNA CIDADA - SIMULACAO EDUCATIVA - NAO E UMA URNA OFICIAL
  echo.
  echo COMPATIBILIDADE
  echo Pacote para Windows 64 bits. O Java ja esta incluido; nao instale JDK, Maven ou VS Code.
  echo A aplicacao usa interface grafica Java Swing. Nao precisa de navegador.
  echo.
  echo INICIO RAPIDO
  echo 1. Extraia o ZIP inteiro para Documentos ou Area de Trabalho.
  echo 2. Abra a pasta UrnaCidada e de duplo clique em UrnaCidada.exe.
  echo 3. Com Firebase, identifique-se somente com CPF. No primeiro uso, escolha Primeiro acesso.
  echo 4. Ao terminar, a urna toca o sinal, fala a mensagem de encerramento e retorna a identificacao em 5 segundos.
  echo 5. Sem Firebase, a urna funciona localmente, sem bloqueio de repeticao entre computadores.
  echo 6. UrnaCidadaAdmin.exe abre a limpeza administrativa; exige conta admin separada e permissao no Firestore.
  echo    A limpeza apaga dados do Firestore e arquivos de voto apenas deste computador.
  echo.
  echo DADOS
  echo Sem configuracao Firebase, os votos ficam no computador em modo local.
  echo Com Firebase configurado e internet, a urna sincroniza as sessoes. Sem internet, mantem pendencias locais.
  echo Copie a pasta inteira para preservar os dados locais.
  echo Nunca envie urna.properties ao codigo-fonte.
  echo.
  echo Se o Windows bloquear o arquivo, use Propriedades e marque Desbloquear antes de abrir.
  echo O CPF nao e enviado nem gravado em texto; o app usa um identificador tecnico derivado do numero.
  echo A identificacao serve para a simulacao e nao consulta uma base oficial de titulares.
  echo O botao administrativo apaga sessoes, contadores e marcas de voto; esta acao nao pode ser desfeita.
  echo Votos pendentes em outros computadores podem voltar a ser enviados quando a conexao retornar.
) > dist\pacote-portatil\UrnaCidada\LEIA-ME.txt

powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -Path 'dist\pacote-portatil\UrnaCidada' -DestinationPath 'dist\UrnaCidada-Windows-x64.zip' -Force"
if errorlevel 1 goto :zip_error

echo.
echo Pacote pronto: dist\UrnaCidada-Windows-x64.zip
echo Ele inclui um runtime Java e nao exige JDK no computador de destino.
exit /b 0

:jpackage_error
echo ERRO: nao encontrei jpackage. Instale um JDK 17 ou superior completo e confira: jpackage --version
pause
exit /b 1

:package_error
echo ERRO ao criar o aplicativo portatil. Confira as mensagens do jpackage acima.
pause
exit /b 1

:zip_error
echo ERRO ao compactar o pacote. Confira se o PowerShell esta disponivel e se ha espaco em disco.
pause
exit /b 1
