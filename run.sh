#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p out
javac -Xlint:all -d out src/smartstay/*.java
java -cp out smartstay.EUK00361749_Cli
