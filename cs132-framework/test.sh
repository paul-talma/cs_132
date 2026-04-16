#!/usr/bin/env bash
set -e

GRADLE_JAVA=/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home
RUN_JAVA=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/bin/java
CLASSPATH="build/classes/java/main:lib/cs132.jar"
TESTDIR="testcases/hw2"

# Build once
JAVA_HOME=$GRADLE_JAVA gradle classes -Phomework=hw2 -q

run_test() {
    local file="$1"
    local expected_file="${file}.out"
    if [ ! -f "$expected_file" ]; then
        echo "SKIP: $file (no .out file)"
        return
    fi
    local actual expected
    actual=$($RUN_JAVA -cp "$CLASSPATH" Typecheck < "$file" 2>/dev/null)
    expected=$(cat "$expected_file")
    if [ "$actual" = "$expected" ]; then
        echo "PASS: $(basename "$file")"
        return 0
    else
        echo "FAIL: $(basename "$file")"
        echo "  expected: $expected"
        echo "  actual:   $actual"
        return 1
    fi
}

if [ -n "$1" ]; then
    # Single test — accept full path or bare name like BubbleSort or BubbleSort.java
    arg="$1"
    if [ ! -f "$arg" ]; then
        arg="$TESTDIR/${arg%.java}.java"
    fi
    run_test "$arg"
else
    # All tests
    pass=0; fail=0
    for file in "$TESTDIR"/*.java; do
        if run_test "$file"; then
            ((pass++)) || true
        else
            ((fail++)) || true
        fi
    done
    echo ""
    echo "Results: $pass passed, $fail failed out of $((pass + fail))"
fi
