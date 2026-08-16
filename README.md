# Zoya — Voice Assistant (Android / Kotlin / Jetpack Compose)

A background-running, wake-word-activated voice assistant with a flirty/witty persona, built on
Gemini's Live API for real-time audio-to-audio conversation and native Android function calling
for device control.

## Project layout (Clean Architecture / MVVM)

```
ui/            Compose screens + the animated orb (presentation only, no business logic)
viewmodel/     AssistantViewModel — binds to the service, republishes state as Flows
service/       BackgroundAudioService (foreground service) + WakeWordDetector
ai/            LiveSessionManager (Gemini Live WebSocket), GeminiPersona, ZoyaState
tools/         ToolExecutionEngine + DeviceTools (the actual Intent-based device actions)
util/          Audio (PCM record/playback), permissions
```

Data flow: `MainActivity` binds to `BackgroundAudioService` → the service owns one
`LiveSessionManager` (the live connection + mic/speaker loop) → `LiveSessionManager` calls into
`ToolExecutionEngine` whenever Gemini emits a function call → `AssistantViewModel` exposes
`ZoyaState` + amplitude as `StateFlow`s that `ZoyaOrb` renders. The Compose layer never talks to
the network or the mic directly.

## Setup

1. Open in Android Studio (Koala+ recommended), let Gradle sync, run on a device (not the
   emulator — you need a real mic for wake-word + Live audio).
2. First launch shows permissions onboarding (mic, contacts, calls, SMS, camera, notifications).
3. Tap the gear icon on the Home screen → paste your Gemini API key (get one free at
   https://aistudio.google.com/app/apikey) → Save. The key lives in on-device DataStore, entered
   entirely from the app — nothing to configure in Gradle or `local.properties`.
4. Pick a voice on the same Settings screen if you want something other than the default.

## Screens

- **Home** — dashboard with greeting, quick actions, and recent chat history (matches the
  provided reference UI: pastel gradient background, glass white cards, pink/violet accents).
- **Voice Pulse** — the full-screen orb experience: tap the mic or say "Zoya" to start a live
  audio conversation; the orb's Canvas animation reflects idle/listening/thinking/speaking states.
- **Smart Chat** — text view of the same conversation (Gemini's input/output audio transcription
  is enabled so both sides of a voice call show up here too), with a text field to type instead
  of speak.
- **Settings** — Gemini API key entry + voice picker.

## Things I want to be upfront about

- **Live API transport**: Google's official Live API client SDK, `@google/genai`, is JS/Node
  only — there's no first-party Kotlin/Android SDK with Live support yet. `LiveSessionManager`
  therefore speaks the underlying `BidiGenerateContent` WebSocket protocol directly over OkHttp,
  which is the standard way native (Android/iOS/desktop) Live API clients are built today. If
  Google ships an official Kotlin SDK, only this one file needs to change.
- **Model name**: I wired up `gemini-3.1-flash-live-preview` exactly as requested. Live-model
  names and availability change fairly often — if your API key's project 400s on connect, check
  https://ai.google.dev/gemini-api/docs/live for the current live-capable model name and swap the
  `MODEL` constant in `LiveSessionManager.kt`.
- **Wake word**: true always-on hotword detection (the kind that runs for days without meaningful
  battery drain) needs a dedicated trained keyword model — e.g. Picovoice Porcupine with a custom
  "Zoya.ppn" file. `WakeWordDetector` as written uses Android's on-device `SpeechRecognizer` in a
  continuously-restarting loop and checks partial results for "zoya," so it's genuinely local
  where the device supports on-device recognition, but it's heavier on battery and a bit less
  reliable in noise than a real DSP wake-word engine. It has the exact same
  `start()/stop()/onWake()` surface as a Porcupine wrapper would, so swapping it in later is a
  contained change, not a rewrite.
- **Background execution**: Android 12+ restricts starting foreground services from the
  background in various situations, and OEM battery managers (especially on Chinese Android
  skins) can still kill long-lived mic services despite `foregroundServiceType="microphone"`.
  Users on aggressive OEMs will likely need to manually whitelist the app in battery settings —
  worth a one-time in-app prompt pointing them there.
- **WhatsApp/Gmail intents**: these use `ACTION_VIEW`/`ACTION_SENDTO` deep links rather than
  WhatsApp Business API or Gmail's API, so they open the target app with content pre-filled and
  require the user to hit send themselves — there's no way to silently send on a user's behalf via
  public Intents, which is also the right privacy boundary for an assistant doing this
  autonomously.
- **Icons**: the launcher icon included is a placeholder vector (a simple gradient orb) — swap
  `ic_launcher_foreground.xml` / `ic_launcher_background.xml` for real artwork before shipping.

## Extending the tool set

Add a new tool in three places: the JSON schema in `ToolDeclarations.FUNCTION_DECLARATIONS`, the
native implementation in `DeviceTools`, and a `when` branch in `ToolExecutionEngine.execute()`.
Nothing else needs to change — `LiveSessionManager` dispatches by name generically.

## CI/CD (GitHub Actions)

Two workflows are included under `.github/workflows/`:

- **`android-ci.yml`** — runs on every push/PR to `main`: lint, unit tests, and an unsigned debug
  APK build, uploaded as a workflow artifact. Nothing to configure — works as soon as you push.
- **`android-release.yml`** — runs when you push a tag like `v1.0.0`: builds a **signed** release
  APK and attaches it to a new GitHub Release. Needs four repo secrets first:
  1. Generate a keystore locally: 
     `keytool -genkey -v -keystore zoya-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias zoya`
  2. Base64-encode it (`base64 -w0 zoya-release.jks` on Linux, `base64 -i zoya-release.jks | pbcopy` on Mac)
     and add it as the `ZOYA_KEYSTORE_BASE64` secret in your repo (Settings → Secrets and
     variables → Actions).
  3. Add `ZOYA_KEYSTORE_PASSWORD`, `ZOYA_KEY_ALIAS`, `ZOYA_KEY_PASSWORD` secrets to match.
  4. Push a tag: `git tag v1.0.0 && git push origin v1.0.0`.

Both workflows install Gradle directly via `gradle/actions/setup-gradle` rather than relying on a
committed `gradlew` wrapper — this repo doesn't ship one (Android Studio generates it on first
open), so if you'd rather use the wrapper, run `gradle wrapper` locally once, commit the result,
and swap the workflow steps to `./gradlew ...`.
