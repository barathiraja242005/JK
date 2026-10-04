# JK — Train smarter. Live stronger.

Offline-first fitness app for Android: Kotlin + Jetpack Compose + Material 3 + Room.

## Features
| Tab | What's in it |
|---|---|
| **Today** | Daily rotating workout, streak, steps/water/calorie rings, active challenge, today's read, macro target |
| **Train** | 24 home & gym programs · 6 progressive challenges (30-day push-up, squat, plank, abs, transformation, 21-day HIIT) · 873-exercise library with animated photo demos, filters, saves and YouTube tutorials · custom workout builder |
| **Shorts** | Full-screen vertical feed of exercise demos: double-tap like, save, share, tutorial |
| **Health** | Water bottle tracker, steps + walk/run sessions, diet log (130+ Indian & common foods, macros, veg filter) + meal plans, fasting, guided meditation with generated ambient sound, breathing, calculators, stopwatch & interval/Tabata timer, guides |
| **Progress** | Weekly charts, weight trend, walks/mindful minutes, before/after photo slider, history; **Me** (avatar) for profile, settings, reminders, help |

Guided player: get-ready, timed/rep sets, rest, rounds, voice coach, beeps, animated exercise demo, confetti on finish.

## Training system (daily plan)
Modelled on GymFaction's flow, but generated on-device (`domain/TrainingEngine.kt`, `data/TrainingRepo.kt`):
- **Setup** (after onboarding, or ⋮ → Edit Workout Preferences): gym/home, level, goal, active days, equipment, injuries, rest time.
- **Weekly split** (⋮ → Edit Workout Days): body parts per weekday, e.g. Mon Chest+Abdomen+Triceps; rest days.
- **Today's Workout**: week strip, progress, warm-up, exercises grouped by body part with **+ Add**, replace/remove,
  set · reps · weight tables, fat-loss switch, complete all, rest-day card, history.
- **Exercise session**: complete sets, edit reps/weight/sets, rest timer with beeps, timed sets, last-session numbers, prev/next.
- **Progressive overload**: weights carry over from your last session and go up when every set hit its reps.
- Curated pool of 175 exercises across 8 body parts with injury filters; photos bundled offline.

## Builds
- `./gradlew assembleRelease` — fast build for the phone (signed with the local debug key; replace with an upload key for Play Store).
- `./gradlew testDebugUnitTest` — 25 unit tests (health maths, catalog, training engine).

## Assets & licences
- Exercises + photos: [free-exercise-db](https://github.com/yuhonas/free-exercise-db) (Unlicense / public domain).
  Photos for the 68 exercises used by built-in programs are bundled in `assets/exercise_images`; the rest load online and are cached by Coil.
- Font: Inter (SIL OFL 1.1). Icons: Material Symbols (Apache 2.0).
- Animations (confetti, water wave, breathing) are Compose code; meditation soundscapes are synthesised at runtime (`audio/Ambient.kt`).
- Food values are typical-serving estimates; articles and meditation scripts are original.

## Build
```
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug testDebugUnitTest
adb install app/build/outputs/apk/debug/app-debug.apk
```
Or open this folder in Android Studio and press Run.

## Layout
```
app/src/main/java/com/barathiraja/jk/
  JkApp.kt, MainActivity.kt      app container (manual DI), entry point
  data/                          Room entities/DAO/DB, UserPrefs, workout Catalog
  domain/Health.kt               all health math (unit-tested)
  steps/StepTracker.kt           TYPE_STEP_COUNTER -> steps today
  reminders/Reminders.kt         AlarmManager reminders + boot receiver
  ui/                            JkViewModel, navigation root, theme, components, screens/
```

## Roadmap
Accounts + cloud sync, community feed, premium/payments, multi-language (GymFaction ships 8), background step counting via a foreground service, home-screen widget.
