# eSpeak NG Charlie

eSpeak NG Charlie is a fork of [eSpeak NG](https://github.com/espeak-ng/espeak-ng) for Android. It is a separate app from the official eSpeak for Android (`com.reecedunn.espeak`), so both can be installed at once.

- Package: `nz.charlie.espeakng`
- Version: 1.0.0
- License: GNU General Public License, version 3 (see `COPYING`)
- Source: https://github.com/BlindCharlieSmith2020/espeak-ng-charlie

This fork is based on eSpeak NG 1.53.0 (commit ee9fa7cc). No pull request has been opened against the upstream project.

## What Charlie adds

- A reachable voice folder at **Documents/eSpeak NG Voices**, with `variants`, `languages` and `dictionaries` subfolders.
- **Rescan voices**, from the app's main screen and from TTS settings, copies files from that folder into the engine and refreshes the list.
- Voice **variants are discovered at runtime** (see [espeak-ng issue #2376](https://github.com/espeak-ng/espeak-ng/issues/2376)). Every file in `voices/!v` appears in the variant list, including m8, klatt, robosoft and voices you design yourself.
- The existing **Import eSpeak dictionary** option is kept. It also looks in `Documents/eSpeak NG Voices/dictionaries`.
- A **Design voice** screen: adjust pitch, speed, formants, roughness, breathiness and flutter, preview, then save a named variant into the voice folder.
- An **English (New Zealand)** voice, with Kiwi vowels and te reo Māori place-name pronunciations (Whangārei, Taupō and many others).

## How to add and rescan voices

1. Install eSpeak NG Charlie and open the app once. It creates `Documents/eSpeak NG Voices`.
2. On Android 11 and later, choose **Allow all files access** so the app can read files you drop in that folder.
3. Copy variant files into `variants`, language files into `languages`, and compiled `*_dict` dictionaries into `dictionaries`.
4. Choose **Rescan voices**. You will hear a short count of what was imported.
5. New variants appear under **Voice variant** in eSpeak NG Charlie settings.

## How to use Design voice

Open eSpeak NG Charlie and choose **Design voice**, or open it from TTS settings. Set the sliders, type a name, choose **Preview** to hear a New Zealand English sample, then **Save voice**. The file is stored in `Documents/eSpeak NG Voices/variants` and is available immediately as a variant.

## How to pick English (New Zealand)

1. Open Android **Settings**, then **Accessibility**, then **Text-to-speech**.
2. Set the preferred engine to **eSpeak NG Charlie**.
3. Open the language or voice list and choose **English (New Zealand)**.
4. In TalkBack, you can also set this under TalkBack settings, then Text to speech.

## Building

Host tools and the Android SDK/NDK are required. From the `android` directory, with `ANDROID_HOME` set:

```
./gradlew assembleRelease test
```

The unsigned release APK is written to `android/build/outputs/apk/release/`. Sign it with `apksigner` before installing. The engine data (including English (New Zealand)) is compiled as part of the Gradle build.
