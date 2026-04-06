# RailBridge Demo

Android Java + WebView bridge sample project for testing a native `JavascriptInterface`, mock SDK failures, retry handling, and local error logging.

## Environment

- Android Gradle Plugin: `8.2.0`
- Gradle Wrapper: `8.2`
- Min SDK: `26`
- Compile / Target SDK: `34`
- Language: Java

## Open In Android Studio

1. Open this folder in Android Studio.
2. Use the Gradle wrapper when prompted.
3. Set the Gradle JDK to a JDK `17+`.
4. Sync the project.
5. Run the `app` configuration on an emulator or device.

Notes:

- `local.properties` is intentionally ignored by git. Android Studio can create it automatically if needed.
- This repo already enables `android.overridePathCheck=true` because the current Windows path contains non-ASCII characters.
- On this machine, Android Studio's bundled JBR 21 works for Gradle, but JDK 17 remains the safest baseline to use across teammates.

## Required Android SDK Packages

Install these from SDK Manager before building:

- Android SDK Platform `34`
- Android SDK Build-Tools `34.0.0` or newer
- Android SDK Platform-Tools

## Project Structure

```text
app/
  src/main/
    java/com/demo/railbridge/
      MainActivity.java
      WebViewActivity.java
      bridge/
      logging/
      retry/
      sdk/
    assets/webview/index.html
    res/
```

## Manual Test Flow

1. Launch the app.
2. Tap `Start demo`.
3. In the WebView screen:
   - Tap `Request charge`
   - Tap `Get balance`
   - Tap `SDK status`
   - Tap `Report JS error`
4. Confirm that the result log updates for each action.

## Troubleshooting

If Android Studio build or sync fails, check these in order:

1. Gradle wrapper files exist: `gradlew`, `gradlew.bat`, `gradle/wrapper/*`
2. Gradle JDK is set to `17+`
3. `local.properties` points to a valid Android SDK
4. Android SDK Platform `34` is installed
5. Sync error category:
   - Missing SDK / `sdk.dir`
   - Dependency resolution
   - Manifest merge
   - Resource not found
   - WebView asset loading at runtime

## CLI Verification

From PowerShell:

```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

If your Android SDK path is different, update `local.properties` or let Android Studio regenerate it.
