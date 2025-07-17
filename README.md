# 📱 BluetoothMessenger

An Android application for Bluetooth messaging between devices. 

## ✨ Features

- 🔍 **Device Discovery**: Real-time Bluetooth device scanning
- 🔄 **Dual Connection Mode**: Simultaneous server/client connection attempts
- 💬 **Real-time Messaging**: Instant message exchange between connected devices
- 💾 **Chat History**: Persistent message storage with SQLite database

## 🛠️ Technology Stack

- **Language**: Kotlin
- **Architecture**: MVVM + Repository Pattern
- **UI Framework**: Jetpack Compose + XML Layouts
- **Database**: Room
- **Concurrency**: Kotlin Coroutines
- **Bluetooth**: BluetoothAdapter

## 📋 Prerequisites

### System Requirements
- **Android Studio**
- **Android SDK**: API Level 29+ (Android 10+)
- **Kotlin**
- **Gradle**

### Device Requirements
- **Android Version**: 10.0 (API 29) or higher
- **Bluetooth**: Classic Bluetooth support required
- **Permissions**: Location and Bluetooth permissions

## 🚀 Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/Chinthana7858/BluetoothMessenger
cd BluetoothMessenger
```

### 2. Open in Android Studio

### 3. Configure Project

#### Check Build Configuration
Ensure your `app/build.gradle` has the correct settings:

```gradle
android {
    compileSdk 34
    
    defaultConfig {
        applicationId "com.example.bluetoothmessenger"
        minSdk 29
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }
}
```

#### Sync Dependencies
```bash
# In Android Studio terminal or click "Sync Now" when prompted
./gradlew build
```

### 4. Set Up Physical Devices

#### Device Setup:
1. **Enable Developer Options**:
   - Go to `Settings > About Phone`
   - Tap `Build Number` 7 times
   - Return to `Settings > Developer Options`

2. **Enable USB Debugging**:
   - In Developer Options, enable `USB Debugging`

3. **Connect Devices**:
   - Connect 2+ Android devices via USB
   - Accept USB debugging prompts

## 🏃‍♂️ Running the Application

### Method 1: Android Studio

1. **Select Target Device**:
   - Click device dropdown in toolbar
   - Choose your connected physical device

2. **Build and Run**:
   - Click the green ▶️ **Run** button
   - Or press `Shift + F10`

3. **Install on Multiple Devices**:
   - Repeat the process for each device you want to test

### Method 2: Command Line

```bash
# Build the APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or install specific APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Method 3: Generate APK for Manual Installation

```bash
# Generate debug APK
./gradlew assembleDebug

# APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

Transfer the APK to your devices and install manually.



## 📁 Project Structure

```
BluetoothMessenger/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/bluetoothmessenger/
│   │   │   ├── ui/                     # UI Components
│   │   │   ├── repository/             # Data Layer
│   │   │   ├── service/                # Bluetooth Services
│   │   │   ├── database/               # Room Database
│   │   │   └── MainActivity.kt         # Main Activity
│   │   ├── res/
│   │   │   ├── layout/                 # XML Layouts
│   │   │   ├── drawable/               # Icons & Graphics
│   │   │   └── values/                 # Strings, Colors, Themes
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── gradle/
├── README.md
└── build.gradle
```


```

