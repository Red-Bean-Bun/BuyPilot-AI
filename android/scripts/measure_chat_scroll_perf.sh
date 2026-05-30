#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android"
PACKAGE_NAME="${PACKAGE_NAME:-com.buypilot}"
LAUNCH_ACTIVITY="${LAUNCH_ACTIVITY:-com.buypilot/.SplashActivity}"
QUERY="${QUERY:-oil cleanser under 200}"
QUERY_SEQUENCE="${QUERY_SEQUENCE:-}"
MOCK_WAIT_SECONDS="${MOCK_WAIT_SECONDS:-25}"
SWIPE_COUNT="${SWIPE_COUNT:-12}"
OUT_DIR="${OUT_DIR:-/tmp/buypilot_chat_scroll_perf}"
STAMP="$(date +%Y%m%d_%H%M%S)"
RUN_DIR="$OUT_DIR/$STAMP"

mkdir -p "$RUN_DIR"

bundled_jdk="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home"
if [[ -d "$bundled_jdk" ]]; then
  current_java_version="$(
    if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
      "$JAVA_HOME/bin/java" -version 2>&1
    else
      java -version 2>&1 || true
    fi
  )"
  if [[ "$current_java_version" != *'version "1.'* && "$current_java_version" =~ version\ \"([0-9]+) ]]; then
    current_java_major="${BASH_REMATCH[1]}"
  else
    current_java_major="8"
  fi
  if (( current_java_major < 17 )); then
    export JAVA_HOME="$bundled_jdk"
  fi
fi

find_gradle() {
  if [[ -n "${GRADLE_BIN:-}" && -x "$GRADLE_BIN" ]]; then
    printf '%s\n' "$GRADLE_BIN"
    return
  fi
  if [[ -x "$ANDROID_DIR/gradlew" ]]; then
    printf '%s\n' "$ANDROID_DIR/gradlew"
    return
  fi
  if command -v gradle >/dev/null 2>&1; then
    command -v gradle
    return
  fi

  local cached
  cached="$(
    find "$HOME/.gradle/wrapper/dists" \
      -path '*/gradle-8*-bin/*/gradle-*/bin/gradle' \
      -type f \
      -perm -111 \
      2>/dev/null \
      | sort -V \
      | tail -n 1
  )"
  if [[ -n "$cached" ]]; then
    printf '%s\n' "$cached"
    return
  fi

  cached="$(
    find "$HOME/.gradle/wrapper/dists" \
      -path '*/gradle-*/bin/gradle' \
      -type f \
      -perm -111 \
      2>/dev/null \
      | sort -V \
      | tail -n 1
  )"
  if [[ -n "$cached" ]]; then
    printf '%s\n' "$cached"
    return
  fi

  echo "Could not find Gradle. Set GRADLE_BIN=/path/to/gradle." >&2
  exit 1
}

adb_device() {
  adb get-state >/dev/null
}

install_app() {
  local use_mock="$1"
  local task="$2"
  echo "==> Gradle $task -PUSE_MOCK_CHAT=$use_mock"
  (cd "$ANDROID_DIR" && "$GRADLE" "$task" -PUSE_MOCK_CHAT="$use_mock" --no-daemon)
}

wait_for_package() {
  local expected="$1"
  local deadline=$((SECONDS + 20))
  while (( SECONDS < deadline )); do
    local focused
    focused="$(adb shell dumpsys window 2>/dev/null | tr -d '\r' | grep -E 'mCurrentFocus|mFocusedApp' || true)"
    if [[ "$focused" == *"$expected"* ]]; then
      return 0
    fi
    sleep 1
  done
  return 1
}

wait_for_composer() {
  local deadline=$((SECONDS + 30))
  while (( SECONDS < deadline )); do
    if adb shell uiautomator dump /sdcard/buypilot-perf-window.xml >/dev/null 2>&1 &&
      adb exec-out cat /sdcard/buypilot-perf-window.xml | grep -q 'class="android.widget.EditText"'; then
      return 0
    fi
    sleep 0.5
  done
  return 1
}

composer_focused() {
  adb shell uiautomator dump /sdcard/buypilot-perf-window.xml >/dev/null 2>&1 &&
    adb exec-out cat /sdcard/buypilot-perf-window.xml |
      grep -E 'class="android.widget.EditText"[^>]*focused="true"' >/dev/null
}

tap_composer_and_send() {
  local query="$1"
  local size width height
  size="$(adb shell wm size | tr -d '\r' | awk -F': ' '/Physical size/ {print $2}')"
  width="${size%x*}"
  height="${size#*x}"

  local input_x=$((width * 30 / 100))
  local input_y=$((height - 220))
  local send_x=$((width - 110))
  local keyboard_send_y=$((height * 56 / 100))
  local encoded_query
  encoded_query="${query// /%s}"

  for _ in 1 2 3; do
    adb shell input tap "$input_x" "$input_y"
    sleep 0.8
    if composer_focused; then
      break
    fi
  done
  adb shell input text "$encoded_query"
  sleep 0.5
  adb shell input tap "$send_x" "$keyboard_send_y"
  sleep 0.5
  adb shell input keyevent KEYCODE_ESCAPE || true
}

perform_scrolls() {
  local size width height start_y end_y x
  size="$(adb shell wm size | tr -d '\r' | awk -F': ' '/Physical size/ {print $2}')"
  width="${size%x*}"
  height="${size#*x}"
  x=$((width / 2))
  start_y=$((height * 72 / 100))
  end_y=$((height * 50 / 100))

  for _ in $(seq 1 "$SWIPE_COUNT"); do
    adb shell input swipe "$x" "$start_y" "$x" "$end_y" 260
    sleep 0.12
  done
  for _ in $(seq 1 "$SWIPE_COUNT"); do
    adb shell input swipe "$x" "$end_y" "$x" "$start_y" 260
    sleep 0.12
  done
}

GRADLE="$(find_gradle)"
echo "Using Gradle: $GRADLE"
echo "Output: $RUN_DIR"

if [[ -n "$QUERY_SEQUENCE" ]]; then
  IFS='|' read -r -a QUERIES <<< "$QUERY_SEQUENCE"
else
  QUERIES=("$QUERY")
fi

adb_device
install_app true :app:installDebug

adb shell am force-stop "$PACKAGE_NAME" || true
adb shell pm clear "$PACKAGE_NAME" >/dev/null || true
adb shell cmd statusbar collapse >/dev/null 2>&1 || true
adb shell am start -n "$LAUNCH_ACTIVITY" >/dev/null
wait_for_package "$PACKAGE_NAME" || echo "Warning: package focus was not confirmed before input."

wait_for_composer || echo "Warning: composer was not confirmed before input."
for query in "${QUERIES[@]}"; do
  if [[ -z "$query" ]]; then
    continue
  fi
  echo "==> Sending query: $query"
  adb shell cmd statusbar collapse >/dev/null 2>&1 || true
  tap_composer_and_send "$query"
  adb shell cmd statusbar collapse >/dev/null 2>&1 || true
  sleep "$MOCK_WAIT_SECONDS"
done

adb shell dumpsys gfxinfo "$PACKAGE_NAME" reset >/dev/null || true
perform_scrolls
adb shell cmd statusbar collapse >/dev/null 2>&1 || true
sleep 0.5
adb exec-out screencap -p > "$RUN_DIR/scroll.png"
adb shell dumpsys gfxinfo "$PACKAGE_NAME" > "$RUN_DIR/gfxinfo.txt"
adb logcat -d -t 800 > "$RUN_DIR/logcat_tail.txt"

echo "==> Frame metrics"
grep -E \
  'Total frames rendered|Janky frames:|50th percentile|90th percentile|95th percentile|99th percentile|Number Missed Vsync|Number High input latency|Number Slow UI thread|Number Slow bitmap uploads|Number Slow issue draw commands' \
  "$RUN_DIR/gfxinfo.txt" || true

if grep -E 'FATAL EXCEPTION|AndroidRuntime: FATAL|ANR in|Process: com\.buypilot' "$RUN_DIR/logcat_tail.txt" >/dev/null; then
  echo "Runtime errors found in $RUN_DIR/logcat_tail.txt" >&2
else
  echo "No FATAL/ANR markers in logcat tail."
fi

echo "==> Reinstalling non-mock debug build"
install_app false :app:installDebug
adb shell am force-stop "$PACKAGE_NAME" || true
adb shell am start -n "$LAUNCH_ACTIVITY" >/dev/null || true

echo "Done."
echo "gfxinfo: $RUN_DIR/gfxinfo.txt"
echo "screenshot: $RUN_DIR/scroll.png"
