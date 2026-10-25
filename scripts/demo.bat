@echo off
chcp 65001 >nul
rem Modo demonstracao: nada e salvo ao fechar (nao usa internet nem Firestore).
call "%~dp0rodar.bat" --memoria
