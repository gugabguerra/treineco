# Security

GymPlan is a single-user, offline Android app. It has no account, no server and no network code;
it requests no Android permissions. The only data it handles is what you type in — plans,
exercise settings, body weight — stored on the phone.

## How access works

- Data lives in `files/gym.json` inside the app's private storage. Android's app sandbox keeps
  other apps from reading it; anyone who can unlock the phone can open the app and see it.
- There is no in-app lock, login or export endpoint.

## What is *not* protected

- **Data at rest** is not encrypted by the app — it relies on Android's device encryption
  (on by default since Android 10). Mitigation: keep a screen lock on the phone.
- **Debug builds** allow `adb shell run-as gugabguerra.gymplan` to read the file from a connected
  computer with USB debugging authorized. Mitigation: install a release build, or turn USB
  debugging off when not developing.
- **Backups**: Android's default auto-backup may copy app data to your Google account backup.
  Mitigation: disable backup for the app, or add `android:allowBackup="false"` if that matters.
- **Release signing key**: `docs/local/release.keystore` and its password file
  `docs/local/keystore.properties` stay out of git. Whoever holds them can sign updates that
  replace the installed app. Mitigation: keep them private and backed up offline.
- **No backup of your own**: uninstalling or clearing app data deletes everything permanently.

## Hardening checklist

1. Keep a screen lock and device encryption enabled.
2. Use a release build on your daily phone; disable USB debugging when not needed.
3. Decide whether you want Android auto-backup to include the app's data.

## Reporting a problem

Best-effort only: if the repository is public, use GitHub's *private vulnerability reporting*.
Answers have no service level — see README § Project status. Include the version from
`docs/CHANGELOG.md` and any `adb logcat -b crash` output (the app logs no personal data).
