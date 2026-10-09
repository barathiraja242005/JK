# JK — Train smarter. Live stronger.

A single-gym fitness app for Android: the owner runs the gym, trainers coach, members train.
Kotlin + Jetpack Compose + Material 3 + Room, with Firebase (Google sign-in + Firestore, free Spark plan) for the gym.
Everyone signs in with Google and joins the gym with a code.

## Who sees what
| Role | Tabs | Can do |
|---|---|---|
| **Owner** | Home · People · Awards · Me | Create the gym (gets a gym code), approve trainers, see today's turnout and every trainer's results, move or remove people, give awards |
| **Trainer** | Home · Members · Ranks · Me | Join with the gym code (the owner approves), invite members with their own member code (can make a new one), assign workouts (many members and days, templates), check finished workouts, leave notes |
| **Member** | Today · Train · Gym · Health · Progress | Join with a trainer's code, do the coach's workouts (week plan on Train), log sets and weights, see rank, points and awards; plus water, steps, diet, fasting, meditation, breathing, calculators, timers and guides. **Me** opens from the photo. |

Members whose coach hasn't sent a workout yet get a plan JK generates on the phone (`domain/TrainingEngine.kt`).
Programs, challenges and custom workouts are extras: they count toward the streak, not gym points.

**Points** (`gym/Scoring.kt`, unit-tested): workout done +10, done on the day +5, each set +1 (max 30 a day), checked by the
coach +5, every workout in a week done +20. Trainers are scored on their members' completion rate, not on how much they assign.
**Monthly awards** are saved by the owner's phone a day into the new month: Best Member, Best Trainer, Most Consistent,
Most Improved, Iron Lifter.

**Test mode** (link at the bottom of Me, or on the sign-in screen) runs every screen on a made-up gym kept in memory
(`gym/TestGym.kt`), switchable between owner, trainer, member, waiting trainer and newcomer. Your real settings are put
back when you leave it.

## Firebase setup (once)
1. Create a project and add the Android app `com.barathiraja.jk` with your signing key's SHA-1.
2. Enable Authentication → Google, and Firestore.
3. Put `google-services.json` in `app/` (git-ignored).
4. Firestore → Rules: paste `firestore.rules` and Publish. **Republish whenever that file changes.**
5. Firestore → Indexes: add the two composite indexes in `firestore.indexes.json` (assignments: `memberUid` + `epochDay`,
   and `trainerUid` + `epochDay`, both ascending). Until they exist the app reads the whole gym instead (slower, more reads).

## Build
```
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleRelease testDebugUnitTest
adb install -r app/build/outputs/apk/release/app-release.apk
```
- Release builds are shrunk with R8. They're signed with the key in `keystore.properties` (git-ignored, see below),
  or the local debug key when that file doesn't exist (fine for your own phones, not for the Play Store).
- `keystore.properties`: `storeFile=/path/to/jk-upload.jks`, `storePassword=…`, `keyAlias=…`, `keyPassword=…`.
  Add that key's SHA-1 in Firebase. Switching keys means uninstalling the old app first (its local history goes).
- `./gradlew testDebugUnitTest` runs 77 unit tests: scoring, awards, history sync, lifts, health maths, catalog, training engine, the test gym.

## Privacy and security
- Firestore rules (`firestore.rules`): only the owner approves trainers; removals stick (only someone who left by
  themselves can rejoin); trainer codes can be retired; members can only log their own open, unchecked workouts; names
  and photos are checked. Everyone in the gym can read the gym's workouts (the leaderboard needs them).
- Phone backups include only JK's own history and settings, never Firestore's copy of the gym or sign-in tokens.
- Signing out clears the phone, including Firestore's offline copy. **Delete my account** (Me) removes your name and
  photo from the gym, your cloud backup and your Firebase account.

## Layout
```
app/src/main/java/com/barathiraja/jk/
  JkApp.kt, MainActivity.kt   app container (manual DI): database, prefs, Firebase, test mode, phone wipe
  data/                       Room entities, DAO, database, UserPrefs, workout catalog, exercise library
  domain/                     Health maths, TrainingEngine (generated plans)
  gym/                        Firestore repo, TestGym, models, Scoring, OwnerStats, Lifts, HistorySync, AwardsJob
  steps/                      step counter and its permission
  reminders/                  AlarmManager reminders + boot receiver
  ui/theme/                   palette (Jk), typography, Tone
  ui/components/              shared kit: pages, cards, buttons, rings, avatars, scaffolds
  ui/member/                  member Today, Train plan, Gym, workout screen
  ui/gym/                     owner and trainer screens, ranks, awards, workout cards, rest timer
  ui/screens/                 Train extras, Health tools, Progress, Me, diet, meditation, player…
```

## Assets & licences
- Exercises + photos: [free-exercise-db](https://github.com/yuhonas/free-exercise-db) (Unlicense / public domain).
  Photos for the common exercises are bundled in `assets/exercise_images`; the rest load online and are cached by Coil.
- Font: IBM Plex Sans & Mono (SIL OFL 1.1, licence in `third_party/ibm-plex/`). Icons: Material Symbols (Apache 2.0).
- Animations (confetti, water wave, breathing) are Compose code; meditation soundscapes are synthesised at runtime (`audio/Ambient.kt`).
- Food values are typical-serving estimates; articles and meditation scripts are original.

## Roadmap
Push notifications for new workouts (needs Cloud Functions / Blaze plan), steps counted in the background (Health Connect),
multi-language, home-screen widget.
