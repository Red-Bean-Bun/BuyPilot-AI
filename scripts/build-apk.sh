#!/bin/bash
# Android Release APK 编译脚本
# 用法: ./scripts/build-apk.sh [version]
# 示例: ./scripts/build-apk.sh 0.2.0

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ANDROID_DIR="$PROJECT_ROOT/android"
DIST_DIR="$PROJECT_ROOT/dist"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log() { echo -e "${GREEN}[INFO]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

java_major_version() {
    local version="$1"
    if [[ "$version" == 1.* ]]; then
        echo "$version" | cut -d'.' -f2
    else
        echo "$version" | cut -d'.' -f1
    fi
}

inplace_sed() {
    local expression="$1"
    local file="$2"
    sed -i.bak "$expression" "$file"
    rm -f "$file.bak"
}

# 检查环境
check_env() {
    log "检查编译环境..."

    # JDK
    if ! command -v java &> /dev/null; then
        error "JDK 未安装，请安装 JDK 17"
    fi
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
    JAVA_MAJOR=$(java_major_version "$JAVA_VERSION")
    if [ "$JAVA_MAJOR" -lt 17 ]; then
        if command -v /usr/libexec/java_home &> /dev/null; then
            for CANDIDATE_VERSION in 17 21; do
                CANDIDATE_HOME=$(/usr/libexec/java_home -v "$CANDIDATE_VERSION" 2>/dev/null || true)
                if [ -n "$CANDIDATE_HOME" ]; then
                    export JAVA_HOME="$CANDIDATE_HOME"
                    export PATH="$JAVA_HOME/bin:$PATH"
                    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
                    JAVA_MAJOR=$(java_major_version "$JAVA_VERSION")
                    break
                fi
            done
        fi
    fi

    if [ "$JAVA_MAJOR" -lt 17 ]; then
        error "JDK 版本: $JAVA_VERSION，Android Gradle Plugin 需要 JDK 17+"
    fi
    if [ "$JAVA_MAJOR" != "17" ]; then
        warn "JDK 版本: $JAVA_VERSION (推荐 JDK 17，当前版本可用于构建)"
    fi
    log "使用 JDK: ${JAVA_HOME:-$(dirname "$(dirname "$(command -v java)")")} ($JAVA_VERSION)"

    # Android SDK
    if [ -z "$ANDROID_HOME" ]; then
        if [ -n "$ANDROID_SDK_ROOT" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
            export ANDROID_HOME="$ANDROID_SDK_ROOT"
            log "自动设置 ANDROID_HOME=$ANDROID_HOME"
        elif [ -d "$HOME/Library/Android/sdk" ]; then
            export ANDROID_HOME="$HOME/Library/Android/sdk"
            log "自动设置 ANDROID_HOME=$ANDROID_HOME"
        elif [ -d "$HOME/Android/Sdk" ]; then
            export ANDROID_HOME="$HOME/Android/Sdk"
            log "自动设置 ANDROID_HOME=$ANDROID_HOME"
        else
            error "ANDROID_HOME 未设置，请设置环境变量"
        fi
    fi
    export ANDROID_SDK_ROOT="$ANDROID_HOME"

    if [ ! -d "$ANDROID_HOME/platforms/android-35" ]; then
        error "Android SDK API 35 未安装"
    fi

    log "环境检查通过 ✓"
}

# 更新版本号（如果提供了参数）
update_version() {
    if [ -n "$1" ]; then
        log "更新版本号到 $1..."
        local GRADLE_FILE="$ANDROID_DIR/app/build.gradle.kts"

        # 更新 versionName
        inplace_sed "s/versionName = \"[^\"]*\"/versionName = \"$1\"/" "$GRADLE_FILE"

        # 递增 versionCode
        local CURRENT_CODE=$(grep "versionCode = " "$GRADLE_FILE" | grep -o '[0-9]\+')
        local NEW_CODE=$((CURRENT_CODE + 1))
        inplace_sed "s/versionCode = $CURRENT_CODE/versionCode = $NEW_CODE/" "$GRADLE_FILE"

        log "版本号已更新: versionName=$1, versionCode=$NEW_CODE"
    fi
}

# 编译 APK
build_apk() {
    log "开始编译 Release APK..."

    cd "$ANDROID_DIR"

    # 清理（可选）
    if [ "$1" = "clean" ]; then
        log "清理旧构建..."
        ./gradlew clean
    fi

    # 编译
    log "执行 assembleRelease..."
    ./gradlew assembleRelease --no-daemon

    log "编译完成 ✓"
}

# 复制输出
copy_output() {
    mkdir -p "$DIST_DIR"

    local VERSION=$(grep "versionName = " "$ANDROID_DIR/app/build.gradle.kts" | cut -d'"' -f2)
    local SRC="$ANDROID_DIR/app/build/outputs/apk/release/app-release.apk"
    local DEST="$DIST_DIR/BuyPilot-v${VERSION}-release.apk"

    cp "$SRC" "$DEST"

    log "APK 已复制到: $DEST"
    ls -lh "$DEST"
}

# 验证 APK
verify_apk() {
    local APK_PATH="$DIST_DIR/BuyPilot-v*-release.apk"

    log "验证 APK 内容..."

    # 检查内置后端地址
    local BASE_URL=$(unzip -p $APK_PATH classes.dex 2>/dev/null | strings | grep -o 'https://api\.[a-z.]*' | head -1)

    if [ -z "$BASE_URL" ]; then
        # 尝试其他模式
        BASE_URL=$(unzip -p $APK_PATH classes.dex 2>/dev/null | strings | grep -o 'http://[0-9.]*:[0-9]*' | head -1)
    fi

    if [ -n "$BASE_URL" ]; then
        log "内置后端地址: $BASE_URL"
    else
        warn "无法确认内置后端地址，请手动检查"
    fi
}

# 主流程
main() {
    log "=== BuyPilot APK 编译脚本 ==="
    echo

    check_env
    update_version "$1"
    build_apk "$2"
    copy_output
    verify_apk

    echo
    log "✅ 编译完成!"
    log "APK 路径: $DIST_DIR/BuyPilot-v*-release.apk"
    echo
    log "下一步:"
    echo "  1. 安装到设备: adb install -r $DIST_DIR/BuyPilot-v*-release.apk"
    echo "  2. 提交版本: git add -A && git commit -m 'release: v$(grep versionName $ANDROID_DIR/app/build.gradle.kts | cut -d'"' -f2)'"
}

# 帮助
show_help() {
    echo "用法: $0 [version] [clean]"
    echo
    echo "参数:"
    echo "  version  新版本号 (例如: 0.2.0)"
    echo "  clean    清理旧构建 (可选)"
    echo
    echo "示例:"
    echo "  $0                    # 使用当前版本号编译"
    echo "  $0 0.2.0              # 更新版本号并编译"
    echo "  $0 0.2.0 clean        # 清理并编译新版本"
    echo
}

# 参数解析
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_help
    exit 0
fi

main "$1" "$2"
