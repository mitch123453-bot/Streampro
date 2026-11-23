# StreamPro Android TV / Fire TV

Native Kotlin starter project for an IPTV **player** using Xtream Codes credentials and AndroidX Media3 (ExoPlayer).

## Included
- Xtream Codes API authentication (`player_api.php`)
- Loads live channel list (`get_live_streams`)
- Plays selected live streams with built-in Media3 player
- Android TV / Fire TV launcher entry and remote-friendly large controls
- No channels, subscriptions, playlists, or credentials bundled

## Build
1. Install Android Studio (latest stable) and Android SDK Platform 35.
2. Open this folder in Android Studio.
3. Allow Gradle sync.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Install the generated APK on your own Fire TV / Android TV device.

Command line: `gradle assembleDebug` (with Android SDK configured).

## Notes / next work
This is an initial working starter, not a store-ready release. EPG, VOD, series, category filtering, favourites, secure credential storage, HTTPS-only policy, robust stream format handling, and provider-specific compatibility checks are not yet implemented. Xtream endpoints and stream URL formats vary by provider. Only use subscriptions and streams you are authorised to access.
