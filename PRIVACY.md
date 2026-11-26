# Melo Privacy Policy

Last updated: November 20, 2026

Melo uses basic app-usage analytics to understand how the app is used and help improve it.

## Usage analytics

Analytics is on by default. When enabled, Firebase Analytics helps us estimate app sessions and daily or monthly active installations. Firebase automatically receives app-lifecycle and engagement signals, a per-install app-instance identifier, app and Android version details, and basic device information. Google Analytics may also infer a broad region, such as a country, from network information.

When enabled, Melo also tells `api.melo.aarushsagar.com` when the app is open, for a public count of how many people are using Melo right now. While Melo is in the foreground it sends a random installation ID and the platform (Android) every five minutes, and a closing signal when you leave the app. Music playing in the background is not counted. Installation IDs are HMAC-hashed on the server, and only the hash, the platform and the time of the last signal are kept. They are deleted when you leave the app, or 90 seconds after a missed signal. Public stats contain counts only.

Melo does not send music account details, Spotify cookies, track or artist names, track IDs, searches, playlists, queue contents, or listening history. Playback time is an estimate based on player-reported progress and does not prove that audio was audible. Automatic screen-view reporting, Android Advertising ID collection, and ad-personalization signals are disabled.

You can switch analytics off anytime in **Settings → Privacy**. This stops future analytics collection by Melo. Melo sends a closing signal when you do, so you leave the live count at once; if that signal cannot get through, the server forgets the installation within about seven minutes. Data already sent to Firebase remains subject to Firebase/Google Analytics retention.

Google processes Analytics data to provide its reporting service. See [Google’s privacy information](https://policies.google.com/privacy) and [Firebase’s Android data disclosure](https://firebase.google.com/docs/android/play-data-disclosure).

## Music services and on-device data

When you connect a music service, search its catalog, or play content, that service receives the requests needed to provide the feature. Its own privacy policy applies. Melo stores app settings, caches, and local downloads on your device; these are not sent to Firebase Analytics.

Questions or corrections? [Open an issue in the Melo repository](https://github.com/aarush-io/melo/issues).
