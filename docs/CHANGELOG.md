# Changelog

All notable changes to Treineco. Newest first; versions match the git tags and the root `VERSION`
file.

## [Unreleased]

## [0.5.0] - 2026-11-19

### Added

- Languages: Español, Français and Português (Portugal), alongside English and Português BR
  (Brazil). The old "Português" option is now Português BR, so saved choices keep their language.

### Changed

- The app is renamed Treineco (launcher label, title, export file `treineco-<date>.json`).
- Application id is now `gugabguerra.treineco`. It installs as a separate app from
  `gugabguerra.gymplan` builds, which keep their own data; export from the old app and import
  into the new one to move it.
- Icons come from Google's Material Symbols (filled, 24dp) as vector drawables. The
  `material-icons-core` dependency is removed.
- New / Edit exercise: labels sit above each field, Sets, Reps and Weight share one row, Seat and
  Notes are relabelled, and the split control is an arrow icon with the hint "select for each set".
  Sets and Reps use the same number wheels as before.
- Training list: sets × reps sit under the weight on the right, and the exercise name wraps to two
  lines before it is cut. Varying reps and weights show the first three values, then `...`.

### Fixed

- Exercise screen: the weight summary of a per-set breakdown wraps onto its own line instead of
  being squeezed into a narrow column when there are several sets.

## [0.4.0] - 2026-11-19

### Added

- Settings → *Export data* and *Import data*: save all plans, history, weigh-ins and defaults to a
  JSON file or restore from one, using Android's file picker. Import replaces all data after a
  confirmation that shows what the backup contains; files that are not GymPlan backups are
  rejected. The format is the same JSON as the app's storage file, unencrypted.

## [0.3.0] - 2026-11-18

### Added

- Per-set reps and weights: with more than one set, the icon inside the Reps or Weight field opens
  a breakdown with one control per set.
- Strength exercises with no weight (0 kg) show no weight on the training list and exercise screen.
- Long-press an exercise to edit it, or clone it into another plan.

### Changed

- Sets and reps are picked by scrolling a vertical number wheel instead of typing into text fields.
- Training list: a single set shows only its reps ("× 10"), varying reps show only the per-set
  values ("12 / 10 / 8"), and varying weights show in a smaller font ("70 / 65 / 60 / ...").

## [0.2.0] - 2026-11-14

### Added

- Training plans with strength exercises: sets, reps, weight, seat adjustment and other
  adjustments.
- Cardio exercises (treadmill, bike, …) with duration, speed / incline / level, seat and notes.
- Daily completion: tick exercises off; ticks reset the next day.
- Weight evolution per exercise (duration for cardio), logged automatically on every change, with
  chart and history.
- Body-weight log with chart and history.
- Dark theme with `#7BC4FF` accents, glass cards and floating bottom bar.
- *Clear* button to reset a plan's completed checks.
- Oxanium typeface throughout the app.
- Drag-to-reorder exercises with a handle on each card; long-press a card to delete it.
- App icon: adaptive launcher icon (clipboard with a dumbbell) with a themed-icon layer for
  Android 13+, plus a 512 px Play Store version.
- Settings screen with an in-app language choice; Portuguese (Brazil) translation.
- Signed, minified release build (`make release`).
- Long-press a plan card for a menu: sort plans, clone plan, rename, delete (replaces the rename
  and delete buttons).
- Long-press an exercise card to clone the exercise (or delete it).
- New exercises are pre-filled with the sets, reps and weight of the last strength exercise added.

### Changed

- Application id is now `gugabguerra.gymplan`; it installs as a separate app from earlier
  `com.example.gymplan` builds, which keep their own data.
- The add button now sits inside the bottom bar as a flat accent circle instead of a separate
  glowing button.
- Dialogs restyled: rounded glass look, filled confirm button, red confirm for deletions, and a
  Strength / Cardio segmented switch.
- Plans tab icon is now a dumbbell; the letter circle on exercise cards is replaced by the drag
  handle.
- UI polish: fade between tabs and a resizing bottom bar; empty plans read "No exercises" and
  hide the `0/0 done` count; the new-plan chip and *Completed today* button use real icons.
- Plan chips replaced by plan cards with a progress bar for today's ticks; *New plan* is a dashed
  card at the end of the row.
- A plan's ticks now clear 24 h after its last tick instead of at midnight.
- Cardio exercises no longer have a seat adjustment field.

### Fixed

- Grey window flash on launch; the window background now matches the app's dark theme.
- Accessibility: exercise check circles and header buttons have 48dp touch targets; checks, tabs,
  plan chips and language options are announced with their role and selected state.
