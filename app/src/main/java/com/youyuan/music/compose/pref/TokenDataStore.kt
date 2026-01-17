package com.youyuan.music.compose.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "token")

class TokenDataStore(context: Context) {
    private val dataStore = context.tokenDataStore

    companion object {
        // 登录相关
        val AUTH_COOKIE = stringPreferencesKey("auth_cookie")
        val USER_ID = longPreferencesKey("user_id")
        val NICKNAME = stringPreferencesKey("nickname")
        val AVATAR_URL = stringPreferencesKey("avatar_url")
        val BACKGROUND_URL = stringPreferencesKey("background_url")
    }

    // Cookie
    val authCookie: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[AUTH_COOKIE]
        }

    // 用户信息
    val userId: Flow<Long?> = dataStore.data
        .map { preferences ->
            preferences[USER_ID]
        }

    val nickname: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[NICKNAME]
        }

    val avatarUrl: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[AVATAR_URL]
        }

    val backgroundUrl: Flow<String?> = dataStore.data
        .map { preferences ->
            preferences[BACKGROUND_URL]
        }

    // 保存Cookie
    suspend fun saveAuthCookie(cookie: String) {
        dataStore.edit { settings ->
            settings[AUTH_COOKIE] = cookie
        }
    }

    // 保存用户信息
    suspend fun saveUserInfo(
        userId: Long,
        nickname: String,
        avatarUrl: String?,
        backgroundUrl: String?
    ) {
        dataStore.edit { settings ->
            settings[USER_ID] = userId
            settings[NICKNAME] = nickname
            avatarUrl?.let { settings[AVATAR_URL] = it }
            backgroundUrl?.let { settings[BACKGROUND_URL] = it }
        }
    }

    // 清除所有登录信息
    suspend fun clearAll() {
        dataStore.edit { settings ->
            settings.remove(AUTH_COOKIE)
            settings.remove(USER_ID)
            settings.remove(NICKNAME)
            settings.remove(AVATAR_URL)
            settings.remove(BACKGROUND_URL)
        }
    }
}
