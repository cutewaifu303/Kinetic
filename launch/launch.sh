#!/usr/bin/env bash
# Starts the Kinetic client without the launcher.
# Looks for Java 8 in client/jre, then in the launcher's data folder (downloaded on first launcher start),
# then JAVA_HOME / java on PATH. Assets: client/assets, else the launcher's data folder.
cd "$(dirname "$0")"
CLIENT="$(pwd)/client"
OS="$(uname -s)"
if [ "$OS" = "Darwin" ]; then
    DATA="$HOME/Library/Application Support/KineticClient"; NATIVES="$CLIENT/natives/macos"
else
    DATA="${XDG_DATA_HOME:-$HOME/.local/share}/KineticClient"; NATIVES="$CLIENT/natives/linux"
fi
GAMEDIR="$HOME/.minecraft"
JAVA=""
for c in "$CLIENT/jre/bin/java" "$CLIENT/jre/jre/bin/java" "$DATA/jre/bin/java" "$DATA/jre/Contents/Home/bin/java" "${JAVA_HOME:+$JAVA_HOME/bin/java}"; do
    [ -n "$c" ] && [ -x "$c" ] && JAVA="$c" && break
done
[ -z "$JAVA" ] && command -v java >/dev/null 2>&1 && JAVA="$(command -v java)"
if [ -z "$JAVA" ]; then
    echo "[Kinetic] No Java found. Start Kinetic.jar once (it downloads Java 8) or install Java 8."
    exit 1
fi
if [ -d "$CLIENT/assets/indexes" ]; then ASSETS="$CLIENT/assets"; elif [ -d "$DATA/assets/indexes" ]; then ASSETS="$DATA/assets"; else
    echo "[Kinetic] No assets found. Start Kinetic.jar once, it downloads them to $DATA/assets."
    exit 1
fi
if [ ! -f "$CLIENT/Kinetic.jar" ]; then echo "[Kinetic] client/Kinetic.jar not found."; exit 1; fi
mkdir -p "$GAMEDIR"
echo "[Kinetic] Java: $JAVA"
echo "[Kinetic] Assets: $ASSETS"
echo "[Kinetic] Game dir: $GAMEDIR"
JAVA_LIB="$(dirname "$JAVA")/../lib/amd64"
export LD_LIBRARY_PATH="$NATIVES:$JAVA_LIB${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
"$JAVA" -Dorg.lwjgl.librarypath="$NATIVES" -Xmx4G -Xms2G -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=20 -XX:G1HeapRegionSize=32M -XX:+ParallelRefProcEnabled -XX:-UsePerfData -jar "$CLIENT/Kinetic.jar" --version Kinetic --accessToken 0 --assetsDir "$ASSETS" --assetIndex 1.8 --gameDir "$GAMEDIR" --userProperties {}
