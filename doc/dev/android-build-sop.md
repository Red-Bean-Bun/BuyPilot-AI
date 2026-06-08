# Android APK 编译 SOP

> 最后更新：2026-06-09  
> 适用版本：BuyPilot v0.1.0

---

## 一、环境要求

| 组件 | 版本 | 安装方式 |
|------|------|----------|
| JDK | 17 | `sudo apt install openjdk-17-jdk` |
| Android SDK | API 35 | Android Studio 或手动下载 |
| Gradle | 8.9 | 项目自带 gradle wrapper |

### 环境变量

```bash
export ANDROID_HOME=/path/to/android-sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools
```

macOS 默认路径：`~/Library/Android/sdk`  
Linux 默认路径：`~/Android/Sdk` 或自定义路径

---

## 二、签名证书

### 已有证书（推荐）

项目已包含签名证书：

```
android/app/release.jks
```

配置信息（`app/build.gradle.kts`）：

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("release.jks")
        storePassword = "buypilot2026"
        keyAlias = "buypilot"
        keyPassword = "buypilot2026"
    }
}
```

### 重新生成证书（如需）

```bash
cd android/app

keytool -genkey -v \
  -keystore release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias buypilot \
  -storepass YOUR_PASSWORD \
  -keypass YOUR_PASSWORD \
  -dname "CN=BuyPilot, OU=Dev, O=BuyPilot, L=Beijing, ST=Beijing, C=CN"
```

⚠️ **重要**：更新密码后需同步修改 `build.gradle.kts` 中的 `storePassword` 和 `keyPassword`。

---

## 三、编译 Release APK

### 步骤

```bash
# 1. 进入 Android 项目目录
cd android

# 2. 清理（可选，首次编译可跳过）
./gradlew clean

# 3. 编译 Release APK
./gradlew assembleRelease

# 4. 查看输出
ls -lh app/build/outputs/apk/release/
```

### 输出路径

```
android/app/build/outputs/apk/release/app-release.apk
```

### 复制到项目根目录（推荐）

```bash
mkdir -p ../dist
cp app/build/outputs/apk/release/app-release.apk ../dist/BuyPilot-v0.1.0-release.apk
```

---

## 四、内置后端地址

APK 内置的后端地址由 `.env` 文件控制：

```bash
# 项目根目录 .env
BUY_PILOT_BASE_URL=https://api.lzjyyds.top
ADMIN_API_KEY=your-admin-key
```

**编译时**：Gradle 读取 `.env` 并写入 `BuildConfig.BUY_PILOT_BASE_URL`

**验证**：

```bash
# 解压 APK 检查内置地址
unzip -p dist/BuyPilot-v0.1.0-release.apk classes.dex | strings | grep "api.lzjyyds"
# 应输出：https://api.lzjyyds.top
```

### 切换后端地址

```bash
# 1. 修改 .env
echo "BUY_PILOT_BASE_URL=http://192.168.1.100:8000" > .env

# 2. 重新编译
cd android && ./gradlew assembleRelease
```

---

## 五、调试版 APK（开发用）

```bash
cd android
./gradlew assembleDebug

# 输出路径
ls app/build/outputs/apk/debug/app-debug.apk
```

调试版特点：
- 未压缩，体积较大
- 包含调试信息
- 可安装到模拟器/真机

---

## 六、安装到设备

### USB 连接

```bash
# 查看已连接设备
adb devices

# 安装 APK
adb install dist/BuyPilot-v0.1.0-release.apk

# 覆盖安装
adb install -r dist/BuyPilot-v0.1.0-release.apk
```

### 无线调试

```bash
# 1. 设备开启无线调试（开发者选项）
# 2. 连接（首次需配对）
adb pair <ip>:<port>
adb connect <ip>:<port>

# 3. 安装
adb install dist/BuyPilot-v0.1.0-release.apk
```

---

## 七、常见问题

### Q1: `SDK location not found`

**原因**：未设置 `ANDROID_HOME` 环境变量

**解决**：
```bash
# 临时设置
export ANDROID_HOME=/path/to/android-sdk

# 永久设置（添加到 ~/.bashrc 或 ~/.zshrc）
echo 'export ANDROID_HOME=/path/to/android-sdk' >> ~/.bashrc
source ~/.bashrc
```

### Q2: `Could not determine java version`

**原因**：JDK 版本不匹配（需要 JDK 17）

**解决**：
```bash
# 检查当前版本
java -version

# 切换到 JDK 17
sudo update-alternatives --config java
# 选择 java-17-openjdk
```

### Q3: `Execution failed for task ':app:mergeReleaseResources'`

**原因**：Gradle 缓存损坏

**解决**：
```bash
cd android
./gradlew clean
rm -rf .gradle build app/build
./gradlew assembleRelease
```

### Q4: APK 安装失败 `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

**原因**：签名不一致（之前安装过其他签名的版本）

**解决**：
```bash
# 卸载旧版本
adb uninstall com.buypilot

# 重新安装
adb install dist/BuyPilot-v0.1.0-release.apk
```

### Q5: 编译耗时过长

**原因**：首次编译需下载依赖

**解决**：
- 首次编译约 5-10 分钟（正常）
- 后续编译使用缓存，约 1-2 分钟
- 使用 Gradle daemon：`./gradlew --daemon assembleRelease`

---

## 八、版本管理

### 更新版本号

编辑 `android/app/build.gradle.kts`：

```kotlin
defaultConfig {
    versionCode = 2           // 递增（整数）
    versionName = "0.2.0"     // 语义化版本
}
```

### Git 标签

```bash
git tag -a v0.1.0 -m "Release v0.1.0"
git push origin v0.1.0
```

---

## 九、自动化脚本（可选）

创建 `scripts/build-apk.sh`：

```bash
#!/bin/bash
set -e

echo "🔨 Building BuyPilot Release APK..."

cd android

# 清理
./gradlew clean

# 编译
./gradlew assembleRelease

# 复制
mkdir -p ../dist
cp app/build/outputs/apk/release/app-release.apk \
   ../dist/BuyPilot-v$(grep versionName app/build.gradle.kts | cut -d'"' -f2)-release.apk

echo "✅ APK ready: dist/BuyPilot-v*-release.apk"
ls -lh ../dist/*.apk
```

使用：

```bash
chmod +x scripts/build-apk.sh
./scripts/build-apk.sh
```

---

## 十、快速参考

```bash
# 一键编译
cd android && ./gradlew assembleRelease && cp app/build/outputs/apk/release/app-release.apk ../dist/

# 一键安装
adb install -r dist/app-release.apk

# 查看日志
adb logcat | grep -i buypilot
```

---

**文档维护者**：BuyPilot 团队  
**联系方式**：项目内部沟通
