# Build Melo from source

These instructions are for contributors and people who want to build Melo themselves. Most users should install the signed APK from [GitHub Releases](https://github.com/aarush-io/melo/releases/latest).

## Requirements

- JDK 21
- Android SDK Platform 37 and Build Tools 36.0.0 or newer
- The unmodified Spotify 9.2.0.26 ARM64 APKM (build 148373686) for the complete Spotify engine path

The Spotify APKM and generated engine binaries are not stored in this repository. Provide a local copy that you are entitled to use. Gradle checks the pinned APK and native library hashes before preparing the runtime.

## Windows

Clone the repository, configure the Android SDK using `ANDROID_HOME` or an ignored `local.properties` file, and place the APKM at:

    native-runtime/artifacts/spotify-9.2.0.26.apkm

Build and package the signed release:

~~~powershell
.\gradlew.bat :app:assembleRelease -PmeloUpdateRepository=aarush-io/melo
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/package-release.ps1
~~~

The APK, SHA-256 checksum, and signing-certificate report are written to the ignored `dist` folder. Create the Melo signing key once with `scripts/create-signing-key.ps1` and back up the ignored `.signing` directory securely. Never commit the signing key or its properties file.

## macOS and Linux

Use `./gradlew` with the same Gradle property and packaging script.

## Build without the Spotify runtime archive

For development, pass `-PnativeRuntime=false`. This build cannot play encrypted Spotify audio.

## Playback implementation notes

Melo uses native Android protocols for its Spotify integration. The protocol notes distinguish static checks, tests with doubles, and validation against live services. A moving playhead or a populated buffer is not proof of audible playback; full Spotify playback is only confirmed when decoded audio is checked beyond the clear lead-in, after seeks, and across track changes.

Before changing playback, read the [native runtime guide](../native-runtime/README.md), [audio delivery notes](../protocol/07-audio-delivery.md), and [native engine notes](../protocol/08-native-engine.md).
