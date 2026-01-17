# YouYanMusic Copilot Instructions

## Big Picture
YouYanMusic is a Kotlin + Jetpack Compose music player using Salt UI + Hilt + Media3.
Main flow: `MainActivity` → `YouYanMusicTheme` → `RootView` (drawer/top bar/nav + mini-player bottom sheet) → screen composables.

## Architecture (verified)
- **DI**: App is `App` (`@HiltAndroidApp`). Activities use `@AndroidEntryPoint`. ViewModels use `@HiltViewModel`.
- **UI shell**: `ui/view/RootView.kt` owns `NavHostController`, `DismissibleNavigationDrawer` (gestures disabled), `TopAppBar`, `BottomBar`, and the player bottom-sheet/insets.
- **Navigation**: Routes are a sealed class in `ui/screens/ScreenRoute.kt`. Destinations are registered in `ui/screens/NavBuilder.kt` via `navigationBuilder(...)`.
  - Dynamic route example: `ScreenRoute.LikedSong.createRoute(userId)`.
- **Player**:
  - Playback service is `service/MusicPlaybackService.kt` (Media3 `MediaLibraryService` + `ExoPlayer`), registered in `AndroidManifest.xml` with `foregroundServiceType="mediaPlayback"`.
  - UI talks to the service via Media3 `MediaController` in `utils/PlayerController.kt` (`@Singleton`, built from a `SessionToken`).
  - App exit is triggered via `MusicPlaybackService.ACTION_EXIT_APP` (handled by a receiver in `MainActivity`).

## Networking & Auth/Cookies
- **ApiClient singleton**: `api/ApiClient.kt` wraps Retrofit/OkHttp, injects browser-like headers, and persists cookies via a custom `CookieManager` (`cookieJar(...)`).
- **Provided via Hilt**: `di/NetworkModule.kt` provides a singleton `ApiClient` using `constants/AppConstants.APP_API_ENDPOINT`.
- **Backend API docs**: `netease-cloudmusic-apidoc-home.md` (NeteaseCloudMusicApiEnhanced, typically hosted on port 3000).
  - Server may cache identical URLs for ~2 minutes; add a `timestamp` query to bypass when needed (QR/login flows already do this in `QrCodeLoginApi`).
  - Many endpoints require login cookies; missing cookie often manifests as `301` (doc notes this). `ApiClient` has a cookie jar + helpers like `saveCookieString(...)`.
  - If you see `460` (“cheating异常”) on restricted environments, doc suggests `realIP=...` or `randomCNIP=true` on requests.
- **Risk control handling**:
  - `ApiClient` may throw `IOException("RISK_CONTROL_-462:...")` when body contains `{"code":-462,...}`.
  - `PlayerViewModel` detects this and enters a short backoff + exposes a user-facing `error` `StateFlow` (shown as a Toast in `RootView`).
- **ViewModel pattern**: create Retrofit services from the injected client:
  ```kotlin
  private val songApi: SongApi = apiClient.createService(SongApi::class.java)
  ```

## Settings / Configuration
- **Base URL**: `constants/AppConstants.kt` contains the endpoint used by `NetworkModule` today.
  - `APP_API_ENDPOINT` is intended for a future “custom API backend” dialog, but there is **no current plan** to implement that UI.
  - `pref/SettingsDataStore.appApiUrl` exists, but is **not** wired into `ApiClient` right now (changing it won’t change network calls unless DI is updated).
- **User settings**: `pref/SettingsDataStore.kt` stores preferences (dynamic color + player behaviors).
- **Theme toggle wiring**: `MainActivity` collects `appDynamicColorEnabled` to configure `YouYanMusicTheme`.
- **Cleartext HTTP**: `app/src/main/AndroidManifest.xml` sets `android:usesCleartextTraffic="true"`.

## State & Logging
- **State**: ViewModels expose `StateFlow` (e.g. `PlayerViewModel.currentSong`, `isPlaying`, `currentPosition`). UI collects with `collectAsState`.
- **Logging**: Use `utils/Logger.kt` (debug logs guarded by `BuildConfig.DEBUG`).

## Common edits
- **Add a screen**: add route in `ui/screens/ScreenRoute.kt` → add `composable(...)` in `ui/screens/NavBuilder.kt` → if it’s a main tab, add to `ScreenRoute.MainScreens` and ensure `RootView` navigation UI logic still works.
- **Add an API endpoint**: create interface in `api/apis/` + models in `api/model/` → create service inside the relevant ViewModel via `apiClient.createService(...)`.

## Build/Test (Gradle)
- Primary workflow: run/debug from **Android Studio**.
- Optional CLI tasks (if needed): `./gradlew.bat :app:assembleDebug`, `./gradlew.bat :app:test`.
