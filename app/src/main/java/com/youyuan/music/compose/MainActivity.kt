package com.youyuan.music.compose

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.youyuan.music.compose.pref.SettingsDataStore
import com.youyuan.music.compose.service.MusicPlaybackService
import com.youyuan.music.compose.ui.theme.YouYanMusicTheme
import com.youyuan.music.compose.ui.view.RootView
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@UnstableSaltUiApi
class MainActivity : ComponentActivity() {

    private val appExitReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MusicPlaybackService.ACTION_EXIT_APP) {
                finishAffinity()
            }
        }
    }

    private val context: Context
        get() = this@MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // 注册应用退出广播接收器
        val filter = android.content.IntentFilter().apply {
            addAction(MusicPlaybackService.ACTION_EXIT_APP)
        }
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(appExitReceiver, filter)

        setContent {
            // 设置项目读取
            val settingsDataStore = remember { SettingsDataStore(context) }
            // 是否启用动态颜色
            val useDynamicColor = settingsDataStore.appDynamicColorEnabled.collectAsState(
                initial = false
            ).value

            YouYanMusicTheme(
                dynamicColor = useDynamicColor
            ) {
                RootView(
                    context = this@MainActivity
                )
            }
        }
    }
}