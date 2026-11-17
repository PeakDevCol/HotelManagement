#!/bin/bash
# Setup script for Claude Code cloud environments (claude.ai/code).
# Paste it in the environment's "Setup script" field. It installs the Android SDK that the
# build needs: the cloud image has Java and Gradle, but no Android SDK.
# The environment must allow the domain dl.google.com, and must define the variable
# ANDROID_HOME=/opt/android-sdk so Gradle finds the SDK in every session.
set -e

ANDROID_HOME=/opt/android-sdk
TOOLS_ZIP=commandlinetools-linux-14742923_latest.zip

mkdir -p "$ANDROID_HOME/cmdline-tools"
curl -fsSL -o "/tmp/$TOOLS_ZIP" "https://dl.google.com/android/repository/$TOOLS_ZIP"
unzip -q -o "/tmp/$TOOLS_ZIP" -d "$ANDROID_HOME/cmdline-tools"
rm -rf "$ANDROID_HOME/cmdline-tools/latest"
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"

SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
yes | "$SDKMANAGER" --licenses > /dev/null || true
# The versions of gradle/libs.versions.toml (android-compileSdk) and of the Android Gradle plugin.
"$SDKMANAGER" "platform-tools" "platforms;android-37.0" "build-tools;36.0.0" > /dev/null
