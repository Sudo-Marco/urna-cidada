@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0.."
if not defined URNA_APP_VERSION set "URNA_APP_VERSION=1.0.14"

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
  echo 4. Ao terminar, a urna toca o sinal tipo pilili, sem voz, e retorna a identificacao em 5 segundos.
  echo 5. Se o Firebase ficar indisponivel ao entrar, escolha Continuar neste computador; o voto ficara local.
  echo    Esse voto offline nao sera enviado automaticamente e so pode ser contado neste computador.
  echo 6. UrnaCidadaAdmin.exe abre a administracao: apuracao local, restaurar estrutura sem apagar votos ou reiniciar.
  echo    Para reiniciar online, use a conta admin separada com permissao no Firestore.
  echo    O reinicio zera a votacao e libera novamente os CPFs sem apagar as contas cadastradas.
  echo.
  echo DADOS
  echo Sem configuracao Firebase, os votos ficam no computador em modo local.
  echo Com Firebase configurado e internet, a urna sincroniza as sessoes. Se a rede cair depois do login, guarda pendencias.
  echo O historico anonimo fica no computador mesmo depois de sincronizar e aparece na apuracao local offline.
  echo A apuracao local soma somente os votos deste computador; nao consulta o Firebase nem outras maquinas.
  echo Copie a pasta inteira para preservar os dados locais.
  echo Nunca envie urna.properties ao codigo-fonte.
  echo.
  echo Se o Windows bloquear o arquivo, use Propriedades e marque Desbloquear antes de abrir.
  echo O CPF nao e enviado nem gravado em texto; o app usa um identificador tecnico derivado do numero.
  echo A identificacao serve para a simulacao e nao consulta uma base oficial de titulares.
  echo O reset administrativo remove as sessoes, zera os nove contadores, rearma os participantes e preserva as colecoes.
  echo Contas dos CPFs, documentos de participantes e administradores permanecem no Firebase. Use Entrar depois do reset.
  echo Votos e historicos locais de outros computadores continuam apenas naquelas maquinas.
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
