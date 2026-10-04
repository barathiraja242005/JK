# Reference analysis: GymFaction.apk (com.thegymfiction.app v2.0.27, build 92)

## Stack
- **Flutter** app (embedding v2), minSdk 24 / targetSdk 35. Business logic is compiled Dart in
  `libapp.so`, which lives in a split ABI APK that was **not** in the folder, so screens/API calls
  were inferred from plugins, assets and native code.
- State: MobX (`flutter_mobx`) + flutter_hooks. Local DB: sqflite. Prefs: shared_preferences.
- Backend: REST at `https://thegymfaction.com/api/*` (Bearer token) + Firebase
  (Auth, Realtime Database, Messaging, Analytics, Crashlytics).

## Features found
| Area | Evidence |
|---|---|
| Home/gym workouts, "today's workout", video player, shorts feed | `homeWorkout`, `gymWorkout`, `todayWorkout.*`, `video_player`, `short_player`, `preload_page_view` |
| Step counter (synced to server) | `pedometer`, `ACTIVITY_RECOGNITION`, `/api/add_step_count` |
| Water tracker | `waterBottleAnimation`, `drinkWater`, `water_drop` |
| Intermittent fasting | `fasting*`, `fastingImage` |
| Meditation & breathing (audio, TTS) | `breathing.mp3`, `inhale/exhale`, `meditation*`, `just_audio`, `flutter_tts` |
| Diet/macros, BMI/BMR/ideal-weight calculators | `ic_bmi`, `ic_bmr`, `ic_ideal_weight`, `ic_protein/carbs/fat` |
| Reports/charts, history | `syncfusion_flutter_charts`, `lineChart`, `history` |
| Challenges / body transformation | `assets/Challenge/*`, trophy/level icons |
| Community: posts, likes, comments, bookmarks, block user | `post`, `like*Animation`, `comment`, `bookmark`, `block_user` |
| Blog / user guide | `ic_blog`, `book_page`, `flutter_html`, webview |
| Subscriptions & payments (India + PayPal) | Razorpay, GPay/PhonePe/Paytm/BHIM/CRED icons, coupons, bronze/silver/gold tiers |
| Login | Google Sign-In, Sign in with Apple, Firebase Auth, OTP (`pinput`) |
| Reminders & push | awesome_notifications, flutter_local_notifications, FCM |
| Themes & languages | light/dark/day/night images; flags: en, ar, fr, pt, tr, vi, af, in |
| Ads | google_mobile_ads |

## Issues spotted
1. **AdMob uses Google's sample app ID** (`ca-app-pub-3940256099942544~3347511713`) in a release
   build: ads either show test ads or nothing; no revenue.
2. `MyFirebaseMessagingService` POSTs the step count on **every** data push, reading the auth token
   from plain-text `FlutterSharedPreferences` — unencrypted token, and any push triggers network I/O.
3. Over-broad permissions for its features: RECORD_AUDIO, CAMERA, NFC, BIND_NOTIFICATION_LISTENER,
   BROADCAST_CLOSE_SYSTEM_DIALOGS, many launcher-badge permissions, FOREGROUND_SERVICE typed
   `dataSync|mediaPlayback|location` on the FCM service.
4. **APK bloat (~94 MB)**: multi-MB GIFs/PNGs/MP4 bundled (e.g. 8.9 MB `untitled_design_2.png`,
   7.3 MB GIF, 3 MB Lottie JSONs), duplicate splash GIFs, a stray `assets/.DS_Store`.
5. Leftover/placeholder strings (`app_name = checkout-otpelf-base` from a Razorpay lib).

## How JK differs
Native Kotlin/Compose, offline-first, no ads/trackers, only the permissions it uses
(activity recognition, notifications, vibrate, boot), vector icon, ~19 MB debug APK.
All code, exercise content and branding in JK are original; nothing was copied from the APK.
