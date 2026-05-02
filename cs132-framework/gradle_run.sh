#!/usr/bin/env bash
set -e

if [[ "$1" -lt 1 || "$1" -gt 5 ]] 2>/dev/null; then
    echo "Usage: $0 <1-5>"
    exit 1
fi

JAVA_HOME=/opt/homebrew/Cellar/openjdk/25.0.2/libexec/openjdk.jdk/Contents/Home \
    gradle run -Phomework="hw$1"
