# Spelling Bee Practice

A spelling bee practice app for desktop (Linux, Windows, macOS) and Amazon Fire HD
tablets, written in Java with libGDX.

The app **says** a word (it is never shown), the player types it on the big on-screen
keyboard (or a real keyboard), and gets a chime for a correct answer. A wrong answer
shows the correct spelling with the differing letters highlighted, plus the meaning
and an example sentence, and reads them aloud.

- **Hint buttons** (audio only): Hear word, Definition, Sentence, Say it another way
- **Sessions** of 10, 20 or all words, shuffled; no word repeats until the whole list
  has been used, even across sessions
- **Review missed words**: quizzes only words that were missed (a word leaves the list
  once it is spelled correctly)
- **My progress**: words tried, correct, missed, and the missed-word list
- **Settings**: speech speed, voice, sound effects on/off, and a Voice check screen

## Install (no building needed)

Download the files from the
[latest release](https://github.com/djr4488/spelling-bee/releases/latest):

- **Fire HD tablet (or other Android tablet):** download `SpellingBee-<version>.apk`
  and follow [Installing on a Fire HD tablet](#installing-on-a-fire-hd-tablet-sideloading)
  below. The easiest way is to open the release page in the tablet's Silk browser
  and tap the APK.
- **Windows, macOS or Linux:** install [Java 17 or newer](https://adoptium.net/),
  download `SpellingBee-<version>.jar`, and double-click it (or run
  `java -jar SpellingBee-<version>.jar`). On Linux, also see
  [Better-sounding voice on Linux](#better-sounding-voice-on-linux-recommended).

To update, install the new version over the old one. Progress is kept.

The rest of this page is for building the app yourself.

## Requirements

| What | Needed for | Notes |
|---|---|---|
| JDK 17+ | everything | Builds target Java 17. JDK 21 works. |
| Android SDK (platform 36, build-tools) | the APK | Install via Android Studio or `sdkmanager`. |
| `local.properties` | the APK | One line: `sdk.dir=/home/you/Android/Sdk` (not committed). `ANDROID_HOME` also works. |
| A speech engine | desktop speech | Linux: `espeak-ng` (preferred) or `spd-say` (speech-dispatcher). macOS: built-in `say`. Windows: built-in. |

If the Android SDK is missing, Gradle prints how to fix it and skips the `android`
module, so the desktop build still works.

### Better-sounding voice on Linux (recommended)

espeak sounds robotic. [Piper](https://github.com/rhasspy/piper) is a free, offline
neural voice that sounds much more natural, and the app uses it automatically when it
is installed:

```sh
uv tool install piper-tts          # or: pipx install piper-tts
mkdir -p ~/.local/share/piper-voices && cd ~/.local/share/piper-voices
base=https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/en/en_US/lessac/medium
curl -LO $base/en_US-lessac-medium.onnx -LO $base/en_US-lessac-medium.onnx.json
```

Restart the app; **Settings** should then say `Speech: Piper (...)`. Other voices
from the [voice list](https://huggingface.co/rhasspy/piper-voices) work too: put each
`.onnx` file and its `.onnx.json` in the same folder and pick one under **Settings → Voice**.
Set `SPELLINGBEE_PIPER_VOICES` to use a different folder.

If no speech engine is found, the app still runs and shows a message on the menu. On
Linux, install one with:

```sh
sudo apt install espeak-ng
```

## Build and run

```sh
./gradlew build                 # compile everything, run unit tests and data validation
./gradlew lwjgl3:run            # run on the desktop
./gradlew lwjgl3:jar            # runnable jar: lwjgl3/build/libs/SpellingBee.jar
./gradlew android:assembleDebug # APK: android/build/outputs/apk/debug/android-debug.apk
./gradlew dist                  # release APK + jar in build/dist/ (needs the release key)
```

Run the jar with `java -jar lwjgl3/build/libs/SpellingBee.jar`, or double-click it if
your desktop opens `.jar` files with Java. It contains everything it needs.

Check the speech engine without opening a window:

```sh
java -jar lwjgl3/build/libs/SpellingBee.jar --speech-test
```

Desktop keys: type letters, space and hyphen; **Backspace** deletes; **Enter** checks
the answer, or continues after a miss; **Esc** goes back to the menu.

## Installing on a Fire HD tablet (sideloading)

Fire tablets run Fire OS, a version of Android, so a normal APK works. Google Play
isn't needed.

**Option A: copy the APK over**

1. On the tablet: **Settings → Security & Privacy → Apps from Unknown Sources**, and
   allow the app you will open the APK with (for example *Files* or *Silk Browser*).
   On older Fire OS versions this is a single "Apps from Unknown Sources" switch.
2. Get the APK onto the tablet: download it from the release page in the Silk
   browser, or copy it over USB (the tablet appears as a drive; put the file in
   `Download`), by email to yourself, or through a cloud drive. If you built it
   yourself, it is `android/build/outputs/apk/debug/android-debug.apk`.
3. On the tablet, open **Files → Downloads**, tap the APK, then tap **Install**.
4. You can turn "Unknown Sources" back off afterwards.

**Option B: adb (USB)**

1. On the tablet: **Settings → Device Options → About Fire Tablet**, tap **Serial
   Number** 7 times to unlock **Developer Options**, then turn on **ADB / USB
   debugging** in Developer Options.
2. Connect USB, accept the prompt on the tablet, then:

```sh
~/Android/Sdk/platform-tools/adb devices
~/Android/Sdk/platform-tools/adb install -r android/build/outputs/apk/debug/android-debug.apk
```

**Speech on the tablet:** the app uses the tablet's text-to-speech engine. If it is
silent, check **Settings → Accessibility → Text-to-Speech** (the setting's name varies
by Fire OS version): make sure an engine is selected, its English voice is downloaded,
and the volume is up. **Settings → Voice check** in the app shows which engine is in
use.

## Making a release

Releases are built on the maintainer's computer and uploaded with the
[GitHub CLI](https://cli.github.com/) (`gh`).

**One-time setup: the release key.** Android only installs an update over an old
version if both were signed with the same key, so every release must use this key.
Keep a backup of it somewhere safe (outside the repo). If it is lost, people have to
uninstall the app (losing their progress) to install newer versions.

```sh
mkdir -p ~/keys
keytool -genkeypair -v -keystore ~/keys/spelling-bee-release.jks \
        -alias spelling-bee -keyalg RSA -keysize 4096 -validity 36500
```

Then create `keystore.properties` in the project root (it is git-ignored):

```properties
storeFile=/home/you/keys/spelling-bee-release.jks
storePassword=...
keyAlias=spelling-bee
keyPassword=...
```

The same values can come from the environment variables `SPELLINGBEE_KEYSTORE`,
`SPELLINGBEE_KEYSTORE_PASSWORD`, `SPELLINGBEE_KEY_ALIAS` and
`SPELLINGBEE_KEY_PASSWORD`.

**Each release:**

1. In `gradle.properties`, raise `appVersionName` (e.g. `1.1`) and add 1 to
   `appVersionCode`. Commit.
2. `./gradlew build dist` runs the tests and writes `build/dist/SpellingBee-<version>.apk`
   and `.jar`.
3. Tag and publish:

```sh
git tag v1.1 && git push origin main v1.1
gh release create v1.1 build/dist/* --title "Spelling Bee 1.1" --notes "What changed..."
```

A tablet with a debug build (`assembleDebug`) installed must uninstall it once
before installing a release APK, because the keys differ.

## Word list

- `assets/words.txt` is the raw list: words separated by commas.
- `assets/words.json` is what the app loads. Each word has a definition, a sentence,
  a pronunciation, an optional `speakAs` respelling for the speech engine,
  alternate pronunciations, and `homophone` / `needsReview` flags.
- `tools/content/*.txt` holds the written definitions and sentences, one word per
  line (`word | partOfSpeech | definition | sentence | pronunciation | speakAs | alts | flags | note`).

**Adding words:** add them to `assets/words.txt`, then run

```sh
python3 tools/enrich.py
```

It lists any words that have no content yet. Add a line for each one to a file in
`tools/content/` and run it again. It checks every entry (the sentence contains the
exact word, the definition doesn't give the word away, required fields are present)
and writes `assets/words.json`. The same checks run as unit tests in `./gradlew build`.

`tools/make_sounds.py` regenerates the two feedback sounds in `assets/sounds/`.

## Where progress is saved

libGDX Preferences: on Linux and macOS, `~/.prefs/spelling-bee-progress` and
`~/.prefs/spelling-bee-settings`; on Windows, under `%USERPROFILE%\.prefs\`; on
Android, in the app's private storage. Uninstalling the app on the tablet clears it.
**My progress → Reset progress** (tap twice) also clears it.

## Project layout

```
core/     all game logic and UI (Scene2D), shared by every platform
  data/     WordEntry, WordBank (loads words.json)
  quiz/     AnswerChecker, LetterDiff, WordDeck, QuizSession, Progress
  speech/   SpeechService interface, Pronouncer (what to say), SilentSpeechService
  store/    Settings and the key-value storage behind it
  screens/  Menu, Quiz, Summary, Progress, Settings, Voice check
  ui/       skin/fonts (UiKit), on-screen keyboard, sounds
lwjgl3/   desktop launcher + DesktopSpeechService (espeak-ng / spd-say / say / PowerShell)
android/  Android launcher + AndroidSpeechService (android.speech.tts)
assets/   words, fonts, sounds (shared by desktop and Android)
tools/    enrich.py (word data), make_sounds.py
```

Developer tool: `./gradlew lwjgl3:run --args="--tour /tmp/shots 1920 1200"` visits
every screen and saves screenshots at the given window size, using separate
throwaway preferences.

## License

Copyright 2026 djr4488

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this project except in compliance with the License. You may obtain a copy of the
License in [LICENSE](LICENSE) or at <https://www.apache.org/licenses/LICENSE-2.0>.

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.

## Credits

- **Fonts:** [DejaVu Sans](https://dejavu-fonts.github.io/) (regular and bold).
  DejaVu fonts are based on the Bitstream Vera fonts, Copyright (c) 2003 by
  Bitstream, Inc. All Rights Reserved. Bitstream Vera is a trademark of
  Bitstream, Inc. DejaVu changes are in the public domain. The fonts are used
  under the Bitstream Vera Fonts license; its full text is in
  [assets/fonts/LICENSE-DejaVu.txt](assets/fonts/LICENSE-DejaVu.txt) and ships
  with the app. The DejaVu fonts are not covered by the Apache License.
- Built with [libGDX](https://libgdx.com/) (Apache License 2.0).
