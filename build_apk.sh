#!/bin/bash
set -e
echo "🔥 Starting full Codespace APK build..."

# Rename folder to avoid spaces
PROJECT_DIR="monitor_apk_full"
if [ ! -d "$PROJECT_DIR" ]; then
    mv "monitor - apk - full" "$PROJECT_DIR"
fi
cd "$PROJECT_DIR"

# Fix build.gradle plugin versions
BUILD_FILE="build.gradle"
if ! grep -q "version" "$BUILD_FILE"; then
    echo "📝 Fixing build.gradle plugin versions..."
    sed -i '1i\
plugins { \
    id '\''com.android.application'\'' version '\''8.2.1'\'' apply false; \
    id '\''org.jetbrains.kotlin.android'\'' version '\''1.9.0'\'' apply false; \
}' "$BUILD_FILE"

    sed -i '2i apply plugin: '\''com.android.application'\''\napply plugin: '\''org.jetbrains.kotlin.android'\''' "$BUILD_FILE"
fi

# Generate Gradle wrapper
echo "⚡ Generating Gradle wrapper..."
gradle wrapper --gradle-version 9.2.1
chmod +x gradlew

# Install Android SDK + Build Tools
echo "📦 Installing Android SDK + Build Tools..."
sudo apt update && sudo apt install -y openjdk-21 unzip wget
mkdir -p $HOME/Android
wget https://dl.google.com/android/repository/commandlinetools-linux-9123335_latest.zip -O cmdline-tools.zip
unzip -q cmdline-tools.zip -d $HOME/Android
export ANDROID_HOME=$HOME/Android/cmdline-tools/latest
export PATH=$PATH:$ANDROID_HOME/bin:$PATH

echo "✅ Installing platform-tools, build-tools, and SDK platform..."
yes | sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Clean project
echo "🧹 Cleaning project..."
./gradlew clean

# Build Debug APK
echo "📱 Building Debug APK..."
./gradlew assembleDebug

# Show APK path
echo "🎯 Build finished! APK location:"
echo "$(pwd)/app/build/outputs/apk/debug/app-debug.apk"

echo "🔥 All done, bro!"
