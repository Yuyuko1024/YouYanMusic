---
description: "Use when adding or changing Compose screens, routes, tabs, or navigation behavior in YouYanMusic, including related service/viewmodel changes. Helps keep ScreenRoute, NavBuilder, and RootView in sync."
name: "YouYan Navigation Sync"
applyTo: ["app/src/main/java/com/youyuan/music/compose/ui/**/*.kt", "app/src/main/java/com/youyuan/music/compose/service/**/*.kt"]
---
# Navigation And Screen Wiring

- Keep route definitions centralized in `ui/screens/ScreenRoute.kt` when route changes are needed.
- Register new destinations in `ui/screens/NavBuilder.kt` via `navigationBuilder(...)` when applicable.
- If a screen is a main tab, update `ScreenRoute.MainScreens` and verify bottom bar visibility logic in `ui/view/RootView.kt`.
- Preserve `RootView` as the shell owner for drawer, top app bar mode, bottom sheet player, and insets. Avoid duplicating shell logic in screen composables.
- Keep drawer gestures disabled in the app shell unless a task explicitly requires changing that default (`gesturesEnabled=false`).

## Quick Checklist

- New route added in `ScreenRoute.kt` when a new screen route is introduced
- Destination registered in `NavBuilder.kt` when routing is touched
- Main tab list updated if needed
- Bottom bar and top app bar behavior verified in `RootView.kt` when shell behavior is affected
