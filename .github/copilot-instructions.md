# YouYanMusic Copilot Instructions

## Big Picture
- Kotlin + Jetpack Compose 音乐播放器：Salt UI + Hilt + Media3。
- UI 主链路：`MainActivity` → `YouYanMusicTheme` → `RootView`（抽屉 + TopAppBar + NavHost + BottomSheetPlayer + BottomBar）。

## Architecture & Data Flows (read-first)
- DI：`App` 使用 `@HiltAndroidApp`；`MainActivity`/`MusicPlaybackService` 使用 `@AndroidEntryPoint`；ViewModel 使用 `@HiltViewModel`。
- UI 外壳在 `ui/view/RootView.kt`：集中管理 `NavHostController`、侧边抽屉（`gesturesEnabled=false`）、TopAppBar 类型切换、底部栏显隐、播放器 BottomSheet 与 Insets。
- 导航约定：路由在 `ui/screens/ScreenRoute.kt`（sealed class），Destination 注册集中在 `ui/screens/NavBuilder.kt` 的 `navigationBuilder(...)`。

## Runtime API Endpoint (must be configured)
- 首次启动强制配置 API Endpoint：`RootView` 读取 `AppConfigViewModel.savedApiUrl`，若为空字符串则弹出不可取消的 `InputDialog`；取消即 `finishAffinity()`。
- `AppConfigViewModel` 负责把 URL 写入 `pref/SettingsDataStore.kt`，并通过 `ApiClient.setBaseUrl(...)` 让后续请求立即生效。

## Networking / Cookies / Risk Control
- `api/ApiClient.kt`：Retrofit 固定 `.baseUrl("http://localhost/")` 仅用于 path 拼接；真实 host/scheme/port 在 OkHttp Interceptor 中运行时重写（`DynamicBaseUrlInterceptor` 是 `ApiClient` 的内部类，支持 baseUrl 带 path 前缀）。
- Cookie：`api/CookieManager.kt` 作为 OkHttp `CookieJar`，按 host 将 cookie 持久化到 `SharedPreferences`；也支持 `ApiClient.saveCookieString(...)` 从字符串手动灌入。
- 风控：`ApiClient.RiskControlInterceptor` 通过 `peekBody` 检测响应 JSON 的 `"code":-462` 并抛 `IOException("RISK_CONTROL_-462:...")`。
- UI 反馈：`PlayerViewModel`/`SongCommentViewModel` 会识别 `RISK_CONTROL_-462` 并进入退避（`PlayerViewModel` 默认 5 分钟），通过 `PlayerViewModel.error` 让 `RootView` Toast。

## Player (Media3)
- 服务端：`service/MusicPlaybackService.kt` 是 `MediaLibraryService + ExoPlayer`（Manifest 声明 `foregroundServiceType="mediaPlayback"`）。
- 懒解析播放 URL：播放列表项可以用占位 URI `yym://song/{id}`；Service 通过 `ResolvingDataSource` 调 `api/apis/SongUrlApi.kt` 解析到真实 URL，并按 `songId|level` 做内存缓存；若所选音质无 URL 会回落到 `standard`。
- 客户端控制：`utils/PlayerController.kt` 用 `SessionToken` 异步构建 `MediaController`（`@Singleton`），以 `StateFlow` 暴露播放/缓冲/循环/随机/速度/音高/索引。
- 播放列表：`utils/PlayerPlaylistManager.kt` 是全局 object + `StateFlow`；`PlayerViewModel` 在其上做更复杂的“补齐/去重/串行化队列变更”(通过 `Mutex` 等) ——改播放队列时优先沿用现有串行化入口。
- 退出应用链路：Service 自定义命令触发 `ACTION_EXIT_APP` 广播；`MainActivity` 用 `LocalBroadcastManager` 接收并 `finishAffinity()`。

## Common edits
- 新增页面：加路由到 `ui/screens/ScreenRoute.kt` → 在 `ui/screens/NavBuilder.kt` 注册 `composable(...)`；若是主 Tab，同时更新 `ScreenRoute.MainScreens`，并检查 `RootView` 底部栏显隐逻辑。
- 新增接口：在 `api/apis/` 新增 Retrofit interface；在 ViewModel/Service 中通过注入的 `ApiClient.createService(FooApi::class.java)` 获取实现。
- 新增设置项：在 `pref/SettingsDataStore.kt` 增 key + Flow + setter；UI 侧用 `collectAsState(initial=...)`。

## Build / Test
- 推荐 Android Studio Run/Debug。
- Windows：`./gradlew.bat :app:assembleDebug`，单测：`./gradlew.bat :app:testDebugUnitTest`。
- Linux/macOS：`./gradlew :app:assembleDebug`，单测：`./gradlew :app:testDebugUnitTest`。
