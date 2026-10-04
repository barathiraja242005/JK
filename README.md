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

## Gym mode (single gym)
Trainers assign workouts, members complete them, the owner watches live reports, and everyone competes on a leaderboard.
Uses Firebase (free Spark plan): Google sign-in + Firestore. Code in `gym/` and `ui/gym/`; rules in `firestore.rules`.

| Role | Tabs | Can do |
|---|---|---|
| **Owner** | Gym · Ranks · Me | Create the gym (gets a gym code), approve trainers, see today's totals and every trainer's completion rate |
| **Trainer** | Members · Assign · Ranks · Train · Me | Join with the gym code (owner approves), invite members with their own code, assign workouts (many members/days, weekly repeat, templates), verify sessions, notes |
| **Member** | Today · Train · Gym · Health · Progress | Join with the trainer's code, do the assigned workout (pinned on Today), log sets, see points/rank/awards; all personal JK features stay |

**Points** (`gym/Scoring.kt`, unit-tested): workout done +10, done on the day +5, each set +1 (max 30/day), coach verified +5, every workout in a week +20.
Trainers are scored on their members' completion rate, not on how much they assign.
**Monthly awards** on the 1st: Best Member, Best Trainer, Most Consistent, Most Improved, Iron Lifter.

**Set up Firebase once:** create a project, add Android app `com.barathiraja.jk` with the debug key's SHA-1, enable
Authentication → Google and Firestore, put `google-services.json` in `app/` (git-ignored), and paste `firestore.rules`.
Without `google-services.json` the app builds and runs as a personal app.

## Builds
- `./gradlew assembleRelease` — fast build for the phone (signed with the local debug key; replace with an upload key for Play Store).
- `./gradlew testDebugUnitTest` — 34 unit tests (health maths, catalog, training engine, gym scoring).

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
Push notifications for new workouts (needs Cloud Functions / Blaze plan), cloud backup of personal data, community feed, premium/payments, multi-language (GymFaction ships 8), background step counting via a foreground service, home-screen widget.
