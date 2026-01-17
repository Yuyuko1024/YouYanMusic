# YouYanMusic Copilot Instructions

## Big Picture
- Kotlin + Jetpack Compose 音乐播放器：Salt UI + Hilt + Media3。
- 主链路：`MainActivity` → `YouYanMusicTheme` → `RootView`（抽屉/TopAppBar/NavHost/BottomSheetPlayer）。

## Architecture（按代码验证）
- DI：`App` 使用 `@HiltAndroidApp`；`MainActivity` 为 `@AndroidEntryPoint`；ViewModel 为 `@HiltViewModel`。
- UI 外壳：`ui/view/RootView.kt` 持有 `NavHostController`、抽屉（`gesturesEnabled=false`）、TopAppBar、底部栏显示/隐藏逻辑，以及播放器 BottomSheet 状态与 Insets。
- 导航：`ui/screens/ScreenRoute.kt` 定义路由；`ui/screens/NavBuilder.kt` 的 `navigationBuilder(...)` 注册 destinations。
  - 动态路由示例：`ScreenRoute.LikedSong.createRoute(userId)` → `liked/{userId}`。

## Player（Media3）
- 服务端：`service/MusicPlaybackService.kt`（`MediaLibraryService` + `ExoPlayer`），在 `app/src/main/AndroidManifest.xml` 注册并声明 `foregroundServiceType="mediaPlayback"`。
- UI/VM 控制：`utils/PlayerController.kt` 用 `SessionToken` 异步构建 `MediaController`（`@Singleton`），对外暴露 `StateFlow`（连接/播放/缓冲/循环/随机/索引）。
- 退出应用：服务广播 `MusicPlaybackService.ACTION_EXIT_APP`，`MainActivity` 用 `LocalBroadcastManager` 接收并 `finishAffinity()`。

## Networking / Cookies / 风控
- Retrofit/OkHttp：`api/ApiClient.kt`（Hilt 单例由 `di/NetworkModule.kt` 提供），baseUrl 当前来自 `constants/AppConstants.APP_API_ENDPOINT`。
- Cookie：`api/CookieManager.kt` 作为 OkHttp `CookieJar` 持久化到 `SharedPreferences`；`ApiClient.saveCookieString(...)` 可手动灌 cookie。
- 风控：`ApiClient` 拦截响应 body 中 `"code":-462` 并抛 `IOException("RISK_CONTROL_-462:...")`；`ui/viewmodel/PlayerViewModel.kt` 识别后进入退避并通过 `error: StateFlow<String?>` 让 `RootView` Toast。
- API 文档：`netease-cloudmusic-apidoc-home.md`；QR 登录接口 `api/apis/QrCodeLoginApi.kt` 默认带 `timestamp` 防缓存。

## Project Conventions
- ViewModel 里从注入的 `ApiClient` 建 service：`private val songApi = apiClient.createService(SongApi::class.java)`（多处同模式）。
- 日志：用 `utils/Logger.kt`（Debug 日志受 `BuildConfig.DEBUG` 控制）。
- 设置：`pref/SettingsDataStore.kt` 存偏好（动态取色/播放器行为等）。`APP_API_URL` 已接入网络：首次启动会强制弹窗填写，且在设置页可修改；由 `ui/viewmodel/AppConfigViewModel.kt` 同步到 `api/ApiClient.kt`（运行时重写请求 host）。

## Common Edits
- 新增页面：加路由到 `ui/screens/ScreenRoute.kt` → 在 `ui/screens/NavBuilder.kt` 增加 `composable(...)`；若是主 Tab，同步 `ScreenRoute.MainScreens` 并检查 `RootView` 底栏显示逻辑。
- 新增接口：在 `api/apis/` 增 Retrofit interface + `api/model/` 数据类；在对应 ViewModel 里通过 `apiClient.createService(...)` 使用。

## Build / Run
- 推荐：Android Studio Run/Debug。
- CLI（Linux/macOS）：`./gradlew :app:assembleDebug`，单测：`./gradlew :app:testDebugUnitTest`。
