# Privacy Policy — BitChord

Last updated: 2026-10-24

## Summary

BitChord is a music client. It has no account system of its own, no analytics or
telemetry SDK, no crash-reporting service, and no advertising. Nothing is sent to
a server operated by this project. Your library, playlists, listening history,
settings, and downloads are stored on the device and leave it only when you use a
feature that talks to a service you chose (for example signing in to Spotify or
enabling scrobbling).

## What the app stores on your device

- Playlists, liked songs, listening history, and queue state.
- Settings and preferences.
- Downloaded audio and any cached artwork or metadata.
- Optional integration tokens (for example a Spotify token you granted, or a
  ListenBrainz token you entered). These stay on the device.

## What is sent, and only when you use those features

- Music, artwork, and metadata requests go to the service serving the content
  (for example YouTube Music, or Spotify if you sign in to it).
- Optional Last.fm / ListenBrainz scrobbling sends your listening activity to
  the service you enabled.
- Optional Discord Rich Presence shares the track you are playing with Discord,
  if you turn it on.
- Optional lyrics and metadata lookups go to the configured provider.

No data is sent to a server operated by this project.

## Permissions

| Permission | Why it is declared | Required? |
| --- | --- | --- |
| `INTERNET` | All network access (music, artwork, metadata) | Yes |
| `ACCESS_NETWORK_STATE` | Offline detection and retry policy | Yes |
| `WAKE_LOCK` | Background playback and downloads | Yes |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `FOREGROUND_SERVICE_DATA_SYNC` | Playback and download services | Yes |
| `POST_NOTIFICATIONS` | Playback notification and download progress | No |
| `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE` | Play audio files already on the device | No |
| `WRITE_EXTERNAL_STORAGE` | Save downloads on older Android versions | No |
| `VIBRATE` | Haptic feedback on controls | No |
| `BLUETOOTH_CONNECT` | Control playback from a connected device | No |
| `REQUEST_INSTALL_PACKAGES` | Install the companion watch app you ask for | No |

## Data deletion

Uninstalling the app removes everything it stored on the device. If you signed in
to a third-party service, revoke the app's access from that service's account
settings.

## Children

The app is not directed at children and does not knowingly collect personal
information from them.

## Changes

Material changes to this policy are recorded in this file's history in the
repository.

## Contact

Open an issue at https://github.com/Exploit7492/BitChord.
