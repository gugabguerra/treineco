# Changelog

All notable changes to GymPlan. Newest first; versions match the git tags and the root `VERSION`
file.

## [Unreleased]

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

### Changed

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

### Fixed

- Grey window flash on launch; the window background now matches the app's dark theme.
- Accessibility: exercise check circles and header buttons have 48dp touch targets; checks, tabs,
  plan chips and language options are announced with their role and selected state.
