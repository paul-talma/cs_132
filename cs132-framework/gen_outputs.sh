#! /usr/bin/env bash

JAVA_PATH="/opt/homebrew/opt/openjdk@21"
TESTCASES_PATH="testcases/hw4"

for file in $TESTCASES_PATH/*.sparrow; do
    [ -f "$file" ] || continue
    echo "${file}"
    java -jar misc/sparrow.jar s <$file >$file.out
done
