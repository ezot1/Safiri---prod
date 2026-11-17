# 🚌 Safiri — School Bus Fleet Tracking System

[![Android CI](https://github.com/ezot1/Safiri---prod/actions/workflows/ci.yml/badge.svg)](https://github.com/ezot1/Safiri---prod/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09.00-green.svg)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%7C%20Auth%20%7C%20AppCheck-orange.svg)](https://firebase.google.com/)

**Safiri** is a modern, real-time school bus fleet tracker and student safety management application built for schools and families in Nairobi, Kenya. It provides role-based portals for **Parents**, **Drivers**, and **School Administrators** with real-time GPS telemetry, boarding rosters, emergency SOS broadcasting, and intelligent route support powered by Gemini API.

---

## 🌟 Key Features

### 👨‍👩‍👧 1. Parent Portal
- **Live Bus Tracking**: Real-time GPS location and movement of the assigned school bus on interactive maps (Google Maps / OpenStreetMap).
- **Student Boarding Status**: Instant notifications and status updates (`NOT_BOARDED`, `BOARDED`, `DROPPED_OFF`, `ABSENT`).
- **Absence Reporting**: Inform drivers ahead of time when a student will miss the morning or afternoon trip.
- **Interportal Messaging**: Two-way direct dispatch communication with bus drivers and school transport admins.

### 🚍 2. Driver Portal
- **Interactive Boarding Roster**: Check in students with one tap as they board and disembark at designated stops.
- **Trip Telemetry**: Automated live GPS reporting, current speed, and occupancy count sync to Firestore.
- **Emergency SOS Broadcasting**: Trigger instant high-severity route alerts to notify admins and parents immediately.
- **Live Incident Reporting**: Log community traffic bottlenecks, weather hazards, or road closures.

### 🏫 3. School Administrator Portal
- **Fleet Command Center**: Overview of all active buses, assigned drivers, capacity, and current on-time status.
- **Driver & Student Management**: Provision authorized driver profiles and assign students to routes.
- **Alert Resolution**: Acknowledge, triage, and resolve incoming emergency alerts and dispatch notices.
- **AI Route & Operations Assistant**: Powered by the Gemini API for transit intelligence and operational insights.

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: Modern declarative UI using [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material 3.
- **Language**: Kotlin 2.2.10 with Coroutines and StateFlow.
- **Architecture**: MVVM (Model-View-ViewModel) + Single Source of Truth Repository pattern.
- **Backend & Cloud**:
  - **Firebase Authentication**: Credential Manager & Google Sign-In with Role-Based Access Control (RBAC).
  - **Cloud Firestore**: Real-time listener streams with battle-tested security rules (`firestore.rules`).
  - **Firebase App Check**: Hardened with reCAPTCHA Enterprise and debug tokens.
- **Local Storage**: [Room Database](https://developer.android.com/training/data-storage/room) for caching and offline state.
- **Networking**: [Retrofit 2](https://square.github.io/retrofit/) & [Moshi](https://github.com/square/moshi) with Moshi Kotlin Codegen.
- **Testing**:
  - JUnit 4 & AndroidX Test
  - [Robolectric](https://robolectric.org/) for local JVM Compose testing
  - [Roborazzi](https://github.com/takahirom/roborazzi) for visual screenshot testing
  - Node.js `@firebase/rules-unit-testing` for Firestore security bounds

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Ladybug / Meerkat or later recommended)
- **JDK 17** or **JDK 21**
- **Android SDK Platform 36** (minimum supported API 24: Android 7.0+)

### 1. Clone the Repository
```bash
git clone https://github.com/ezot1/Safiri---prod.git
cd Safiri---prod
```

### 2. Configure Environment Variables
Copy `.env.example` to `.env`:
```bash
cp .env.example .env
```
Populate optional configuration keys in `.env`:
```env
GOOGLE_MAPS_API_KEY=YOUR_ACTUAL_GOOGLE_MAPS_KEY
```
*(If unset, Safiri automatically falls back gracefully to OpenStreetMap).*

### 3. Firebase Configuration
For production deployment:
1. Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name: `com.aistudio.safiri.axrtqp`.
3. Download `google-services.json` and place it in the `app/` directory (see `app/google-services.json.example`).
4. Enable **Firebase Auth** (Email/Password & Google Sign-In) and **Cloud Firestore**.
5. Deploy Firestore security rules:
   ```bash
   firebase deploy --only firestore:rules
   ```

---

## 📦 Building for Production (Upload Ready)

Safiri includes production-ready Gradle configurations for building signed **Release APKs** and **Android App Bundles (.aab)** ready for the **Google Play Store**.

### Step 1: Generate an Upload Keystore
If you do not have an existing upload keystore, generate one using `keytool`:
```bash
keytool -genkey -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```
Place `my-upload-key.jks` in the project root directory (it is ignored by git).

### Step 2: Set Signing Environment Variables
#### On Linux / macOS:
```bash
export KEYSTORE_PATH="$(pwd)/my-upload-key.jks"
export STORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="upload"
export KEY_PASSWORD="your_key_password"
```

#### On Windows (PowerShell):
```powershell
$env:KEYSTORE_PATH = "$PWD\my-upload-key.jks"
$env:STORE_PASSWORD = "your_keystore_password"
$env:KEY_ALIAS = "upload"
$env:KEY_PASSWORD = "your_key_password"
```

### Step 3: Build the Android App Bundle (.aab)
```bash
./gradlew bundleRelease
```
The output file will be located at:
`app/build/outputs/bundle/release/app-release.aab`
👉 **This is the exact file to upload to the Google Play Console.**

### Step 4: Build a Standalone Release APK
```bash
./gradlew assembleRelease
```
The output file will be located at:
`app/build/outputs/apk/release/app-release.apk`

---

## 🤖 Automated CI/CD (GitHub Actions)

Safiri includes two pre-configured GitHub Actions workflows in `.github/workflows/`:

1. **`ci.yml`**: Automatically runs unit tests and linting on every push and pull request.
2. **`build-release.yml`**: Builds release APKs and App Bundles on tags (`v*`) or manual trigger (`workflow_dispatch`).

### Setting up GitHub Secrets for Automated Releases
In your GitHub repository, navigate to **Settings > Secrets and variables > Actions** and add:

| Secret Name | Description |
| :--- | :--- |
| `KEYSTORE_BASE64` | Base64-encoded string of your `my-upload-key.jks` (`base64 -w 0 my-upload-key.jks`) |
| `STORE_PASSWORD` | Password for your keystore |
| `KEY_ALIAS` | Key alias (default: `upload`) |
| `KEY_PASSWORD` | Password for your key |
| `GOOGLE_MAPS_API_KEY` | *(Optional)* Google Maps API Key |

---

## 🧪 Testing

### Running Android Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Running Firestore Security Rules Unit Tests
Safiri includes exhaustive security rule verification covering PII bounds, role escalation prevention, and student access isolation.
```bash
npm install
npm test
```

---

## 📋 Google Play Store Upload Checklist

Before uploading `app-release.aab` to Google Play Console:
- [x] **App ID**: `com.aistudio.safiri.axrtqp` configured in `app/build.gradle.kts`.
- [x] **SDK Compatibility**: `minSdk = 24`, `targetSdk = 36`, `compileSdk = 36`.
- [x] **Release Signing**: Configured with fallback for CI and environment variable support for production keys.
- [x] **ProGuard / R8**: Hardened rules in `app/proguard-rules.pro` for Firebase, Room, Moshi, and Coroutines.
- [x] **Gradle Wrapper**: Verified Gradle 8.13 distribution wrapper included.
- [ ] **Data Safety Section**: Declare Location (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) and Notifications (`POST_NOTIFICATIONS`) in Play Console.
- [ ] **Privacy Policy**: Provide a valid URL for your school/organization privacy policy.

---

## 📄 License

This project is licensed under the Apache 2.0 License. See the LICENSE file for details.
