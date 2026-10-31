#!/bin/sh
# Compares the working tree's behaviour with the legacy code (commit 532a86d).
# Expected result: only Lettuce Humidity, Lettuce Soil and Strawberry Soil differ.
set -e
cd "$(dirname "$0")/.."
TMP=$(mktemp -d)
mkdir -p "$TMP/old-src" "$TMP/old" "$TMP/new"
git archive 532a86d src/main/java/org/example/SVFRMS.java | tar -x -C "$TMP/old-src"
javac -d "$TMP/old" "$TMP/old-src/src/main/java/org/example/SVFRMS.java" \
    scripts/compare/common/org/example/Compare.java scripts/compare/legacy/org/example/Access.java
javac -d "$TMP/new" $(find src/main/java -name '*.java') \
    scripts/compare/common/org/example/Compare.java scripts/compare/current/org/example/Access.java
java -cp "$TMP/old" org.example.Compare "$TMP/old.txt"
java -cp "$TMP/new" org.example.Compare "$TMP/new.txt"
echo "Cases compared: $(wc -l < "$TMP/new.txt")"
echo "Cases that differ: $(diff "$TMP/old.txt" "$TMP/new.txt" | grep -c '^>' || true)"
diff "$TMP/old.txt" "$TMP/new.txt" | grep '^>' | sed 's/^> //' | cut -d' ' -f1-2 | cut -d, -f1,2 | sort | uniq -c || true
if [ -n "$KEEP_OUTPUT" ]; then cp "$TMP/old.txt" "$TMP/new.txt" "$KEEP_OUTPUT"; fi
rm -rf "$TMP"
