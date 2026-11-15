#!/usr/bin/env bash
set -eu

sdk_root="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$sdk_root" ]; then
    echo "Set ANDROID_SDK_ROOT or ANDROID_HOME before installing the Android SDK." >&2
    exit 1
fi

sdkmanager="$sdk_root/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$sdkmanager" ]; then
    mkdir -p "$sdk_root/cmdline-tools"
    curl -fsSL \
        "https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip" \
        -o "$sdk_root/commandlinetools.zip"
    unzip -q -o "$sdk_root/commandlinetools.zip" -d "$sdk_root/cmdline-tools"
    mv "$sdk_root/cmdline-tools/cmdline-tools" "$sdk_root/cmdline-tools/latest"
    rm "$sdk_root/commandlinetools.zip"
fi

yes | "$sdkmanager" --licenses >/dev/null
"$sdkmanager" --install \
    "platform-tools" \
    "platforms;android-36" \
    "build-tools;36.0.0" \
    "cmake;3.22.1" \
    "ndk;27.0.12077973"
