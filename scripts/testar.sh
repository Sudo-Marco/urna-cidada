#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/testes
rm -rf build/testes/*
javac --release 17 -encoding UTF-8 -d build/testes -sourcepath src:tests tests/com/urnacidada/testes/ExecutarTestes.java
java -Djava.awt.headless=true -cp build/testes com.urnacidada.testes.ExecutarTestes
