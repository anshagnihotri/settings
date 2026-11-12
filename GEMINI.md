# Android Settings App

## Project Overview
This directory contains the source code for the Android Settings application (`com.android.settings`), a core system application in the Android Open Source Project (AOSP). It provides the user interface for configuring device settings, network connections, apps, security, and more.

### Architecture & Technologies
- **Language:** Java and Kotlin (mixed codebase).
- **Build System:** Android Soong (`Android.bp`).
- **Key Dependencies:** AndroidX libraries (Fragment, Navigation, Lifecycle, Room, Compose), Wi-Fi Tracker Lib, SettingsLib, various system UI and framework internal libraries.
- **Flags:** Uses `aconfig` for feature flagging.

## Directory Structure
- `Android.bp`: The main build configuration file for the Soong build system, defining modules like `Settings-core` and the `Settings` APK.
- `AndroidManifest.xml`: Declares the app's components, permissions, and core attributes (runs as `android.uid.system`).
- `src/`: Contains the Java and Kotlin source code.
- `res/`: Contains Android resources (layouts, strings, drawables, XML configs). Includes many localized values directories.
- `tests/`: Contains test configurations and source files (unit tests, instrumentation tests).
- `aconfig/`: Contains feature flag definitions.

## Building and Running
*Note: This project is part of the larger Android OS and is typically built within a full AOSP source tree.*

- **Build:** Typically built using `m Settings` from the AOSP root.
- **Run/Install:** Installed automatically on system builds. For local development on an emulator or device, `adb install -r -d out/target/product/<device>/system_ext/priv-app/Settings/Settings.apk` might be used after building.
- **Testing:** Tests are typically run using `atest SettingsUnitTests` or similar `atest` commands depending on the modules defined in the `tests/` directory.

## Development Conventions
- The project follows standard Android platform development conventions.
- Uses `lint-baseline.xml` and `color-check-baseline.xml` for linting and formatting enforcement.
- Feature flags are widely used (`aconfig`) for staged rollouts of new settings or UI changes.
- Ensure any added dependencies or string resources follow AOSP guidelines for localization and accessibility.
