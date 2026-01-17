package com.youyuan.music.compose.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import androidx.core.content.edit
import kotlin.collections.iterator

class CookieManager(private val context: Context) : CookieJar {
    companion object {
        private const val PREFS_NAME = "app_cookies"
        private const val TAG = "CookieManager"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        Log.d(TAG, "saveFromResponse: url=${url.host}, cookies count=${cookies.size}")
        sharedPreferences.edit {

            for (cookie in cookies) {
                val key = "${url.host}_${cookie.name}"
                val value =
                    "${cookie.value}|${cookie.domain}|${cookie.path}|${cookie.expiresAt}|${cookie.secure}|${cookie.httpOnly}"
                Log.d(TAG, "保存 cookie: $key")
                putString(key, value)
            }

        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = mutableListOf<Cookie>()
        val allCookies = sharedPreferences.all
        
        Log.d(TAG, "loadForRequest: url=${url.host}, 总共有 ${allCookies.size} 个存储的 cookie")

        for ((key, value) in allCookies) {
            if (key.startsWith("${url.host}_") && value is String) {
                val parts = value.split("|")
                if (parts.size == 6) {
                    try {
                        val cookieName = key.substringAfter("${url.host}_")
                        val cookieBuilder = Cookie.Builder()
                            .name(cookieName)
                            .value(parts[0])
                            .domain(parts[1])
                            .path(parts[2])

                        val expiresAt = parts[3].toLongOrNull()
                        if (expiresAt != null && expiresAt > 0) {
                            cookieBuilder.expiresAt(expiresAt)
                        }

                        if (parts[4].toBoolean()) {
                            cookieBuilder.secure()
                        }

                        if (parts[5].toBoolean()) {
                            cookieBuilder.httpOnly()
                        }

                        val cookie = cookieBuilder.build()

                        // 检查cookie是否过期
                        if (cookie.expiresAt == Long.MAX_VALUE || cookie.expiresAt > System.currentTimeMillis()) {
                            cookies.add(cookie)
                            Log.d(TAG, "加载 cookie: $cookieName")
                        } else {
                            // 删除过期的cookie
                            Log.d(TAG, "Cookie 过期，删除: $cookieName")
                            sharedPreferences.edit { remove(key) }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "解析 cookie 失败: $key, error: ${e.message}")
                        // 如果解析失败，删除这个cookie
                        sharedPreferences.edit { remove(key) }
                    }
                }
            }
        }
        
        Log.d(TAG, "loadForRequest: 返回 ${cookies.size} 个 cookie")

        return cookies
    }

    /**
     * 清除所有存储的cookies
     */
    fun clearAll() {
        sharedPreferences.edit { clear() }
    }

    /**
     * 清除特定域名的cookies
     */
    fun clearForHost(host: String) {
        sharedPreferences.edit {
            val allCookies = sharedPreferences.all

            for (key in allCookies.keys) {
                if (key.startsWith("${host}_")) {
                    remove(key)
                }
            }

        }
    }
    
    /**
     * 手动保存从响应 body 中返回的 cookie 字符串
     * cookie 字符串格式如: "MUSIC_U=xxx; __csrf=xxx; ..."
     */
    fun saveCookieString(host: String, cookieString: String) {
        Log.d(TAG, "saveCookieString: host=$host, cookieString length=${cookieString.length}")
        Log.d(TAG, "saveCookieString: raw=$cookieString")
        
        sharedPreferences.edit {
            // 解析 cookie 字符串
            val cookiePairs = cookieString.split(";")
            for (pair in cookiePairs) {
                val trimmed = pair.trim()
                if (trimmed.isEmpty()) continue
                
                val eqIndex = trimmed.indexOf("=")
                if (eqIndex > 0) {
                    val name = trimmed.substring(0, eqIndex).trim()
                    val value = trimmed.substring(eqIndex + 1).trim()
                    
                    // 跳过一些属性字段
                    if (name.lowercase() in listOf("path", "domain", "expires", "max-age", "secure", "httponly", "samesite")) {
                        continue
                    }
                    
                    val key = "${host}_$name"
                    // 使用简化格式存储，设置一个较长的过期时间
                    val cookieValue = "$value|$host|/|${System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000}|false|false"
                    Log.d(TAG, "saveCookieString: 保存 $name")
                    putString(key, cookieValue)
                }
            }
        }
        
        // 验证保存结果
        Log.d(TAG, "saveCookieString: 保存后检查 hasCookiesForHost=$host => ${hasCookiesForHost(host)}")
    }
    
    /**
     * 检查是否有指定 host 的 cookie
     */
    fun hasCookiesForHost(host: String): Boolean {
        val allCookies = sharedPreferences.all
        return allCookies.keys.any { it.startsWith("${host}_") }
    }
}