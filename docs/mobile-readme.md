# CivicFix Android App

## Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- An Android emulator or physical device (API 26+)

## Setup

1. Open `android-app/` folder in Android Studio
2. Wait for Gradle sync to complete
3. Update `API_BASE_URL` in `app/build.gradle.kts` if needed:
   - Emulator: `http://10.0.2.2:8000` (default)
   - Physical device on same network: `http://<your-ip>:8000`

## Running

1. Ensure the backend is running (see root README)
2. Select a device/emulator in Android Studio
3. Click **Run** (▶) or press `Shift+F10`

## Features
- **Login/Signup** — Email + password authentication
- **Report Issue** — Camera/gallery photo, issue type dropdown, description, GPS
- **Report History** — View submitted reports with status badges
- **Location Detection** — Auto-captures GPS coordinates

## Permissions
The app requests:
- `CAMERA` — For taking photos
- `ACCESS_FINE_LOCATION` — For GPS coordinates
- `INTERNET` — For API communication
- `READ_MEDIA_IMAGES` — For gallery access

## Building APK
```
./gradlew assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Architecture
- **UI Layer**: Jetpack Compose with Material3
- **Navigation**: Jetpack Navigation Compose
- **Networking**: Retrofit + OkHttp
- **Image Loading**: Coil
- **Location**: Google Play Services Location
- **Camera**: CameraX
- **Offline**: WorkManager (scaffolded)
