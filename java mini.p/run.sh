#!/bin/sh
# LABSTOCK - compile and run (macOS / Linux)
cd "$(dirname "$0")"
mkdir -p out
javac -encoding UTF-8 -d out -sourcepath src src/labstock/Main.java || exit 1
java -cp out labstock.Main
