package com.youyuan.music.compose.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.youyuan.music.compose.constants.AppConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        // API 服务器 URL
        val APP_API_URL = stringPreferencesKey("app_api_url")

        // 用户界面
        val APP_DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("app_dynamic_color_enabled")
        val PLAYER_SQUIGGLY_WAVE_ENABLED = booleanPreferencesKey("player_squiggly_wave_enabled")
        val PLAYER_COVER_TYPE = intPreferencesKey("player_cover_type")

        // 播放器行为
        val PLAYER_SEEK_TO_PREVIOUS_ACTION = intPreferencesKey("player_seek_to_previous_action")
    }

    val appApiUrl: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[APP_API_URL] ?: AppConstants.APP_API_ENDPOINT
        }

    // 用户界面的设置
    val appDynamicColorEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            // 默认禁用
            preferences[APP_DYNAMIC_COLOR_ENABLED] ?: false
        }

    val isPlayerSquigglyWaveEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            // 默认启用
            preferences[PLAYER_SQUIGGLY_WAVE_ENABLED] ?: true
        }

    val playerCoverType: Flow<Int> = dataStore.data
        .map { preferences ->
            // 默认值为 0，即方形封面
            preferences[PLAYER_COVER_TYPE] ?: 0
        }

    // 播放器行为的设置
    val playerSeekToPreviousAction: Flow<Int> = dataStore.data.catch {
        if (it is IOException) {
            emit(emptyPreferences())
        } else {
            throw it
        }
    }
        .map { preferences ->
            // 默认值为 0，即 Media3 默认行为
            preferences[PLAYER_SEEK_TO_PREVIOUS_ACTION] ?: PlayerSeekToPreviousAction.DEFAULT.ordinal
        }

    suspend fun setAppApiUrl(url: String) {
        dataStore.edit { settings ->
            settings[APP_API_URL] = url
        }
    }

    // 用户界面的设置
    suspend fun setAppDynamicColorEnabled(isEnabled: Boolean) {
        dataStore.edit { settings ->
            settings[APP_DYNAMIC_COLOR_ENABLED] = isEnabled
        }
    }

    suspend fun setPlayerSquigglyWaveEnabled(isEnabled: Boolean) {
        dataStore.edit { settings ->
            settings[PLAYER_SQUIGGLY_WAVE_ENABLED] = isEnabled
        }
    }

    suspend fun setPlayerCoverType(type: PlayerCoverType) {
        dataStore.edit { settings ->
            settings[PLAYER_COVER_TYPE] = type.ordinal
        }
    }

    // 播放器行为的设置
    suspend fun setPlayerSeekToPreviousAction(action: PlayerSeekToPreviousAction) {
        dataStore.edit { settings ->
            settings[PLAYER_SEEK_TO_PREVIOUS_ACTION] = action.ordinal
        }
    }

}