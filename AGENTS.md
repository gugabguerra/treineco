# AGENTS.md

GymPlan: offline Android app for gym training plans, exercise settings and weight progress.
Kotlin 2.2 + Jetpack Compose (Material 3), AGP 8.13, Gradle 8.13 wrapper, JDK 17 target, minSdk
26 / targetSdk 36. Storage is one JSON file via Android's built-in `org.json` — no database, no
network. Tests: JUnit 4 on the JVM. Dependency policy: AndroidX/Compose only; ask before adding
anything else.

## Documentation

- `README.md` — canonical feature inventory and product behavior. Update its **Features** list
  whenever user-visible behavior changes.
- `docs/CHANGELOG.md` — add an `### Added` / `### Fixed` entry under `[Unreleased]` for notable
  behavior changes (minor or quick ones need none); add the version section at the next tag.
- `docs/SECURITY.md` — canonical security model; update it when storage, permissions, or network
  behavior changes.
- `docs/CONVENTIONS.md` — human-facing standards: git workflow, commit style, doc routing,
  versioning, dependencies, and markdown/code style. This file defers to it.
- `docs/PUBLISHING-ANONYMOUSLY.md` — checklist for releases that must not identify the maintainer;
  follow it before the first public push.
- `docs/local/` — gitignored planning, design, issue, build and deployment notes; never commit its
  contents. Keep private hosts, credentials, and tokens out of tracked files.
- **Markdown style**: wrap prose at 100 columns; keep code blocks and tables unchanged.

## Maintaining AGENTS.md

Durable agent policy only — not product docs, feature logs, or API specs. Never edit it during
routine work (features, fixes, refactors, UI tweaks). Edit only when the user explicitly asks, when
a core dev command or workflow rule changes, or when a cross-cutting invariant spans subsystems and
can't be enforced by tests or code comments. Routing: feature behavior → README; security behavior
→ SECURITY.md; narrow implementation details → code comments and unit tests.

## Run & verify

- Commands: the root `Makefile` (`make run` / `make test` / `make lint` / `make build`) is
  canonical — keep the lines below and README § Run it in sync with it.
- Start: `make run` (needs a device or emulator in `adb devices`; `emulator -list-avds` lists
  local emulators).
- Tests: `make test`.
- State: `files/gym.json` in the app's private storage; survives restarts and updates, wiped by
  uninstall or *Clear data*. Reset on a device: `adb shell pm clear com.example.gymplan`.
- No system JDK on the maintainer's Mac: the Makefile falls back to Android Studio's bundled JBR;
  raw `./gradlew` calls need `JAVA_HOME` set the same way.

## Git workflow

- **Never commit without the user's explicit acknowledgement** — ask first, then wait. Keep
  AGENTS.md edits out of commit messages.
- `main` is stable; never commit feature work directly to it unless the user explicitly asks (minor
  change). One branch per change: `feature/<short-name>`, `fix/<short-name>`.
- Conventional Commits (`feat:`, `fix:`, `refactor:`, `chore:`, `docs:`); squash-merge into `main`,
  then delete the branch. Tag stable checkpoints: `git tag -a vX.Y.Z -m "..."`.

## Layout

Single-component Android project in the standard Gradle shape (`app/` takes the role of `src/`):

- `app/src/main/java/com/example/gymplan/Model.kt` — data classes, pure `Data.*` rules (upsert,
  delete, weight-history logging, daily completion), JSON encode/decode, and `Store` (Compose
  state + atomic file save).
- `app/src/main/java/com/example/gymplan/MainActivity.kt` — theme, navigation (ids in
  `rememberSaveable`, no nav library), all screens, shared building blocks, and dialogs.
- `app/src/test/` — JVM unit tests, one file per module (`ModelTest.kt`).
- `VERSION` — app version; `app/build.gradle.kts` derives `versionName`/`versionCode` from it.
- `docs/` — `CHANGELOG.md`, `SECURITY.md`, `CONVENTIONS.md`, `PUBLISHING-ANONYMOUSLY.md`;
  `docs/local/` is gitignored scratch space.

## Invariants (easy to break)

- Persist every mutation through `Store.update { ... }`, which writes `gym.json` atomically (temp +
  rename). Never write the file elsewhere.
- Saved data must keep loading after updates: new JSON fields are read with `opt*` defaults; never
  rename or drop a key without reading the old one. An unreadable file is set aside as
  `gym.json.bad-<ms>`, never overwritten.
- Logic that depends on "now" takes the time/day as a parameter (`upsertExercise(now)`,
  `toggleDone(day)`) so tests don't depend on the clock.
- `history` holds the *tracked* value: kg for strength, minutes for cardio (`Exercise.tracked` /
  `Exercise.unit`). Deleting an exercise or plan also deletes its `history` and `done` entries.
- Root composables must provide `LocalContentColor` (there is no `Surface` at the root); without it
  text renders black on the dark background.
- Keep private hosts, credentials, tokens, and personal data out of tracked files; `docs/local/` is
  the place for them.
