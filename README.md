# mulaisekarang-android

Native Android frontend (Kotlin + Jetpack Compose) for [mulaisekarang](../mulaisekarang), consuming the `/api/v1` JSON API added to the Laravel backend.

## Scope of this initial slice

- Auth: register, login, Google Sign-In, `/me`, logout (Laravel Sanctum token auth).
- Marketplace: course listing with search + category filter.
- Course detail: topics/lessons, mentor, price, enrollment status.
- Profile: current user info + logout.

Not included yet (future work): mentor/student/admin dashboards, push notifications.

## Requirements

- JDK 17, Android SDK (platform 35, build-tools 35.0.0), Gradle — see `~/android-toolchain` on this machine, or install your own and point `local.properties` at your SDK.
- A running backend. Two options:
  - **Local dev** (default for `debug` builds): run `php artisan serve --host=0.0.0.0 --port=8001` from `../mulaisekarang`, reachable at `BASE_URL` configured in `app/build.gradle.kts` (`debug` build type). Update that IP/port if your dev server differs.
  - **Production**: `release` builds point at `https://v2.mulaisekarang.com/api/v1/`.

## Build & run

```bash
export JAVA_HOME=~/android-toolchain/jdk-17
export ANDROID_HOME=~/android-toolchain/sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"

./gradlew assembleDebug
adb connect <device-ip>:5555   # if using wireless debugging
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Release build

Signing config is read from `keystore.properties` (repo root, gitignored — copy `keystore.properties.example` and fill in real values, or point `storeFile` at an existing keystore). Without that file present, `signingConfig` is simply left unset and `assembleRelease`/`bundleRelease` will produce an unsigned artifact — `assembleDebug`/`installDebug` are unaffected either way.

```bash
./gradlew bundleRelease   # signed .aab for Play Console, under app/build/outputs/bundle/release/
./gradlew assembleRelease # signed .apk for manual/sideload testing
```

R8 minification + resource shrinking are enabled for `release` (`app/proguard-rules.pro` carries the kotlinx.serialization keep rules the models need). `release` also uses its own `network_security_config.xml` (`src/release/res/xml/`) with no cleartext exceptions at all, separate from the dev-friendly one in `src/debug/`.

See `store-assets/release-checklist.md` for the full Play Console submission checklist.

## Architecture

- **UI**: Jetpack Compose, single-activity, `navigation-compose` for screen routing (`ui/navigation/NavGraph.kt`).
- **Networking**: Retrofit + OkHttp + kotlinx.serialization (`data/network/`). `AuthInterceptor` attaches the stored Bearer token to every request.
- **Auth/session**: token persisted via Jetpack DataStore (`data/TokenStore.kt`).
- **DI**: Hilt (`di/NetworkModule.kt`, `di/DatabaseModule.kt` provide Retrofit/OkHttp/Room singletons; every screen's ViewModel is `@HiltViewModel` and obtained via `hiltViewModel()` in the nav graph).
- **Offline cache**: Room (`data/local/`) is the source of truth for the default course catalog feed only (`CourseEntity`/`CourseDetailEntity`/`RemoteKeyEntity` via Paging3's `RemoteMediator`); every other screen is a thin, network-only pass-through to Retrofit.
- **Cleartext HTTP**: only permitted to the dev server IP / emulator loopback, via the `debug`-only `network_security_config.xml`; the `release` variant has its own copy with no cleartext exceptions (production traffic is HTTPS-only).

## Google Sign-In

Uses Credential Manager (`androidx.credentials` + `googleid`) — see `auth/GoogleAuthClient.kt`. The flow gets a Google ID token on-device and posts it to the backend's `POST /api/v1/auth/google`, which verifies it via `google/apiclient` and issues a Sanctum token (mirrors `SocialAuthController` on the web app: matches by `google_id` or `email`, creates a `subscriber` if no match).

Two separate OAuth client registrations are involved, both in the same Google Cloud project that owns `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` in the backend's `.env`:

- **Web client** (`785712319782-ps89gis6i0uc83kjesjg9k0qtj1nlue8...`) — used both by the web app and passed as `serverClientId`/`BuildConfig.GOOGLE_WEB_CLIENT_ID` in the Android app. This is the audience the backend checks when verifying the ID token.
- **Android client — debug** (`785712319782-ap6uk2nfudcafoqpk9bd72jn6poejv4v...`) — registered against package name `com.mulaisekarang.app` + the **debug** SHA-1 (`B3:4D:6B:49:24:17:35:25:CF:2E:D9:02:B3:6F:14:1B:6E:71:30:51`).
- **Android client — release** (`785712319782-043gtftbprm13mj3jv7ds8vovp30le2m...`) — registered against the same package name + the **release** SHA-1 (`DE:7A:D6:09:71:46:30:30:EF:B9:E0:BC:3B:C7:42:3B:10:AD:19:96`, from `mulaisekarang-release.jks`).

Neither Android client ID is referenced anywhere in code — Google authorizes the app purely by package name + SHA-1 match at sign-in time, so registering the cert is the entire fix. Only the **web** client ID above ever appears in code (`GOOGLE_WEB_CLIENT_ID` / `serverClientId`).
