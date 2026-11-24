# Project: Spelling Bee Practice App

Build a spelling bee practice app in **Java with libGDX**, built with **Gradle**, that runs on **desktop (PC, Linux)** and **FireHD tablet**. Work in milestones (below). After each milestone, make sure `./gradlew build` passes and summarize what you did before moving on.

## Before you start

1. Look in the project folder for the dictionary file (a list of practice words, no definitions or sentences). If you can't find it, ask me where it is and what format it's in.
2. Ask me (one short batch of questions) only about things you can't safely assume:
    - Desktop OS I'm developing on Linux. This decides the desktop speech approach.
    - Tablet is FireHD? (Assume yes.)
    - Whether I want a `words` file format change, or to keep mine as-is.
3. Then propose a short plan and start building. Don't wait for approval on routine decisions.

## How the app behaves

**Quiz loop**
1. App picks the next word and **speaks it aloud** using text-to-speech. The word is **never shown on screen** while the player is guessing.
2. Player can tap buttons (audio only, nothing revealing is displayed):
    - **Repeat word**
    - **Definition** (speaks the definition)
    - **Use in a sentence** (speaks the sentence)
    - **Alternate pronunciation** (speaks the word using its alternate pronunciation, if one exists)
3. Player types the spelling and submits.
4. **If correct:** brief positive feedback (sound + visual), move to the next word. Do not display the word.
5. **If wrong:** *now* reveal the correct spelling, the definition, and the example sentence on screen, and speak them. Show what the player typed next to the correct spelling, with differing letters highlighted. Player taps Continue to move on.

**Other requirements**
- Input: a custom **on-screen letter keyboard** (Scene2D) that works identically on desktop and tablet, plus physical keyboard support on desktop (type, Backspace, Enter).
- Track progress locally: words attempted, correct/incorrect counts, and a list of missed words. Persist using libGDX `Preferences` or a small JSON file.
- A **Review missed words** mode that quizzes only previously missed words.
- Words should be shuffled; avoid repeating a word until the list is exhausted (or a session length, e.g. 10/20/all, is chosen).
- Simple, clean, large-touch-target UI that is readable on a tablet and scales on desktop (use a Viewport such as `FitViewport`).
- Settings: speech rate, voice (if platform supports it), sound on/off.

## Word data

The dictionary I provide has **words only**, comma separated. Definitions, example sentences, and alternate pronunciations must be generated.

- **Do not call an AI API at runtime.** Generate the content once, ahead of time, and ship it as a data file (e.g. `assets/words.json`).
- You (Claude Code) write the definitions, sentences, and pronunciation notes directly into the enriched data file, in batches if the list is large. Keep a script in `tools/` that converts my raw dictionary into the enriched format and merges in your generated content, so it's re-runnable if I add words.
- Enriched schema per word:
  ```json
  {
    "word": "necessary",
    "partOfSpeech": "adjective",
    "definition": "Needed in order to achieve a result; required.",
    "sentence": "It is necessary to bring a ticket to enter the show.",
    "pronunciation": "NESS-uh-sair-ee",
    "speakAs": null,
    "altPronunciations": ["NESS-uh-sair-ee"]
  }
  ```
- Rules for generated content:
    - Definition: one clear sentence, age-appropriate, and **must not contain the word itself** (or an obvious form of it).
    - Sentence: natural, shows meaning from context, and **must contain the exact word** so I can verify it.
    - For homophones (e.g., "their/there/they're") make the definition and sentence unambiguous; flag them in a `"homophone": true` field.
    - `speakAs`: an optional respelling used as TTS input when the engine mispronounces the default word. `altPronunciations`: genuinely accepted alternates (regional/variant), as respellings the TTS can speak.
    - If you're unsure about a word, flag it with `"needsReview": true` rather than guessing, and list those in your summary.
- Write a validation test or script that checks every entry: sentence contains the word, definition doesn't contain the word, required fields present.

## Voice / text-to-speech

libGDX has no built-in TTS, so abstract it:

- In `core`, define an interface, e.g. `SpeechService { void speak(String text); void speak(String text, float rate); void stop(); boolean isSpeaking(); void dispose(); }`, injected into the game on startup.
- **Android:** implement with `android.speech.tts.TextToSpeech` (handle async init, language set, utterance progress).
- **Desktop:** implement by invoking the OS speech engine, based on my OS:
    - Windows: PowerShell + `System.Speech.Synthesis.SpeechSynthesizer`
    - macOS: `say`
    - Linux: `espeak-ng` (fall back to `spd-say`)
      Run speech on a background thread so the render loop never blocks, and cancel the current utterance when a new one starts. If no engine is found, fail gracefully (show a message, keep the app usable).
- Speak words slowly and clearly (slightly reduced rate by default). Speak the word on its own utterance, with a short pause between the word, the definition, and the sentence when reading them in sequence.

## Tech requirements

- **Java 17**, **Gradle** wrapper committed. Use the standard libGDX multi-module layout: `core`, `lwjgl3` (desktop), `android`, plus `assets`. Scaffold it the way gdx-liftoff would (use the current stable libGDX version; check rather than assuming).
- All game logic and UI live in `core` and are platform-independent. Platform modules only contain launchers and the `SpeechService` implementation.
- Use Scene2D (`Stage`, `Skin`, `Table`) for UI. Include a simple skin; large readable fonts via FreeType or a bitmap font that stays crisp at tablet sizes.
- Unit tests (JUnit) for core logic: word selection/shuffling, answer checking (case-insensitive, trim whitespace), the letter-diff highlight, progress persistence, and data validation.
- Android: set a sensible `minSdk` (24+), landscape-friendly, keep screen on during a session.

## Build & run targets

Document these in `README.md` and make sure each actually works:

- Run desktop: `./gradlew lwjgl3:run`
- Desktop distributable jar: `./gradlew lwjgl3:jar` (a runnable fat jar I can double-click or run with `java -jar`)
- Android debug APK: `./gradlew android:assembleDebug`, with instructions for sideloading onto my friend's tablet (enable "install unknown apps", transfer the APK, or `adb install`).
- Note any SDK/JDK setup I need (Android SDK path, `local.properties`) and detect/handle it clearly if it's missing.

## Milestones

1. **Scaffold:** Gradle multi-module project builds and shows a blank screen on desktop; Android module compiles.
2. **Data:** Load my dictionary; build the enrichment tool; generate `words.json` with definitions, sentences, and pronunciations; validation passes. Show me a sample of ~10 entries for a quick sanity check.
3. **Speech:** `SpeechService` on desktop and Android; a test screen that speaks a word.
4. **Core quiz loop:** Speak word, on-screen keyboard, correct/wrong flow with the reveal screen.
5. **Polish:** Progress tracking, review-missed mode, settings, session length, sounds, UI cleanup.
6. **Packaging:** Desktop jar and Android APK built and documented in the README.

## Working style

- Keep code simple and readable; small classes with clear names. No over-engineering.
- Commit after each milestone with a clear message.
- If something blocks you (missing SDK, no TTS engine on this machine), tell me exactly what's needed instead of working around it silently.
- At the end, give me a short summary: what works, what I should test on the tablet, and any words flagged `needsReview`.