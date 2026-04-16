#!/usr/bin/env bash
# Run all hw1 test cases and report results.
# All generated files (compiled classes, output) go into _test_output/ only.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src/main/java"
TEST_DIR="$SCRIPT_DIR/testcases/hw1"
OUT_DIR="$SCRIPT_DIR/_test_output"

# --- Setup ---
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR/classes" "$OUT_DIR/stdout"

# --- Compile ---
echo "Compiling..."
if ! javac -sourcepath "$SRC_DIR" "$SRC_DIR/Parse.java" -d "$OUT_DIR/classes" 2>"$OUT_DIR/compile.err"; then
    echo "COMPILE ERROR:"
    cat "$OUT_DIR/compile.err"
    exit 1
fi
echo "Compile OK"
echo ""

# --- Run tests ---
PASS=0
FAIL=0

for input_file in "$TEST_DIR"/*; do
    # Skip .out files
    [[ "$input_file" == *.out ]] && continue

    name="$(basename "$input_file")"
    expected_file="$input_file.out"

    if [[ ! -f "$expected_file" ]]; then
        echo "SKIP $name  (no .out file)"
        continue
    fi

    actual="$(java -cp "$OUT_DIR/classes" Parse < "$input_file" 2>/dev/null)"
    expected="$(cat "$expected_file")"

    if [[ "$actual" == "$expected" ]]; then
        echo "PASS  $name"
        (( PASS++ )) || true
    else
        echo "FAIL  $name"
        echo "      expected: $expected"
        echo "      actual:   $actual"
        (( FAIL++ )) || true
    fi
done

echo ""
echo "Results: $PASS passed, $FAIL failed out of $(( PASS + FAIL )) tests"
