@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."

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
jpackage --type app-image --name UrnaCidada --input dist\entrada-portatil --main-jar urna-cidada.jar --main-class com.urnacidada.Main --app-version 1.0.0 --vendor "Projeto Urna Cidada" --dest dist\pacote-portatil
if errorlevel 1 goto :package_error

copy /y urna.properties.example dist\pacote-portatil\UrnaCidada\app\urna.properties.example >nul
if /i "%INCLUIR_FIREBASE_RELEASE%"=="1" if exist urna.properties copy /y urna.properties dist\pacote-portatil\UrnaCidada\app\urna.properties >nul
(
  echo URNA CIDADA - SIMULACAO EDUCATIVA - NAO E UMA URNA OFICIAL
  echo.
  echo INICIO RAPIDO
  echo 1. Extraia a pasta UrnaCidada para Documentos ou Area de Trabalho.
  echo 2. De um duplo clique em UrnaCidada.exe. Nao precisa instalar Java, VS Code, Git ou Maven.
  echo 3. Para uma votacao, escolha candidatos e confirme cada cargo; ao final use Iniciar nova votacao.
  echo.
  echo DADOS
  echo Sem urna.properties, os votos ficam no computador em modo local. Copie a pasta inteira para preservar os dados.
  echo Sem configuracao Firebase, os votos ficam locais. Um pacote publico com Firebase so deve ser criado pelo workflow e apos restringir a chave publica a API do Firestore.
  echo Nunca envie urna.properties ao codigo-fonte.
  echo.
  echo Se o Windows bloquear o arquivo, use Propriedades e marque Desbloquear antes de abrir.
  echo Esta simulacao nao e um sistema eleitoral oficial e nao coleta dados pessoais.
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
