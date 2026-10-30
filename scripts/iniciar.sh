#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
"$(dirname "$0")/compilar.sh"
exec java -jar dist/urna-cidada.jar "$@"
