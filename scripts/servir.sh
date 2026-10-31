#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
"$(dirname "$0")/compilar.sh"
exec java --add-modules jdk.httpserver -cp build/classes com.urnacidada.WebMain "$@"
