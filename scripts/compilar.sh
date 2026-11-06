#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if ! command -v java >/dev/null 2>&1 || ! command -v javac >/dev/null 2>&1; then echo 'ERRO: instale o JDK 17+ (Temurin) e confira java -version.' >&2; exit 1; fi
java -version 2>&1 | grep -Eq '"(17|1[89]|[2-9][0-9])' || { echo 'ERRO: JDK 17 ou superior necessario.' >&2; exit 1; }
mkdir -p build/classes dist
rm -rf build/classes
mkdir -p build/classes
javac --release 17 -encoding UTF-8 -d build/classes -sourcepath src src/com/urnacidada/Main.java
if [ -d src/main/resources ]; then cp -R src/main/resources/. build/classes/; fi
jar --create --file dist/urna-cidada.jar --main-class com.urnacidada.Main -C build/classes .
