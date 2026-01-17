package com.youyuan.music.compose.utils

import android.content.Context
import android.content.Intent

object IntentUtils {

    fun openSystemEqualizer(context: Context): Boolean {
        return runCatching {
            val intent = Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }.onFailure {
            it.printStackTrace()
        }.isSuccess
    }
}