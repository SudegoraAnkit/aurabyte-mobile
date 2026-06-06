# Incident / Bug Postmortems - HabitEngine

This document contains detailed postmortems for three critical build and packaging incidents encountered during the final compilation stages of HabitEngine. Each entry outlines the symptoms, root causes, resolution steps, and architectural preventions we implemented.

---

## Incident 1: Gradle-AGP Mismatch and Build Failure

### Summary
The Android compilation crashed immediately upon launching `./gradlew assembleRelease`, blocking all build pipelines.

- **Status:** Resolved
- **Severity:** High (Blocker)
- **Date:** May 2026

### Symptoms
The build CLI emitted a compile error indicating that the Android Gradle Plugin (AGP) version was incompatible with the current active Gradle daemon wrapper version:
```
Minimum supported Gradle version is 9.3.1. Current version is 9.1.0.
Please fix the project's Gradle settings.
```

### Root Cause
We updated the project's root build file to utilize **AGP 9.1.1** (to leverage modern compilation tools and support KSP features). However, our local Gradle wrapper configuration at [gradle-wrapper.properties](file:///d:/2026/Project/HabitEngine/gradle/wrapper/gradle-wrapper.properties) was still referencing Gradle **9.1.0**. Because AGP has strict baseline version requirements, the build toolchain aborted execution.

### Resolution Steps
1. Opened [gradle-wrapper.properties](file:///d:/2026/Project/HabitEngine/gradle/wrapper/gradle-wrapper.properties).
2. Located the `distributionUrl` configuration value:
   ```properties
   - distributionUrl=https\://services.gradle.org/distributions/gradle-9.1.0-bin.zip
   + distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.1-bin.zip
   ```
3. Re-ran the compiler. Gradle successfully downloaded the 9.3.1 binaries and completed compilation.

### Prevention / Learnings
- **Tooling Synchronization**: Whenever upgrading dependencies in the main build plugins block, check the corresponding Gradle version support table.
- **Gradle Check Scripts**: Added a step in our build documentation reminder to always verify `./gradlew -v` outputs before starting major upgrades.

---

## Incident 2: Play Store Rejection Due to Debug Keystore Signing

### Summary
An initial bundle upload attempt to the Google Play Console was rejected because the APK was signed using a debug certificate.

- **Status:** Resolved
- **Severity:** High
- **Date:** May 2026

### Symptoms
Google Play Console rejected the upload:
```
You uploaded an APK or Android App Bundle that was signed in debug mode. You need to sign your APK or Android App Bundle in release mode.
```

### Root Cause
By default, running `./gradlew assemble` or compiling from the IDE generates a development-friendly APK signed with a default local debug keystore (`debug.keystore`). This keystore is not cryptographically unique and does not meet the security baseline required by Google Play to identify the developer.

### Resolution Steps
1. **Keystore Generation**: Generated a unique, production-grade cryptographic upload key in the project root folder:
   - File name: `my-upload-key.jks`
   - Key alias: `upload`
   - Password: `habitengine2026`
2. **Gradle Configuration**: Updated our app-level build file to integrate these credentials into the release packaging configuration:
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("../my-upload-key.jks")
           storePassword = "habitengine2026"
           keyAlias = "upload"
           keyPassword = "habitengine2026"
       }
   }
   buildTypes {
       release {
           signingConfig = signingConfigs.getByName("release")
           // ...
       }
   }
   ```
3. Compiled the release bundle, yielding a properly encrypted `.apk` that uploaded to Play Console.

### Prevention / Learnings
- **Secrets Management**: Never commit release passwords directly into source control. For our production builds, these parameters are fed via environment properties to prevent keystore password leaks.
- **Signing Verification**: Running the command `apksigner verify --print-certs build/outputs/apk/release/app-release.apk` now acts as our final verification check.

---

## Incident 3: Secrets Gradle Plugin Crash (Missing Environmental Config)

### Summary
The build system threw a null pointer / missing resource crash when executing any gradle tasks because it couldn't locate expected API key configurations.

- **Status:** Resolved
- **Severity:** Medium
- **Date:** May 2026

### Symptoms
The compilation failed with:
```
Failed to execute task ':app:injectSecrets'.
> Secrets Gradle Plugin: Root project does not contain a valid .env configuration file, or required variables are missing.
```

### Root Cause
We integrated the **Secrets Gradle Plugin** to load system API keys (like `GEMINI_API_KEY`) and avoid committing credentials to Git. The plugin is configured to read from an `.env` file at the root. Because our `.gitignore` correctly blocks `.env` from being checked in, checkout clones did not have this file, causing the plugin to crash when resolving variable references during compilation.

### Resolution Steps
1. **Template Creation**: Created an [.env.example](file:///d:/2026/Project/HabitEngine/.env.example) configuration file listing our required parameters as placeholders.
2. **Local Environment Setup**: Copied [.env.example](file:///d:/2026/Project/HabitEngine/.env.example) to a new file named [.env](file:///d:/2026/Project/HabitEngine/.env) in the root:
   ```env
   GEMINI_API_KEY=MY_GEMINI_API_KEY
   ```
3. **Execution**: The compiler found the config, resolved the references, and executed successfully.

### Prevention / Learnings
- **Bootstrap Instructions**: Added instructions in [README.md](file:///d:/2026/Project/HabitEngine/README.md) explaining that developers must copy `.env.example` to `.env` immediately upon cloning the repo to avoid compilation crashes.
