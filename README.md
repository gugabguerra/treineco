# GymPlan

GymPlan is a small Android app for keeping your gym training plans on your phone: every exercise
with its sets, reps, weight, seat position and machine adjustments, so you stop relying on paper
cards, notes apps or memory between sessions. It also tracks how your working weights and your
body weight change over time.

It is a single offline app. All data stays in one file in the app's private storage on the phone —
no account, no server, no network access. Uninstalling the app or clearing its data deletes
everything.

## Project status

- Hobby project for personal use, published as-is. No support offered: issues and pull requests
  may sit unanswered, and there are no release schedules or compatibility promises.
- Forking is not just allowed — it is the recommended path if you want changes.
- Maintained only as the author's own needs dictate.

## Features

**Plans & exercises**

- Create several training plans (e.g. "Day A", "Day B") and switch between them with cards at the
  top of the home screen; each card shows today's progress (`done / total`). Rename or delete a
  plan.
- Strength exercises store sets, reps (free text, so `8-12` works), weight in kg, seat adjustment
  and other adjustments / notes.
- Cardio exercises (treadmill, bike, …) store duration in minutes, speed / incline / level, seat
  adjustment and notes.
- Drag the handle on the left of an exercise card to reorder the plan; long-press a card to
  delete the exercise.
- Decimal values accept both comma and dot (`42,5` or `42.5`).

**Workout**

- Tick an exercise off with the circle on its card or the *Mark complete* button; the plan shows
  `done / total` for today. Ticks reset automatically the next day, or tap *Clear* to reset the
  plan's ticks right away.

**Progress**

- Every change to an exercise's weight (or a cardio exercise's duration) is logged with the date
  and shown as a chart plus history list on the exercise screen.
- Body tab: log your body weight, see the current value, a chart of its evolution and the full
  history; delete wrong entries.

**App**

- Dark theme with `#7BC4FF` accents, glass-style cards, the Oxanium typeface, and a floating
  bottom bar with the add button built in.
- Settings: choose the app language — system default, English or Portuguese (Brazil) — without
  changing the phone's language. Numbers and dates follow the chosen language.
- Android 8.0 (API 26) or newer.

## 1. Run it

Needs the Android SDK (installed with Android Studio) and JDK 17+. On macOS the `Makefile` uses
Android Studio's bundled JDK automatically when `JAVA_HOME` is not set. Gradle finds the SDK
through `ANDROID_HOME` or a `local.properties` file (`sdk.dir=/path/to/Android/sdk`, gitignored);
Android Studio creates the latter on first open.

```bash
make build   # debug APK -> app/build/outputs/apk/debug/app-debug.apk
make run     # install on the connected phone/emulator and launch
make test    # JVM unit tests
make lint    # Android Lint
```

To install on a phone, enable *Developer options → USB debugging*, connect it and run `make run`.
Alternatively open the folder in Android Studio and press Run.

### Logs

The app logs nothing of its own. Crashes show up in logcat:

```bash
adb logcat -b crash
adb logcat --pid=$(adb shell pidof com.example.gymplan)
```

### Data

Everything lives in `files/gym.json` inside the app's private storage, written atomically (temp
file + rename). If the file cannot be parsed on startup it is renamed to `gym.json.bad-<epoch ms>`
and the app starts empty instead of overwriting it. On a debug build you can back it up with:

```bash
adb shell run-as com.example.gymplan cat files/gym.json > gym-backup.json
```

## Security notes

The app requests no permissions and has no network access; your data never leaves the phone. It
is not encrypted beyond Android's own app sandbox and device encryption. Details in
[docs/SECURITY.md](docs/SECURITY.md).

## Version history

See [docs/CHANGELOG.md](docs/CHANGELOG.md); the current version lives in `VERSION`.

## License

MIT — see [LICENSE](LICENSE). The bundled Oxanium font is licensed under the SIL Open Font
License 1.1 ([docs/licenses/Oxanium-OFL.txt](docs/licenses/Oxanium-OFL.txt)).
