# Play Store release checklist

Everything below can only be done in Play Console / Google Cloud Console's web UI —
nothing here can be automated from the repo. Everything that *could* be prepared
from the repo (signing config, R8, icon, screenshots, listing copy, privacy policy)
is already done — see `README.md` → "Release build" and the rest of this folder.

## 1. Google Cloud Console — register the release SHA-1
⚠️ The original `mulaisekarang-release.jks` was lost; a new release keystore was
generated (2026-08-21). The old registered SHA-1 (`DE:7A:D6:09...`) is stale —
register the **new** release SHA-1 instead:
`B7:C3:73:0B:48:D4:A0:42:AE:46:29:C7:BE:22:0D:33:17:D7:4C:23`
as a new Android OAuth client (package `com.mulaisekarang.app`) in the
`mulai-sekarang-auth` Google Cloud project. `keystore.properties` / the `.jks`
are gitignored — see README.md's Google Sign-In section for the client layout.

After the first Play Console upload with **Play App Signing** enabled, also
register the separate "App signing certificate" SHA-1 that Play Console shows
under Setup → App signing — that's the cert Play re-signs the app with for
end users, different from this upload-key SHA-1.

## 2. Play Console — create the app
1. Create app → name "Mulai Sekarang" (or per final `listing-copy.md`) → Education category → Free.
2. Store listing: upload `play-store-icon-512.png`, `feature-graphic-1024x500.png`, and the screenshots in `screenshots/`. Paste in the short/full description from `listing-copy.md`.
3. Privacy policy URL: `https://mulaisekarang.com/privacy-policy` — confirmed live (200 OK, checked 2026-08-21).

## 3. Play Console — required questionnaires
- **Data safety**: use `data-safety.md` as the answer key.
- **Content rating (IARC)**: education app, has user-to-user chat (answer "yes" to the communication question), no violence/gambling/ads.
- **Target audience & content**: pick the appropriate age range given the course content is general-audience skill-building, not directed at children.
- **App access**: since login is required to use most features, provide a test account (e.g. create one via the app's registration flow) so Google's reviewers can sign in.

## 4. App signing
Enable **Play App Signing** (Google manages the actual signing key; you upload with the key from `keystore.properties`, which becomes the "upload key"). This is the default/recommended flow for new apps — no extra setup needed beyond uploading the first signed `.aab`.

## 5. Build and upload
```bash
./gradlew bundleRelease
# app/build/outputs/bundle/release/app-release.aab
```
Upload that `.aab` to a **Closed/Internal testing** track first, not Production — verify the signed build actually works (see README's release section + this repo's own on-device verification) before promoting.

## 6. After the first release
- Keep `/home/ykde/mulaisekarang-android-release-keys/mulaisekarang-release.jks` and `keystore.properties` backed up somewhere safe *outside* this machine (password manager + encrypted backup) — losing them blocks all future updates unless Play App Signing's key-upgrade process is used.
- `versionCode` must increase on every subsequent upload (`app/build.gradle.kts`); `versionName` is cosmetic.
