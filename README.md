# Lorebook Forge

A native Android app for creating, editing, and standardizing lorebook JSON
files for AI chatbots — fully offline, no network permissions, no analytics.
Files are opened and saved directly through Android's Storage Access
Framework, so a lorebook is just a `.json` file you pick once; the app
re-opens it from your "library" list afterward with no extra prompts.

## How it works

- **Library** — the home screen lists lorebooks you've opened before. The
  folder icon opens an existing `.json` file from anywhere on your device;
  the `+` creates a new one (you choose where to save it).
- **Switches** — every property an entry can have (`key`, `content`,
  `order`, and so on) is a "switch": a name + type (text, number, or
  toggle) that you can rename, retype, or delete freely, including the
  defaults a new book starts with. The wrench icon on a book's screen opens
  the full switch registry.
- **Entries** — listed by whichever switch you've marked "Entry title"
  (comment, by default) with "Entry subtitle" underneath (keys, by
  default). Tapping an entry opens its editor; the `+` there opens a list
  of this book's other switches to turn on for that entry, with "New
  switch" at the bottom to define one on the spot.
- **File format** — because switches are per-book rather than fixed, every
  exported file declares its own schema in a top-level `switches` array,
  so each `.json` is self-describing and portable on its own.

Autosave writes back to the open file about 400ms after you stop typing —
no explicit save button.

## Building the APK

This project builds with Gradle and the Android SDK; GitHub's hosted
runners already have both, so the included workflow
(`.github/workflows/build.yml`) builds a debug APK on every push to `main`
and on manual runs. To get an installable APK without installing anything
yourself:

1. Create a new GitHub repository and push this project to it.
2. Open the repo's **Actions** tab — the workflow runs automatically on
   push (or trigger it manually with "Run workflow").
3. Once the run finishes, open it and download the `lorebook-forge-debug`
   artifact — it's a zip containing `app-debug.apk`.
4. Copy the APK to your phone and open it to install. Android will likely
   ask you to allow installs from that source (Settings → Apps → your
   file manager / browser → "Install unknown apps") since this isn't from
   the Play Store.

This debug build is self-signed automatically by Gradle's debug keystore,
which is fine for installing on your own device. If you ever want a
release build with stable signing across updates, that needs a keystore
stored as a GitHub Actions secret — ask and it's a small addition to the
workflow.

## Building locally (optional)

If you'd rather build from Android Studio:

1. Open this folder as a project (Android Studio will recognize the
   Gradle project automatically).
2. Let Gradle sync, then Run on a device or emulator, or
   **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

## Project layout

```
app/src/main/java/com/lorebookforge/app/
  model/        Switch, Entry, and default-schema data classes
  data/         JSON (de)serialization, file I/O, recent-files storage
  ui/           Compose screens: Library, Book, Entry editor, dialogs
  AppViewModel.kt   All app state and mutation logic
  MainActivity.kt
```
