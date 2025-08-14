#!/bin/bash

# E2E Test Runner for B-Scan Android App
# This script sets up and runs instrumented tests locally

set -e

echo "🚀 B-Scan E2E Test Runner"
echo "=========================="

# Function to check if emulator is running
check_emulator() {
    local device_count
    device_count=$(adb devices | grep -c "emulator-" || true)
    return $((device_count == 0))
}

# Function to wait for emulator boot
wait_for_emulator() {
    echo "⏳ Waiting for emulator to boot..."
    local timeout=300  # 5 minutes
    local elapsed=0
    
    while [ $elapsed -lt $timeout ]; do
        if adb shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; then
            echo "✅ Emulator is ready!"
            return 0
        fi
        sleep 5
        elapsed=$((elapsed + 5))
        echo "   Still waiting... (${elapsed}s)"
    done
    
    echo "❌ Timeout waiting for emulator"
    return 1
}

# Check if Android SDK is properly configured
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "❌ Android SDK not configured. Please set ANDROID_HOME or ANDROID_SDK_ROOT"
    exit 1
fi

# Set SDK path
SDK_PATH="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
if [ ! -d "$SDK_PATH" ]; then
    echo "❌ Android SDK not found at: $SDK_PATH"
    exit 1
fi

echo "📱 Using Android SDK: $SDK_PATH"

# Check for available emulator
echo "🔍 Checking for Android emulator..."
if [ ! -f "$SDK_PATH/emulator/emulator" ]; then
    echo "❌ Android emulator not found. Please install it via Android SDK Manager"
    exit 1
fi

# List available AVDs
echo "📋 Available Android Virtual Devices:"
avd_list=$("$SDK_PATH/emulator/emulator" -list-avds)
if [ -z "$avd_list" ]; then
    echo "❌ No AVDs found. Creating a test AVD..."
    echo "   Please install a system image first:"
    echo "   sdkmanager 'system-images;android-30;google_apis;arm64-v8a'"
    exit 1
fi

echo "$avd_list"

# Use first available AVD
avd_name=$(echo "$avd_list" | head -n1)
echo "🎯 Using AVD: $avd_name"

# Check if emulator is already running
if ! check_emulator; then
    echo "📱 Emulator already running"
else
    echo "🚀 Starting emulator..."
    "$SDK_PATH/emulator/emulator" "@$avd_name" -no-snapshot -no-audio &
    emulator_pid=$!
    
    # Wait for emulator to boot
    if ! wait_for_emulator; then
        echo "❌ Failed to start emulator"
        kill $emulator_pid 2>/dev/null || true
        exit 1
    fi
fi

# Run instrumented tests
echo "🧪 Running instrumented tests..."
echo "================================="

# Set environment variables for testing
export GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.workers.max=2"

# Run the tests
if ./gradlew connectedDebugAndroidTest --info --continue; then
    echo ""
    echo "✅ All instrumented tests completed!"
    echo "📊 Test reports available in: app/build/reports/androidTests/"
else
    echo ""
    echo "❌ Some instrumented tests failed"
    echo "📊 Test reports available in: app/build/reports/androidTests/"
    exit 1
fi

# Optional: Kill emulator if we started it
if [ -n "$emulator_pid" ]; then
    echo "🛑 Stopping emulator..."
    kill $emulator_pid 2>/dev/null || true
fi

echo "🎉 E2E test run complete!"