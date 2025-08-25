# B-Scan Development Guide

## Prerequisites

- Android Studio Ladybug or newer
- Android SDK with API levels 29-35
- Java 17 or newer
- Device with NFC support for testing

## Setup

1. Clone the repository
2. Copy `local.properties.template` to `local.properties`
3. Update the `sdk.dir` path to your Android SDK location
4. Open the project in Android Studio
5. Sync the project and resolve any dependencies

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build  
./gradlew assembleRelease

# Install to connected device
./gradlew installDebug
```

## Testing

The app requires physical testing with actual Bambu Lab RFID tags, as the emulator cannot simulate NFC hardware.

### Test Requirements

- Android device with NFC enabled
- Bambu Lab filament reels with RFID tags
- Tags must be Mifare Classic 1K format

### Manual Testing Steps

1. Install debug APK on NFC-enabled device
2. Enable NFC in device settings
3. Launch B-Scan app
4. Hold device near Bambu Lab filament reel tag for 2-3 seconds
5. Verify filament information displays correctly

## Architecture

- **MVVM Pattern**: ViewModel manages UI state and business logic
- **Jetpack Compose**: Modern declarative UI framework
- **Material 3**: Latest Material Design components
- **Single Activity**: Simplified navigation architecture

## Key Files

- `MainActivity.kt`: Main entry point and NFC handling
- `MainViewModel.kt`: UI state management and tag processing
- `BambuTagDecoder.kt`: RFID tag data extraction logic
- `NfcManager.kt`: NFC hardware interface
- `ui/components/`: Reusable UI components
- `ui/screens/`: Main application screens

## Troubleshooting

### Build Issues
- Ensure Android SDK path is correct in local.properties
- Check that Java 17+ is configured in Android Studio

### NFC Issues  
- Verify device has NFC hardware support
- Ensure NFC is enabled in device settings
- Test with known working Bambu Lab tags
- Check logcat for NFC-related errors