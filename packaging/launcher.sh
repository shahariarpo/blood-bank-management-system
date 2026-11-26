#!/bin/sh
# Launches the bundled jlink runtime directly, avoiding the jpackage native
# launcher (which emits a JDK 21 startup warning on some Linux builds).
set -e
HERE="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
APPDIR="$(CDPATH= cd -- "$HERE/../.." && pwd)"
JAVA="$APPDIR/usr/lib/bloodbank/lib/runtime/bin/java"
JAR="$APPDIR/usr/lib/bloodbank/lib/app/bloodbank.jar"
exec "$JAVA" -jar "$JAR" "$@"
