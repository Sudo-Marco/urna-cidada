#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
"$(dirname "$0")/compilar.sh"
mkdir -p dist
cat > dist/iniciar.sh <<'SH'
#!/bin/sh
set -eu
cd "$(dirname "$0")"
if ! command -v java >/dev/null 2>&1; then echo 'ERRO: instale JDK 17+ e confira java -version.' >&2; exit 1; fi
java -version 2>&1 | grep -Eq '"(17|1[89]|[2-9][0-9])' || { echo 'ERRO: JDK 17 ou superior necessario.' >&2; exit 1; }
exec java -jar urna-cidada.jar "$@"
SH
chmod +x dist/iniciar.sh
cp urna.properties.example dist/urna.properties.example
cat > dist/LEIA-ME.txt <<'TXT'
URNA CIDADA - SIMULACAO EDUCATIVA - NAO E UMA URNA OFICIAL
Requer JDK 17+. Execute iniciar.sh (Linux/macOS) ou iniciar.bat (Windows).
Para configurar Firebase, copie urna.properties.example para urna.properties e siga docs/firebase-configuracao.md.
TXT
