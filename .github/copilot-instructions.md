# YouYanMusic Copilot Instructions

## Big Picture
- Kotlin + Jetpack Compose 音乐播放器：Salt UI + Hilt + Media3。
- 主链路：`MainActivity` → `YouYanMusicTheme` → `RootView`（抽屉/TopAppBar/NavHost/BottomSheetPlayer）。

## Architecture & Data Flows（按代码验证）
- DI：`App` 是 `@HiltAndroidApp`；`MainActivity`/`MusicPlaybackService` 是 `@AndroidEntryPoint`；ViewModel 用 `@HiltViewModel`。
- UI 外壳：`ui/view/RootView.kt` 持有 `NavHostController`、抽屉（`gesturesEnabled=false`）、TopAppBar、底部栏显示/隐藏、播放器 BottomSheet 状态与 Insets。
- 导航：`ui/screens/ScreenRoute.kt` 定义路由；`ui/screens/NavBuilder.kt` 的 `navigationBuilder(...)` 注册 destinations。

## Networking / Cookies / 风控
- API Endpoint 是运行期可切换的：`RootView` 首次启动强制弹窗填写；`ui/viewmodel/AppConfigViewModel.kt` 写入 `SettingsDataStore` 并调用 `ApiClient.setBaseUrl(...)` 立即生效。
- Retrofit/OkHttp：`api/ApiClient.kt` 用 `DynamicBaseUrlInterceptor` 在发起请求前重写 `scheme/host/port`（并支持 baseUrl 带 path 前缀）；Retrofit 的 `.baseUrl("http://localhost/")` 仅用于 path 拼接。
- Cookie：`api/CookieManager.kt` 作为 OkHttp `CookieJar`，按 host 持久化到 `SharedPreferences`；`ApiClient.saveCookieString(...)` 可手动灌 cookie。
- 风控：`ApiClient` 检测响应 body 中 `"code":-462` 并抛 `IOException("RISK_CONTROL_-462:...")`；`ui/viewmodel/PlayerViewModel.kt` 识别后退避（默认 5 分钟）并通过 `error: StateFlow<String?>` 让 `RootView` Toast。
- API 文档：不清楚接口行为/参数时，优先查 `netease-cloudmusic-apidoc-home.md`；QR 登录接口 `api/apis/QrCodeLoginApi.kt` 默认带 `timestamp` 防缓存。
- 注意：该文档不提供响应参数体；若需要为某接口编写/补全 `api/model/` 数据类，请先向维护者索要该接口的真实响应 JSON（或抓包结果）再实现。

## Player（Media3）
- 服务端：`service/MusicPlaybackService.kt`（`MediaLibraryService` + `ExoPlayer`），在 `app/src/main/AndroidManifest.xml` 注册并声明 `foregroundServiceType="mediaPlayback"`。
- 播放 URL 解析：播放器使用占位 URI `yym://song/{id}`；服务端通过 `ResolvingDataSource` 调 `SongUrlApi` 解析为真实 URL（并做内存缓存）。
- UI/VM 控制：`utils/PlayerController.kt` 用 `SessionToken` 异步构建 `MediaController`（`@Singleton`），对外暴露 `StateFlow`（连接/播放/缓冲/循环/随机/索引/速度/音高）。
- 退出应用：服务自定义命令触发 `ACTION_EXIT_APP` 广播；`MainActivity` 用 `LocalBroadcastManager` 接收并 `finishAffinity()`。

## Project Conventions
- ViewModel 里从注入的 `ApiClient` 建 service：`apiClient.createService(FooApi::class.java)`（例如 `PlayerViewModel`）。
- 日志：用 `utils/Logger.kt`（Debug 日志受 `BuildConfig.DEBUG` 控制）。
- 设置：`pref/SettingsDataStore.kt` 存偏好（动态取色/播放器行为/保存的 API URL 等）。

## Common Edits
- 新增页面：加路由到 `ui/screens/ScreenRoute.kt` → 在 `ui/screens/NavBuilder.kt` 增加 `composable(...)`；若是主 Tab，同步 `ScreenRoute.MainScreens` 并检查 `RootView` 底栏显示逻辑。
- 新增接口：在 `api/apis/` 增 Retrofit interface + `api/model/` 数据类；在对应 ViewModel 里通过 `apiClient.createService(...)` 使用。

## Build / Run
- 推荐：Android Studio Run/Debug。
- CLI（Windows）：`./gradlew.bat :app:assembleDebug`，单测：`./gradlew.bat :app:testDebugUnitTest`。
- CLI（Linux/macOS）：`./gradlew :app:assembleDebug`，单测：`./gradlew :app:testDebugUnitTest`。
